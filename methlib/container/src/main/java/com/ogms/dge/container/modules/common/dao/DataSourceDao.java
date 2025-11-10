package com.ogms.dge.container.modules.common.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ogms.dge.container.modules.common.entity.DataSourceEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据源类型
 * 
 * @author Lingkai Shi
 * @email lingkai.shi@nnu.edu.cn
 * @date 2024-09-23 15:23:47
 */
@Mapper
public interface DataSourceDao extends BaseMapper<DataSourceEntity> {
	
}
