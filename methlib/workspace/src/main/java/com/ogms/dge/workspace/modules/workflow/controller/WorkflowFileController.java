package com.ogms.dge.workspace.modules.workflow.controller;

import com.ogms.dge.workspace.common.utils.PageUtils;
import com.ogms.dge.workspace.common.utils.R;
import com.ogms.dge.workspace.modules.workflow.entity.WorkflowFileEntity;
import com.ogms.dge.workspace.modules.workflow.service.WorkflowFileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;



/**
 * 工作流中间文件
 *
 * @author Lingkai Shi
 * @email lingkai.shi@nnu.edu.cn
 * @date 2024-11-19 15:19:51
 */
@Api
@RestController
@RequestMapping("workflow/workflowfile")
public class WorkflowFileController {
    @Autowired
    private WorkflowFileService workflowFileService;

    /**
     * 列表
     */
    @ApiOperation(value = "", httpMethod = "GET")
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = workflowFileService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @ApiOperation(value = "", httpMethod = "GET")
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id){
		WorkflowFileEntity workflowFile = workflowFileService.getById(id);

        return R.ok().put("workflowFile", workflowFile);
    }

    /**
     * 保存
     */
    @ApiOperation(value = "", httpMethod = "POST")
    @RequestMapping("/save")
    public R save(@RequestBody WorkflowFileEntity workflowFile){
		workflowFileService.save(workflowFile);

        return R.ok();
    }

    /**
     * 修改
     */
    @ApiOperation(value = "", httpMethod = "POST")
    @RequestMapping("/update")
    public R update(@RequestBody WorkflowFileEntity workflowFile){
		workflowFileService.updateById(workflowFile);

        return R.ok();
    }

    /**
     * 删除
     */
    @ApiOperation(value = "", httpMethod = "POST")
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids){
		workflowFileService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
