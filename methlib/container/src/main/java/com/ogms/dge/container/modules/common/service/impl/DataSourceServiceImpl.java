package com.ogms.dge.container.modules.common.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ogms.dge.container.common.utils.PageUtils;
import com.ogms.dge.container.common.utils.Query;
import com.ogms.dge.container.modules.common.converter.DataSourceConverter;
import com.ogms.dge.container.modules.common.dao.DataSourceDao;
import com.ogms.dge.container.modules.common.entity.DataSourceEntity;
import com.ogms.dge.container.modules.common.service.DataSourceService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;


@Service("dataSourceService")
public class DataSourceServiceImpl extends ServiceImpl<DataSourceDao, DataSourceEntity> implements DataSourceService {

    @Resource
    private DataSourceConverter dataSourceConverter;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<DataSourceEntity> page = this.page(
                new Query<DataSourceEntity>().getPage(params),
                new QueryWrapper<DataSourceEntity>().orderByAsc("classification")
                        .orderByAsc("name")
        );
        PageUtils pageUtils = new PageUtils(page);
        pageUtils.setList(dataSourceConverter.poList2VoList(page.getRecords()));

        return pageUtils;
    }

}