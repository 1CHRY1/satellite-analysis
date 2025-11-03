package com.ogms.dge.container.modules.method.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ogms.dge.container.common.utils.PageUtils;
import com.ogms.dge.container.common.utils.Query;
import com.ogms.dge.container.modules.method.converter.MethodLogConverter;
import com.ogms.dge.container.modules.method.dao.MethodLogDao;
import com.ogms.dge.container.modules.method.entity.MethodLogEntity;
import com.ogms.dge.container.modules.method.service.MethodLogService;
import com.ogms.dge.container.modules.method.vo.MethodLogVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;


@Service("methodLogService")
public class MethodLogServiceImpl extends ServiceImpl<MethodLogDao, MethodLogEntity> implements MethodLogService {

    @Autowired
    MethodLogConverter methodLogConverter;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MethodLogEntity> page = this.page(
                new Query<MethodLogEntity>().getPage(params),
                new QueryWrapper<MethodLogEntity>().orderByDesc("start_time")
                // 按时间倒序排列
        );
        // 将转换后的VO列表设置回分页结果中
        IPage<MethodLogVo> voPage = new Page<>(
                page.getCurrent(),  // 当前页
                page.getSize(),     // 每页条数
                page.getTotal()     // 总条数
        );
        voPage.setRecords(methodLogConverter.poList2VoList(page.getRecords()));

        // 返回分页数据封装
        return new PageUtils(voPage);
    }

    @Override
    public int deleteBatchByMethodIds(Long[] methodIds) {
        return baseMapper.deleteBatchByMethodIds(methodIds);
    }
}
