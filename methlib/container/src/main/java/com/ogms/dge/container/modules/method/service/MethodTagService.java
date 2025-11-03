package com.ogms.dge.container.modules.method.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ogms.dge.container.common.utils.PageUtils;
import com.ogms.dge.container.modules.method.entity.MethodTagEntity;

import java.util.List;
import java.util.Map;

/**
 * 方法与标签对应关系
 *
 * @author chenshun
 * @email sunlightcs@gmail.com
 * @date 2024-08-14 10:25:54
 */
public interface MethodTagService extends IService<MethodTagEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveOrUpdate(Long methodId, List<Long> tagIdList);

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

