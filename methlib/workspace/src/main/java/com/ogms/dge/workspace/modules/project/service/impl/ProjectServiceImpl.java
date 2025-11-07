package com.ogms.dge.workspace.modules.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ogms.dge.workspace.common.utils.PageUtils;
import com.ogms.dge.workspace.common.utils.Query;
import com.ogms.dge.workspace.modules.project.converter.ProjectConverter;
import com.ogms.dge.workspace.modules.project.dao.ProjectDao;
import com.ogms.dge.workspace.modules.project.entity.ProjectEntity;
import com.ogms.dge.workspace.modules.project.service.ProjectService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;


@Service("projectService")
public class ProjectServiceImpl extends ServiceImpl<ProjectDao, ProjectEntity> implements ProjectService {

    @Resource
    private ProjectConverter projectConverter;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<ProjectEntity> page = this.page(
                new Query<ProjectEntity>().getPage(params),
                new QueryWrapper<ProjectEntity>()
                        .eq(params.containsKey("workspaceUuid"), "workspace_uuid", params.get("workspaceUuid"))
        );
        PageUtils pageUtils = new PageUtils(page);
        pageUtils.setList(projectConverter.poList2VoList(page.getRecords()));

        return pageUtils;
    }

}
