package com.ogms.dge.container.modules.method.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ogms.dge.container.modules.method.entity.MethodEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据处理方法
 * 
 * @author chenshun
 * @email sunlightcs@gmail.com
 * @date 2024-08-13 18:45:08
 */
@Mapper
public interface MethodDao extends BaseMapper<MethodEntity> {
	
}
