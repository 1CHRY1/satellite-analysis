package com.ogms.dge.workspace.modules.workflow.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ogms.dge.workspace.modules.project.entity.DataInsEntity;
import com.ogms.dge.workspace.modules.project.entity.MethodInsEntity;
import com.ogms.dge.workspace.modules.project.service.DataInsService;
import com.ogms.dge.workspace.modules.project.service.MethodInsService;
import com.ogms.dge.workspace.modules.workflow.entity.TaskEntity;
import com.ogms.dge.workspace.modules.workflow.enums.TaskStatusEnum;
import com.ogms.dge.workspace.modules.workflow.enums.TaskTypeEnum;
import com.ogms.dge.workspace.modules.workflow.service.TaskService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

/**
 * @name: SyncJob
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 11/14/2024 4:40 PM
 * @version: 1.0
 */
public class SyncJob implements Job {

    private final TaskService taskService; // 注入任务服务
    private final DataInsService dataInsService;
    private final MethodInsService methodInsService;
    private ObjectMapper objectMapper = new ObjectMapper();

    public SyncJob(TaskService taskService, DataInsService dataInsService, MethodInsService methodInsService) {
        this.taskService = taskService;
        this.dataInsService = dataInsService;
        this.methodInsService = methodInsService;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String jobKey = context.getJobDetail().getKey().toString();
        // 获取任务相关信息，更新任务状态为 "IN_PROGRESS"
        TaskEntity task = taskService.getOne(new QueryWrapper<TaskEntity>().eq("job_key", jobKey));
        task.setStartTime(new Date());
        task.setStatus(TaskStatusEnum.IN_PROGRESS.getCode());
        taskService.saveOrUpdate(task);

        try {
            // 执行同步逻辑
            if (task.getType() == TaskTypeEnum.DATA_SYNC.getCode()) {
                performDataSync(task);
            } else if (task.getType() == TaskTypeEnum.METHOD_SYNC.getCode()) {
                performMethodSync(task);
            }
            task.setEndTime(new Date());
            task.setStatus(TaskStatusEnum.COMPLETED.getCode());
            taskService.saveOrUpdate(task);
        } catch (Exception e) {
            task.setStatus(TaskStatusEnum.FAILED.getCode());
            taskService.saveOrUpdate(task);
        }
    }

    private void performDataSync(TaskEntity task) throws IOException {
        // 实现数据同步逻辑
        Map<String, Object> taskConfig = objectMapper.readValue(task.getConfig(), Map.class);
        String serviceUuid = (String) taskConfig.get("serviceUuid");
        DataInsEntity dataIns = new DataInsEntity();
        dataIns.setServiceUuid(serviceUuid);
        dataIns.setCreateTime(new Date());
        System.out.println("数据开始同步");
        dataInsService.sync(dataIns);
        System.out.println("数据同步完毕");
    }

    private void performMethodSync(TaskEntity task) throws IOException {
        // 实现方法同步逻辑
        Map<String, Object> taskConfig = objectMapper.readValue(task.getConfig(), Map.class);
        Integer methodId = (Integer) taskConfig.get("methodId");
        MethodInsEntity methodIns = new MethodInsEntity();
        methodIns.setMethodId(Long.valueOf(methodId));
        methodIns.setCreateTime(new Date());
        System.out.println("方法开始同步");
        methodInsService.sync(methodIns);
        System.out.println("方法同步完毕");
    }
}
