package com.ogms.dge.workspace.modules.project.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ogms.dge.workspace.modules.project.entity.DataInsEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工作空间数据实例
 * 
 * @author Lingkai Shi
 * @email lingkai.shi@nnu.edu.cn
 * @date 2024-11-08 21:48:58
 */
@Mapper
public interface DataInsDao extends BaseMapper<DataInsEntity> {
	
}
