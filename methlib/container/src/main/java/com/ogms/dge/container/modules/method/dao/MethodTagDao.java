package com.ogms.dge.container.modules.method.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ogms.dge.container.modules.method.entity.MethodTagEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 方法与标签对应关系
 *
 * @author chenshun
 * @email sunlightcs@gmail.com
 * @date 2024-08-14 10:25:54
 */
@Mapper
public interface MethodTagDao extends BaseMapper<MethodTagEntity> {
    /**
     * 根据方法ID，获取标签ID列表
     */
    List<Long> queryTagIdList(Long methodId);


    /**
     * 根据标签ID数组，批量删除
     */
    int deleteBatchByTagIds(Long[] tagIds);

    /**
     * 根据方法ID数组，批量删除
     */
    int deleteBatchByMethodIds(Long[] methodIds);
}
