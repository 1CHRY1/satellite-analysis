package com.ogms.dge.workspace.modules.workflow.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.ogms.dge.workspace.common.utils.PageUtils;
import com.ogms.dge.workspace.common.utils.R;
import com.ogms.dge.workspace.modules.workflow.converter.TaskConverter;
import com.ogms.dge.workspace.modules.workflow.entity.TaskEntity;
import com.ogms.dge.workspace.modules.workflow.enums.TaskStatusEnum;
import com.ogms.dge.workspace.modules.workflow.service.TaskSchedulerService;
import com.ogms.dge.workspace.modules.workflow.service.TaskService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;


/**
 * 任务
 *
 * @author Lingkai Shi
 * @email lingkai.shi@nnu.edu.cn
 * @date 2024-11-13 16:02:54
 */
@Api
@RestController
@RequestMapping("workflow/task")
public class TaskController {
    @Autowired
    private TaskService taskService;

    @Resource
    private TaskConverter taskConverter;

    private final TaskSchedulerService taskSchedulerService;

    public TaskController(TaskSchedulerService taskSchedulerService) {
        this.taskSchedulerService = taskSchedulerService;
    }

    @ApiOperation(value = "", httpMethod = "GET")
    @RequestMapping("/start")
    public R start(@RequestParam String jobName, @RequestParam String jobGroup) throws SchedulerException {
        taskSchedulerService.startTask(jobName, jobGroup, "0 0/5 * * * ?", "");
        return R.ok();
    }

    @ApiOperation(value = "", httpMethod = "GET")
    @RequestMapping("/cancel")
    public R cancel(@RequestParam String jobName, @RequestParam String jobGroup) throws SchedulerException {
        taskSchedulerService.cancelTask(jobName, jobGroup);
        return R.ok();
    }

    @ApiOperation(value = "", httpMethod = "GET")
    @RequestMapping("/pause")
    public R pause(@RequestParam String jobName, @RequestParam String jobGroup) throws SchedulerException {
        taskSchedulerService.pausejob(jobName, jobGroup);
        return R.ok();
    }

    /**
     * 列表
     */
    @ApiOperation(value = "", httpMethod = "GET")
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = taskService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @ApiOperation(value = "", httpMethod = "GET")
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id) throws JsonProcessingException {
        TaskEntity task = taskService.getById(id);

        return R.ok().put("task", taskConverter.po2Vo(task));
    }

    /**
     * 信息
     */
    @ApiOperation(value = "", httpMethod = "GET")
    @RequestMapping("/job/{key}")
    public R info(@PathVariable("key") String jobKey) throws JsonProcessingException {
        TaskEntity task = taskService.getOne(new QueryWrapper<TaskEntity>().eq("job_key", jobKey));

        return R.ok().put("task", taskConverter.po2Vo(task));
    }

    /**
     * 保存
     */
    @ApiOperation(value = "", httpMethod = "POST")
    @RequestMapping("/save")
    public R save(@RequestBody TaskEntity task) {
        task.setStatus(TaskStatusEnum.PENDING.getCode());
        taskService.save(task);

        return R.ok();
    }

    /**
     * 工作流任务统一保存
     */
    @ApiOperation(value = "", httpMethod = "POST")
    @RequestMapping("/save/batch")
    public R saveFromWorkflow(@RequestBody List<TaskEntity> taskList) throws SchedulerException {
        List<TaskEntity> expiredTasks = taskService.list(new QueryWrapper<TaskEntity>()
                .eq(!taskList.isEmpty(), "workflow_uuid", taskList.get(0).getWorkflowUuid()));
        for (TaskEntity task : expiredTasks) {
            // 重启工作流兼容处理
            taskSchedulerService.deleteTask(task.getUuid(), task.getWorkflowUuid());
        }
        for (TaskEntity task : taskList) {
            task.setStatus(TaskStatusEnum.PENDING.getCode());
            taskService.save(task);
        }

        return R.ok();
    }

    /**
     * 修改
     */
    @ApiOperation(value = "", httpMethod = "POST")
    @RequestMapping("/update")
    public R update(@RequestBody TaskEntity task) {
        if (task.getStatus() == null)
            task.setStatus(TaskStatusEnum.PENDING.getCode());
        taskService.saveOrUpdate(task);

        return R.ok();
    }

    /**
     * 删除
     */
    @ApiOperation(value = "", httpMethod = "POST")
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids) throws SchedulerException {
        for (Long id : ids) {
            TaskEntity task = taskService.getById(id);
            String jobKey = task.getJobKey();
            String[] parts = jobKey.split("\\.");  // 使用正则表达式分割字符串
            String jobGroup = parts[0];  // jobGroup 是第一个部分
            String jobName = parts[1];   // jobName 是第二个部分
            taskSchedulerService.deleteTask(jobName, jobGroup);
        }

        return R.ok();
    }

}
