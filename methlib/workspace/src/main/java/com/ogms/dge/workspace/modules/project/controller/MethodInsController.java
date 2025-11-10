package com.ogms.dge.workspace.modules.project.controller;

import com.ogms.dge.workspace.common.utils.PageUtils;
import com.ogms.dge.workspace.common.utils.R;
import com.ogms.dge.workspace.modules.project.entity.DataInsEntity;
import com.ogms.dge.workspace.modules.project.entity.MethodInsEntity;
import com.ogms.dge.workspace.modules.project.service.MethodInsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;


/**
 * 工作空间方法实例
 *
 * @author Lingkai Shi
 * @email lingkai.shi@nnu.edu.cn
 * @date 2024-11-19 15:32:37
 */
@Api
@RestController
@RequestMapping("project/methodins")
public class MethodInsController {
    @Autowired
    private MethodInsService methodInsService;

    /**
     * 列表
     */
    @ApiOperation(value = "", httpMethod = "GET")
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = methodInsService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @ApiOperation(value = "", httpMethod = "GET")
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id){
		MethodInsEntity methodIns = methodInsService.getById(id);

        return R.ok().put("methodIns", methodIns);
    }

    /**
     * 保存
     */
    @ApiOperation(value = "", httpMethod = "POST")
    @RequestMapping("/save")
    public R save(@RequestBody MethodInsEntity methodIns){
		methodInsService.save(methodIns);

        return R.ok();
    }

    /**
     * 同步
     */
    @RequestMapping("/sync")
    @ApiOperation(value = "", httpMethod = "POST")
    public R sync(@RequestBody MethodInsEntity methodIns) throws IOException {
        methodInsService.sync(methodIns);

        return R.ok();
    }

    /**
     * 修改
     */
    @ApiOperation(value = "", httpMethod = "POST")
    @RequestMapping("/update")
    public R update(@RequestBody MethodInsEntity methodIns){
		methodInsService.updateById(methodIns);

        return R.ok();
    }

    /**
     * 删除
     */
    @ApiOperation(value = "", httpMethod = "POST")
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids){
		methodInsService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
