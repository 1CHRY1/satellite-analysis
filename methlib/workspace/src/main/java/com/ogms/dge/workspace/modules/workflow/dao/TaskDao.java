package com.ogms.dge.workspace.modules.workflow.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ogms.dge.workspace.modules.workflow.entity.TaskEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务
 * 
 * @author Lingkai Shi
 * @email lingkai.shi@nnu.edu.cn
 * @date 2024-11-13 16:02:54
 */
@Mapper
public interface TaskDao extends BaseMapper<TaskEntity> {
	
}
