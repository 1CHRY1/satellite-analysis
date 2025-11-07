package com.ogms.dge.container.modules.method.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ogms.dge.container.common.utils.PageUtils;
import com.ogms.dge.container.modules.method.entity.TagEntity;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 标签
 *
 * @author chenshun
 * @email sunlightcs@gmail.com
 * @date 2024-08-14 10:25:54
 */
public interface TagService extends IService<TagEntity> {

    PageUtils queryPage(Map<String, Object> params);

    List<TagEntity> queryAll(Long userId);

    List<TagEntity> getTagListByMethodId(Long methodId);

    void saveTag(TagEntity tag);

    TagEntity findByName(String name);

    @Transactional(rollbackFor = Exception.class)
    void deleteBatch(Long[] tagIds);
}

