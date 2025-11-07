package com.ogms.dge.container.modules.method.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ogms.dge.container.common.utils.MapUtils;
import com.ogms.dge.container.common.utils.PageUtils;
import com.ogms.dge.container.common.utils.Query;
import com.ogms.dge.container.modules.method.dao.MethodTagDao;
import com.ogms.dge.container.modules.method.entity.MethodTagEntity;
import com.ogms.dge.container.modules.method.service.MethodTagService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;


@Service("methodTagService")
public class MethodTagServiceImpl extends ServiceImpl<MethodTagDao, MethodTagEntity> implements MethodTagService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MethodTagEntity> page = this.page(
                new Query<MethodTagEntity>().getPage(params),
                new QueryWrapper<MethodTagEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void saveOrUpdate(Long methodId, List<Long> tagIdList) {
        //先删除方法与标签关系
        this.removeByMap(new MapUtils().put("method_id", methodId));

        if(tagIdList == null || tagIdList.size() == 0){
            return ;
        }

        //保存方法与标签关系
        for(Long tagId : tagIdList){
            MethodTagEntity methodTagEntity = new MethodTagEntity();
            methodTagEntity.setMethodId(methodId);
            methodTagEntity.setTagId(tagId);

            this.save(methodTagEntity);
        }
    }

    @Override
    public List<Long> queryTagIdList(Long methodId) {
        return baseMapper.queryTagIdList(methodId);
    }

    @Override
    public int deleteBatchByTagIds(Long[] tagIds) {
        return baseMapper.deleteBatchByTagIds(tagIds);
    }

    @Override
    public int deleteBatchByMethodIds(Long[] methodIds) {
        return baseMapper.deleteBatchByMethodIds(methodIds);
    }
}