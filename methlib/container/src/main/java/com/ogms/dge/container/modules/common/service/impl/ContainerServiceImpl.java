package com.ogms.dge.container.modules.common.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ogms.dge.container.common.utils.PageUtils;
import com.ogms.dge.container.common.utils.Query;
import com.ogms.dge.container.modules.common.dao.ContainerDao;
import com.ogms.dge.container.modules.common.entity.ContainerEntity;
import com.ogms.dge.container.modules.common.service.ContainerService;
import org.springframework.stereotype.Service;

import java.util.Map;


@Service("containerService")
public class ContainerServiceImpl extends ServiceImpl<ContainerDao, ContainerEntity> implements ContainerService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<ContainerEntity> page = this.page(
                new Query<ContainerEntity>().getPage(params),
                new QueryWrapper<ContainerEntity>()
        );

        return new PageUtils(page);
    }

}
