package com.ogms.dge.workspace.modules.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ogms.dge.workspace.common.utils.PageUtils;
import com.ogms.dge.workspace.modules.project.entity.ProjectEntity;

import java.util.Map;

/**
 * 项目
 *
 * @author Lingkai Shi
 * @email lingkai.shi@nnu.edu.cn
 * @date 2024-11-08 20:54:31
 */
public interface ProjectService extends IService<ProjectEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

