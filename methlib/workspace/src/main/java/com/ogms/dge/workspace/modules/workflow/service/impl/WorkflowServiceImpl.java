package com.ogms.dge.workspace.modules.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ogms.dge.workspace.common.utils.PageUtils;
import com.ogms.dge.workspace.common.utils.Query;
import com.ogms.dge.workspace.modules.workflow.converter.WorkflowConverter;
import com.ogms.dge.workspace.modules.workflow.dao.WorkflowDao;
import com.ogms.dge.workspace.modules.workflow.entity.WorkflowEntity;
import com.ogms.dge.workspace.modules.workflow.service.TaskService;
import com.ogms.dge.workspace.modules.workflow.service.WorkflowExecutionService;
import com.ogms.dge.workspace.modules.workflow.service.WorkflowService;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Map;


@Service("workflowService")
public class WorkflowServiceImpl extends ServiceImpl<WorkflowDao, WorkflowEntity> implements WorkflowService {

    @Resource
    private WorkflowConverter workflowConverter;

    @Autowired
    private TaskService taskService;

    @Autowired
    WorkflowExecutionService workflowExecutionService;

    @Value("${workspace.workflow.fd}")
    private String workflow_fd;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<WorkflowEntity> page = this.page(
                new Query<WorkflowEntity>().getPage(params),
                new QueryWrapper<WorkflowEntity>()
                        .eq("project_uuid", params.get("projectUuid"))
        );
        PageUtils pageUtils = new PageUtils(page);
        pageUtils.setList(workflowConverter.poList2VoList(page.getRecords(), workflow_fd));

        return pageUtils;
    }

    @Override
    public void start(String workflowUuid) throws IOException {
        // 开始执行工作流
        workflowExecutionService.executeWorkflow(workflowUuid);
    }

    @Override
    public void stop(String workflowUuid) throws SchedulerException {
        // 终止工作流
        workflowExecutionService.stopWorkflow(workflowUuid);
    }
}
