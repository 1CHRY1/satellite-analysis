package com.ogms.dge.workspace.modules.workflow.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ogms.dge.workspace.modules.workflow.entity.WorkflowEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工作流
 * 
 * @author Lingkai Shi
 * @email lingkai.shi@nnu.edu.cn
 * @date 2024-11-19 15:19:51
 */
@Mapper
public interface WorkflowDao extends BaseMapper<WorkflowEntity> {
	
}
