package com.ogms.dge.workspace.modules.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ogms.dge.workspace.common.utils.PageUtils;
import com.ogms.dge.workspace.common.utils.Query;
import com.ogms.dge.workspace.modules.workflow.converter.TaskConverter;
import com.ogms.dge.workspace.modules.workflow.dao.TaskDao;
import com.ogms.dge.workspace.modules.workflow.entity.TaskEntity;
import com.ogms.dge.workspace.modules.workflow.service.TaskService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;


@Service("taskService")
public class TaskServiceImpl extends ServiceImpl<TaskDao, TaskEntity> implements TaskService {

    @Resource
    private TaskConverter taskConverter;
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<TaskEntity> page = this.page(
                new Query<TaskEntity>().getPage(params),
                new QueryWrapper<TaskEntity>()
                        .eq(params.containsKey("workflowUuid"), "workflow_uuid", params.get("workflowUuid"))
        );
        PageUtils pageUtils = new PageUtils(page);
        pageUtils.setList(taskConverter.poList2VoList(page.getRecords()));

        return pageUtils;
    }

    @Override
    public List<TaskEntity> getByWorkflow(String workflowUuid) {
        return this.baseMapper.selectList(new QueryWrapper<TaskEntity>()
                .eq("workflow_uuid", workflowUuid));
    }

}
