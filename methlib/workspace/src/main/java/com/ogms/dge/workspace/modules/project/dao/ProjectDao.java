package com.ogms.dge.workspace.modules.project.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ogms.dge.workspace.modules.project.entity.ProjectEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 项目
 * 
 * @author Lingkai Shi
 * @email lingkai.shi@nnu.edu.cn
 * @date 2024-11-08 20:54:31
 */
@Mapper
public interface ProjectDao extends BaseMapper<ProjectEntity> {
	
}
