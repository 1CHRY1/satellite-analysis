package com.ogms.dge.workspace.modules.workflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ogms.dge.workspace.common.utils.PageUtils;
import com.ogms.dge.workspace.modules.workflow.dto.DAGDto;
import com.ogms.dge.workspace.modules.workflow.entity.TaskEntity;
import com.ogms.dge.workspace.modules.workflow.entity.WorkflowEntity;
import org.quartz.SchedulerException;

import java.io.IOException;
import java.util.Map;

/**
 * 工作流
 *
 * @author Lingkai Shi
 * @email lingkai.shi@nnu.edu.cn
 * @date 2024-11-19 15:19:51
 */
public interface WorkflowService extends IService<WorkflowEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void start(String workflowUuid) throws IOException;

    void stop(String workflowUuid) throws SchedulerException;
}

