package com.ogms.dge.workspace.modules.workspace.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ogms.dge.workspace.common.utils.PageUtils;
import com.ogms.dge.workspace.common.utils.Query;
import com.ogms.dge.workspace.modules.workspace.converter.WsConverter;
import com.ogms.dge.workspace.modules.workspace.dao.WsDao;
import com.ogms.dge.workspace.modules.workspace.entity.WsEntity;
import com.ogms.dge.workspace.modules.workspace.service.WsService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;


@Service("wsService")
public class WsServiceImpl extends ServiceImpl<WsDao, WsEntity> implements WsService {

    @Resource
    private WsConverter wsConverter;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<WsEntity> page = this.page(
                new Query<WsEntity>().getPage(params),
                new QueryWrapper<WsEntity>()
        );
        PageUtils pageUtils = new PageUtils(page);
        pageUtils.setList(wsConverter.poList2VoList(page.getRecords()));

        return pageUtils;
    }
}