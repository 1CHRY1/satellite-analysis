package com.ogms.dge.workspace.modules.workflow.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ogms.dge.workspace.modules.workflow.dto.DAGDto;
import com.ogms.dge.workspace.modules.workflow.entity.TaskEntity;
import com.ogms.dge.workspace.modules.workflow.entity.WorkflowEntity;
import com.ogms.dge.workspace.modules.workflow.enums.TaskStatusEnum;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @name: WorkflowExecutionService
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 12/18/2024 2:23 PM
 * @version: 1.0
 */
@Service
public class WorkflowExecutionService {
    private final TaskSchedulerService taskSchedulerService;
    private final TaskService taskService;

    // 缓存用于存储 workflowUuid 对应的 DAG, 未来可用分布式
    private final ConcurrentHashMap<String, DAGDto<TaskEntity>> workflowDagCache = new ConcurrentHashMap<>();

    public WorkflowExecutionService(TaskSchedulerService taskSchedulerService, TaskService taskService) {
        this.taskSchedulerService = taskSchedulerService;
        this.taskService = taskService;
    }

    public void executeWorkflow(String workflowUuid) throws IOException {
        DAGDto<TaskEntity> dag = getDagForWorkflow(workflowUuid);
        System.out.println(dag.generateTopologicalOrder());
        // 找到所有入度为 0 的任务，作为初始任务集合
        List<TaskEntity> initialTasks = dag.getInitialNodes();
        for (TaskEntity task : initialTasks) {
            scheduleTask(task);
        }
    }

    public void stopWorkflow(String workflowUuid) throws SchedulerException {
        clearDagCache(workflowUuid);
        List<TaskEntity> expiredTasks = taskService.list(new QueryWrapper<TaskEntity>()
                .eq("workflow_uuid", workflowUuid));
        for (TaskEntity task : expiredTasks) {
            // 重启工作流兼容处理
            taskSchedulerService.deleteTask(task.getUuid(), task.getWorkflowUuid());
        }
    }

    private void scheduleTask(TaskEntity task) throws IOException {
        String jobName = task.getUuid();
        String jobGroup = task.getWorkflowUuid();
        JobDetail jobDetail = taskSchedulerService.buildJobDetail(task.getUuid(), jobGroup, task.getConfig());

        // 使用 Quartz 调度任务
        taskSchedulerService.startJob(jobName, jobGroup, jobDetail);
    }

    // 拓扑排序
    public void onTaskCompleted(String taskUuid, String workflowUuid) throws IOException {
        // 获取对应工作流的 DAG
        DAGDto<TaskEntity> dag = getDagForWorkflow(workflowUuid);
        dag.handleCompleteTask(taskUuid);
        // 更新完成状态
        TaskEntity completedTask = dag.getNode(taskUuid);
        completedTask.setStatus(TaskStatusEnum.COMPLETED.getCode());
        taskService.saveOrUpdate(completedTask);
        System.out.println(completedTask.getName());
        // 检查后续任务是否可执行(广度优先遍历)
        List<TaskEntity> nextTasks = dag.getNextNodes(taskUuid);
        for (TaskEntity nextTask : nextTasks) {
            if (dag.canExecute(nextTask.getUuid())) {
                scheduleTask(nextTask);
            }
        }
    }

    public void onWorkflowUpdated(String workflowUuid) {
        // 清理缓存，确保后续调用时重建 DAG
        clearDagCache(workflowUuid);
    }

    // 获取 DAG 方法
    public DAGDto<TaskEntity> getDagForWorkflow(String workflowUuid) {
        // 如果缓存中存在，直接返回
        if (workflowDagCache.containsKey(workflowUuid)) {
            return workflowDagCache.get(workflowUuid);
        }

        // 否则从数据库构建 DAG
        DAGDto<TaskEntity> dag = buildTaskDAG(workflowUuid);

        // 放入缓存并返回
        workflowDagCache.put(workflowUuid, dag);
        return dag;
    }

    public DAGDto<TaskEntity> buildTaskDAG(String workflowUuid) {
        List<TaskEntity> taskList = taskService.getByWorkflow(workflowUuid);
        // 1. 获取工作流中的所有任务
        Map<String, TaskEntity> taskMap = taskList.stream()
                .collect(Collectors.toMap(TaskEntity::getUuid, task -> task));
        // 2. 构建 DAG
        DAGDto<TaskEntity> dag = new DAGDto<>();
        for (TaskEntity task : taskList) {
            dag.addNode(task.getUuid(), task);

            // 解析 dependencies
            List<String> dependencies = parseDependencies(task.getConfig());
            for (String dependency : dependencies) {
                dag.addEdge(dependency, task.getUuid());
            }
        }
        // 3. 验证是否有环
        if (dag.hasCycle()) {
            throw new IllegalStateException("Workflow contains cyclic dependencies");
        }
        return dag;
    }

    private List<String> parseDependencies(String config) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonConfig = objectMapper.readTree(config);

            if (jsonConfig.has("dependencies")) {
                JsonNode dependenciesNode = jsonConfig.get("dependencies");
                if (dependenciesNode.isArray()) {
                    return objectMapper.readValue(dependenciesNode.traverse(), List.class);
                }
            }
            return Collections.emptyList();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid task config format", e);
        }
    }

    // 在工作流完成或更新时清理缓存
    public void clearDagCache(String workflowUuid) {
        workflowDagCache.remove(workflowUuid);
    }
}
