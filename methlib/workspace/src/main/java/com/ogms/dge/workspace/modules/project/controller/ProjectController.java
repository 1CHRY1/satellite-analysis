package com.ogms.dge.workspace.modules.project.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ogms.dge.workspace.common.utils.PageUtils;
import com.ogms.dge.workspace.common.utils.R;
import com.ogms.dge.workspace.modules.project.converter.ProjectConverter;
import com.ogms.dge.workspace.modules.project.entity.ProjectEntity;
import com.ogms.dge.workspace.modules.project.service.ProjectService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.UUID;


/**
 * 项目
 *
 * @author Lingkai Shi
 * @email lingkai.shi@nnu.edu.cn
 * @date 2024-11-08 20:54:31
 */
@Api
@RestController
@RequestMapping("project")
public class ProjectController {
    @Autowired
    private ProjectService projectService;

    @Resource
    private ProjectConverter projectConverter;

    /**
     * 列表
     */
    @RequestMapping("/list")
    @ApiOperation(value = "uuid下项目列表", httpMethod = "GET")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = projectService.queryPage(params);
        System.out.println(page);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    @ApiOperation(value = "", httpMethod = "GET")
    public R info(@PathVariable("id") Long id) throws JsonProcessingException {
		ProjectEntity project = projectService.getById(id);

        return R.ok().put("project", projectConverter.po2Vo(project));
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @ApiOperation(value = "", httpMethod = "POST")
    public R save(@RequestBody ProjectEntity project){
        project.setCreateTime(new Date());
        project.setUpdateTime(new Date());
        project.setUuid(UUID.randomUUID().toString());
		projectService.save(project);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @ApiOperation(value = "", httpMethod = "POST")
    public R update(@RequestBody ProjectEntity project){
        project.setUpdateTime(new Date());
		projectService.updateById(project);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @ApiOperation(value = "", httpMethod = "POST")
    public R delete(@RequestBody Long[] ids){
		projectService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
