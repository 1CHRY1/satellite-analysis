package com.ogms.dge.container.modules.method.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ogms.dge.container.modules.method.entity.TagEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 标签
 * 
 * @author chenshun
 * @email sunlightcs@gmail.com
 * @date 2024-08-14 10:25:54
 */
@Mapper
public interface TagDao extends BaseMapper<TagEntity> {
	
}
