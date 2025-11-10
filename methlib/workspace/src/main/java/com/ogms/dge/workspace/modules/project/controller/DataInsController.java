package com.ogms.dge.workspace.modules.project.controller;

import com.ogms.dge.workspace.common.utils.PageUtils;
import com.ogms.dge.workspace.common.utils.R;
import com.ogms.dge.workspace.modules.project.entity.DataInsEntity;
import com.ogms.dge.workspace.modules.project.service.DataInsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;


/**
 * 工作空间数据实例
 *
 * @author Lingkai Shi
 * @email lingkai.shi@nnu.edu.cn
 * @date 2024-11-08 21:48:58
 */
@Api
@RestController
@RequestMapping("project/datains")
public class DataInsController {
    @Autowired
    private DataInsService dataInsService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    @ApiOperation(value = "", httpMethod = "GET")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = dataInsService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    @ApiOperation(value = "", httpMethod = "GET")
    public R info(@PathVariable("id") Long id){
		DataInsEntity dataIns = dataInsService.getById(id);

        return R.ok().put("dataIns", dataIns);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @ApiOperation(value = "", httpMethod = "POST")
    public R save(@RequestBody DataInsEntity dataIns){
        dataIns.setCreateTime(new Date());
		dataInsService.save(dataIns);

        return R.ok();
    }

    /**
     * 同步
     */
    @RequestMapping("/sync")
    @ApiOperation(value = "", httpMethod = "POST")
    public R sync(@RequestBody DataInsEntity dataIns) throws IOException {
        dataInsService.sync(dataIns);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @ApiOperation(value = "", httpMethod = "POST")
    public R update(@RequestBody DataInsEntity dataIns){
		dataInsService.updateById(dataIns);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @ApiOperation(value = "", httpMethod = "POST")
    public R delete(@RequestBody Long[] ids){
		dataInsService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
