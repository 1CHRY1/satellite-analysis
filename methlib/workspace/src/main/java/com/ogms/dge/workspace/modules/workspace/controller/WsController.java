package com.ogms.dge.workspace.modules.workspace.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.ogms.dge.workspace.common.utils.PageUtils;
import com.ogms.dge.workspace.common.utils.R;
import com.ogms.dge.workspace.modules.workspace.converter.WsConverter;
import com.ogms.dge.workspace.modules.workspace.entity.WsEntity;
import com.ogms.dge.workspace.modules.workspace.service.WsService;
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
 * 工作空间
 *
 * @author ${author}
 * @email ${email}
 * @date 2024-11-02 14:41:49
 */
@Api
@RestController
@RequestMapping("ws")
public class WsController {
    @Autowired
    private WsService wsService;

    @Resource
    private WsConverter wsConverter;

    /**
     * 列表
     */
    @RequestMapping("/list")
    @ApiOperation(value = "", httpMethod = "GET")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = wsService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    @ApiOperation(value = "", httpMethod = "GET")
    public R info(@PathVariable("id") Long id) throws JsonProcessingException {
		WsEntity ws = wsService.getById(id);

        return R.ok().put("ws", wsConverter.po2Vo(ws));
    }

    /**
     * 信息
     */
    @RequestMapping("/info/uuid/{uuid}")
    @ApiOperation(value = "", httpMethod = "GET")
    public R infoByUuid(@PathVariable("uuid") String uuid) throws JsonProcessingException {
        WsEntity ws = wsService.getOne(new QueryWrapper<WsEntity>().eq("uuid", uuid));

        return R.ok().put("source", wsConverter.po2Vo(ws));
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @ApiOperation(value = "", httpMethod = "POST")
    public R save(@RequestBody WsEntity ws){
        ws.setCreateTime(new Date());
        ws.setUpdateTime(new Date());
        ws.setUuid(UUID.randomUUID().toString());
		wsService.save(ws);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @ApiOperation(value = "", httpMethod = "POST")
    public R update(@RequestBody WsEntity ws){
        ws.setUpdateTime(new Date());
		wsService.updateById(ws);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @ApiOperation(value = "", httpMethod = "POST")
    public R delete(@RequestBody Long[] ids){
		wsService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
