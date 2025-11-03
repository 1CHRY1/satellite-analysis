package com.ogms.dge.workspace.modules.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ogms.dge.workspace.common.utils.PageUtils;
import com.ogms.dge.workspace.common.utils.Query;
import com.ogms.dge.workspace.modules.workflow.dao.WorkflowFileDao;
import com.ogms.dge.workspace.modules.workflow.entity.WorkflowFileEntity;
import com.ogms.dge.workspace.modules.workflow.service.WorkflowFileService;
import org.springframework.stereotype.Service;

import java.util.Map;


@Service("workflowFileService")
public class WorkflowFileServiceImpl extends ServiceImpl<WorkflowFileDao, WorkflowFileEntity> implements WorkflowFileService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<WorkflowFileEntity> page = this.page(
                new Query<WorkflowFileEntity>().getPage(params),
                new QueryWrapper<WorkflowFileEntity>()
        );

        return new PageUtils(page);
    }

}