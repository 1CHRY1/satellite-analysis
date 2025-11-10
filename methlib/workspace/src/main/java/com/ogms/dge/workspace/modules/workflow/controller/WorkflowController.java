package com.ogms.dge.workspace.modules.workflow.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ogms.dge.workspace.common.utils.FileUtils;
import com.ogms.dge.workspace.common.utils.PageUtils;
import com.ogms.dge.workspace.common.utils.R;
import com.ogms.dge.workspace.modules.workflow.converter.WorkflowConverter;
import com.ogms.dge.workspace.modules.workflow.entity.WorkflowEntity;
import com.ogms.dge.workspace.modules.workflow.service.WorkflowExecutionService;
import com.ogms.dge.workspace.modules.workflow.service.WorkflowService;
import com.ogms.dge.workspace.modules.workflow.vo.WorkflowVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;



/**
 * 工作流
 *
 * @author Lingkai Shi
 * @email lingkai.shi@nnu.edu.cn
 * @date 2024-11-19 15:19:51
 */
@Api
@RestController
@RequestMapping("workflow")
public class WorkflowController {
    @Autowired
    private WorkflowService workflowService;

    @Resource
    private WorkflowConverter workflowConverter;

    @Value("${workspace.workflow.fd}")
    private String workflow_fd;

    ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private WorkflowExecutionService workflowExecutionService;

    /**
     * 列表
     */
    @ApiOperation(value = "", httpMethod = "GET")
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = workflowService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @ApiOperation(value = "", httpMethod = "GET")
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id) throws IOException {
        WorkflowEntity workflow = workflowService.getById(id);

        return R.ok().put("workflow", workflowConverter.po2Vo(workflow, workflow_fd));
    }

    @ApiOperation(value = "", httpMethod = "GET")
    @RequestMapping("/info/uuid/{uuid}")
    public R info(@PathVariable("uuid") String uuid) throws IOException {
        WorkflowEntity workflow = workflowService.getOne(new QueryWrapper<WorkflowEntity>().eq("uuid", uuid));

        return R.ok().put("workflow", workflowConverter.po2Vo(workflow, workflow_fd));
    }

    @ApiOperation(value = "", httpMethod = "GET")
    @RequestMapping("/start/{uuid}")
    public R start(@PathVariable("uuid") String uuid) throws IOException {
        workflowService.start(uuid);
        return R.ok();
    }

    /**
     * 保存
     */
    @ApiOperation(value = "", httpMethod = "POST")
    @RequestMapping("/save")
    public R save(@RequestBody WorkflowVo workflow) throws IOException {
        workflowService.save(workflowConverter.vo2Po(workflow));
        FileUtils.saveToFile(objectMapper.readValue(workflow.getJson(), Map.class), workflow_fd, workflow.getUuid() + ".json");
        return R.ok();
    }

    /**
     * 修改
     */
    @ApiOperation(value = "", httpMethod = "POST")
    @RequestMapping("/update")
    public R update(@RequestBody WorkflowVo workflow) throws IOException {
        workflowService.updateById(workflowConverter.vo2Po(workflow));
        FileUtils.saveToFile(objectMapper.readValue(workflow.getJson(), Map.class), workflow_fd, workflow.getUuid() + ".json");
        workflowExecutionService.onWorkflowUpdated(workflow.getUuid());
        return R.ok();
    }

    /**
     * 终止
     */
    @ApiOperation(value = "", httpMethod = "GET")
    @RequestMapping("/stop/{uuid}")
    public R stop(@PathVariable("uuid") String uuid) throws SchedulerException {
        workflowService.stop(uuid);
        return R.ok();
    }

    /**
     * 删除
     */
    @ApiOperation(value = "", httpMethod = "POST")
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids){
        workflowService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
