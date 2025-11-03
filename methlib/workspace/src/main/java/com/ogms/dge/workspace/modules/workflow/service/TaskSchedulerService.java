package com.ogms.dge.workspace.modules.workflow.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ogms.dge.workspace.common.utils.FileUtils;
import com.ogms.dge.workspace.modules.workflow.converter.WorkflowConverter;
import com.ogms.dge.workspace.modules.workflow.dto.VueFlowNodeDto;
import com.ogms.dge.workspace.modules.workflow.entity.TaskEntity;
import com.ogms.dge.workspace.modules.workflow.enums.TaskStatusEnum;
import com.ogms.dge.workspace.modules.workflow.job.MethodJob;
import com.ogms.dge.workspace.modules.workflow.job.SyncJob;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @name: TaskSchedulerService
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 11/14/2024 5:13 PM
 * @version: 1.0
 */
@Service
public class TaskSchedulerService {

    private final Scheduler scheduler;
    private final TaskService taskService;
    ObjectMapper objectMapper = new ObjectMapper();

    @Value("${workspace.workflow.fd}")
    private String workflow_fd;

    @Resource
    private WorkflowConverter workflowConverter;

    public TaskSchedulerService(Scheduler scheduler, TaskService taskService) {
        this.scheduler = scheduler;
        this.taskService = taskService;
    }

    /**
     * 启动任务
     *
     * @param jobName   任务名称
     * @param jobGroup  任务组
     * @param jobDetail 任务详细信息
     */
    public void startJob(String jobName, String jobGroup, JobDetail jobDetail) {
        try {
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(jobName + "Trigger", jobGroup)
                    .startNow()
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule().withRepeatCount(0))
                    .build();

            // 将任务调度到 Quartz Scheduler 中
            scheduler.start();
            scheduler.scheduleJob(jobDetail, trigger);

            // 更新数据库中的任务状态为“运行中”
            TaskEntity task = taskService.getOne(new QueryWrapper<TaskEntity>().eq("job_key", JobKey.jobKey(jobName, jobGroup).toString()));
            task.setStatus(TaskStatusEnum.IN_PROGRESS.getCode());
            taskService.saveOrUpdate(task);
        } catch (SchedulerException e) {
            e.printStackTrace();
            throw new RuntimeException("Error starting job", e);
        }
    }

    /**
     * 根据任务的 UUID 获取 JobDetail
     *
     * @param jobName 任务 UUID
     * @param config  任务的配置数据
     * @return JobDetail
     */
    public JobDetail buildJobDetail(String jobName, String jobGroup, String config) throws IOException {
        Map<String, Object> jsonMap = FileUtils.readFromFile(workflow_fd, jobGroup + ".json");
        List<VueFlowNodeDto> nodes = workflowConverter.map2NodeDto((List<Map<String, Object>>) jsonMap.get("nodes"));
        Map<String, Object> configMap = objectMapper.readValue(config, Map.class);
        String methodNodeId = (String) configMap.get("methodNodeId");
        VueFlowNodeDto matchingNode = nodes.stream().filter(node -> "method".equals(node.getType()) && methodNodeId.equals(node.getId())).findFirst().orElse(null);

        return JobBuilder.newJob(MethodJob.class)
                .withIdentity(jobName, jobGroup)
                .usingJobData("uuid", jobName)
                .usingJobData("config", config)
                .usingJobData("inputParams", objectMapper.writeValueAsString(matchingNode.getData().get("inputParams")))
                .usingJobData("methodId", objectMapper.writeValueAsString(((Map<String, Object>)(matchingNode.getData().get("method"))).get("id")))
                .build();
    }

    // 启动任务
    public void startTask(String jobName, String jobGroup, String cronExpression, String taskType) throws SchedulerException {
        // 定义 Quartz 任务
        JobDetail jobDetail = JobBuilder.newJob(SyncJob.class)  // 假设这是数据同步任务
                .withIdentity(jobName, jobGroup)
                .build();

        // 定义触发器
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(jobName + "Trigger", jobGroup)
                .startNow()
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withRepeatCount(0))
//                .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                .build();

        // 启动任务
        scheduler.start();
        scheduler.scheduleJob(jobDetail, trigger);

        // 保存任务信息到数据库
        TaskEntity task = taskService.getOne(new QueryWrapper<TaskEntity>().eq("job_key", JobKey.jobKey(jobName, jobGroup).toString()));
        task.setStatus(TaskStatusEnum.IN_PROGRESS.getCode());
        taskService.saveOrUpdate(task);
    }

    public void pausejob(String jobName, String jobGroup) throws SchedulerException {
        scheduler.pauseJob(JobKey.jobKey(jobName, jobGroup));
        TaskEntity task = taskService.getOne(new QueryWrapper<TaskEntity>().eq("job_key", JobKey.jobKey(jobName, jobGroup).toString()));
        task.setStatus(TaskStatusEnum.PAUSED.getCode());
        taskService.saveOrUpdate(task);
    }

    public void resumejob(String jobName, String jobGroup) throws SchedulerException {
        scheduler.resumeJob(JobKey.jobKey(jobName, jobGroup));
        TaskEntity task = taskService.getOne(new QueryWrapper<TaskEntity>().eq("job_key", JobKey.jobKey(jobName, jobGroup).toString()));
        task.setStatus(TaskStatusEnum.IN_PROGRESS.getCode());
        taskService.saveOrUpdate(task);
    }

    // 取消任务
    public void cancelTask(String jobName, String jobGroup) throws SchedulerException {
        // 通过 jobKey 查找任务记录
        TaskEntity task = taskService.getOne(new QueryWrapper<TaskEntity>().eq("job_key", JobKey.jobKey(jobName, jobGroup).toString()));

        // 如果任务存在，取消任务
        if (task != null) {
            scheduler.pauseTrigger(TriggerKey.triggerKey(jobName + "Trigger", jobGroup));
            scheduler.unscheduleJob(TriggerKey.triggerKey(jobName + "Trigger", jobGroup));
            scheduler.deleteJob(JobKey.jobKey(jobName, jobGroup));

            // 更新任务的状态为 "CANCELLED"（已取消）
            task.setStatus(TaskStatusEnum.CANCELLED.getCode());
            taskService.saveOrUpdate(task); // 保存状态更新到数据库
        } else {
            throw new SchedulerException("Job with key " + JobKey.jobKey(jobName, jobGroup) + " not found.");
        }
    }

    public void deleteTask(String jobName, String jobGroup) throws SchedulerException {
        // 通过 jobKey 查找任务记录
        TaskEntity task = taskService.getOne(new QueryWrapper<TaskEntity>().eq("job_key", JobKey.jobKey(jobName, jobGroup).toString()));

        // 如果任务存在，取消任务
        if (task != null) {
            scheduler.pauseTrigger(TriggerKey.triggerKey(jobName + "Trigger", jobGroup));
            scheduler.unscheduleJob(TriggerKey.triggerKey(jobName + "Trigger", jobGroup));
            scheduler.deleteJob(JobKey.jobKey(jobName, jobGroup));

            taskService.removeById(task.getId());
        } else {
            throw new SchedulerException("Job with key " + JobKey.jobKey(jobName, jobGroup) + " not found.");
        }
    }
}
