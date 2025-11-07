package com.ogms.dge.container.modules.method.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ogms.dge.container.common.utils.Constant;
import com.ogms.dge.container.common.utils.PageUtils;
import com.ogms.dge.container.common.utils.Query;
import com.ogms.dge.container.modules.method.dao.TagDao;
import com.ogms.dge.container.modules.method.entity.TagEntity;
import com.ogms.dge.container.modules.method.service.MethodTagService;
import com.ogms.dge.container.modules.method.service.TagService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;


@Service("tagService")
public class TagServiceImpl extends ServiceImpl<TagDao, TagEntity> implements TagService {

    @Autowired
    private MethodTagService methodTagService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String tagName = (String) params.get("tagName");
        Long createUserId = (Long) params.get("createUserId");

        // 处理 createUserId 和 Constant.SUPER_ADMIN 的逻辑, 默认同时包含用户自己和超级管理员默认创建的（即whitebox）
        List<Long> userIds = new ArrayList<>();
        if (createUserId != null) {
            userIds.add(createUserId);
        }
        userIds.add((long) Constant.SUPER_ADMIN);  // 始终添加 SUPER_ADMIN

        IPage<TagEntity> page = this.page(
                new Query<TagEntity>().getPage(params),
                new QueryWrapper<TagEntity>()
                        .like(StringUtils.isNotBlank(tagName), "tag_name", tagName)
                        .in(!userIds.isEmpty(), "create_user_id", userIds)
        );

        return new PageUtils(page);
    }

    @Override
    public List<TagEntity> queryAll(Long userId) {

        // 处理 createUserId 和 Constant.SUPER_ADMIN 的逻辑, 默认同时包含用户自己和超级管理员默认创建的（即whitebox）
        List<Long> userIds = new ArrayList<>();
        if (userId != null) {
            userIds.add(userId);
        }
        userIds.add((long) Constant.SUPER_ADMIN);  // 始终添加 SUPER_ADMIN
        QueryWrapper queryWrapper = new QueryWrapper<TagEntity>()
                .in(!userIds.isEmpty(), "create_user_id", userIds);

        return this.list(queryWrapper);
    }

    @Override
    public List<TagEntity> getTagListByMethodId(Long methodId) {

        //标签列表
        List<Long> tagIdList = methodTagService.queryTagIdList(methodId);
        return getTagList(tagIdList);
    }

    /**
     * 获取拥有的标签列表
     * @param tagIdList
     * @return
     */
    private List<TagEntity> getTagList(List<Long> tagIdList) {
        List<TagEntity> tags = new ArrayList<>();
        for (Long tagId : tagIdList) {
            TagEntity tagEntity = this.getById(tagId);
            tags.add(tagEntity);
        }

        return tags;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveTag(TagEntity tag) {
        tag.setCreateTime(new Date());
        this.save(tag);
    }

    @Override
    public TagEntity findByName(String name) {
        QueryWrapper<TagEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", name);
        List<TagEntity> list = this.list(queryWrapper);
        if (list.size() == 0) {
            return null;
        } else {
            return list.get(0); // 数据库UNIQUE约束
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(Long[] tagIds) {
        //删除标签
        this.removeByIds(Arrays.asList(tagIds));

        //删除方法与标签关联
        methodTagService.deleteBatchByTagIds(tagIds);
    }
}