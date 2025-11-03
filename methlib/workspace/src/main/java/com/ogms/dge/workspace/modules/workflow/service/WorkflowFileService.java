package com.ogms.dge.workspace.modules.workflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ogms.dge.workspace.common.utils.PageUtils;
import com.ogms.dge.workspace.modules.workflow.entity.WorkflowFileEntity;

import java.util.Map;

/**
 * 工作流中间文件
 *
 * @author Lingkai Shi
 * @email lingkai.shi@nnu.edu.cn
 * @date 2024-11-19 15:19:51
 */
public interface WorkflowFileService extends IService<WorkflowFileEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

