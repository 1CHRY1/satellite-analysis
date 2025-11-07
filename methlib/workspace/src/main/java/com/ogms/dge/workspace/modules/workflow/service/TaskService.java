package com.ogms.dge.workspace.modules.workflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ogms.dge.workspace.common.utils.PageUtils;
import com.ogms.dge.workspace.modules.workflow.entity.TaskEntity;

import java.util.List;
import java.util.Map;

/**
 * 任务
 *
 * @author Lingkai Shi
 * @email lingkai.shi@nnu.edu.cn
 * @date 2024-11-13 16:02:54
 */
public interface TaskService extends IService<TaskEntity> {

    PageUtils queryPage(Map<String, Object> params);

    List<TaskEntity> getByWorkflow(String workflowUuid);
}

