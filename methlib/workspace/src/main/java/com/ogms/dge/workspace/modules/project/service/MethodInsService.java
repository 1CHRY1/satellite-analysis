package com.ogms.dge.workspace.modules.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ogms.dge.workspace.common.utils.PageUtils;
import com.ogms.dge.workspace.modules.project.entity.DataInsEntity;
import com.ogms.dge.workspace.modules.project.entity.MethodInsEntity;

import java.io.IOException;
import java.util.Map;

/**
 * 工作空间方法实例
 *
 * @author Lingkai Shi
 * @email lingkai.shi@nnu.edu.cn
 * @date 2024-11-19 15:32:37
 */
public interface MethodInsService extends IService<MethodInsEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void sync(MethodInsEntity methodIns) throws IOException;
}

