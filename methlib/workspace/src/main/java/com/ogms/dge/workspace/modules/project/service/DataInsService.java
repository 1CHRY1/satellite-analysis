package com.ogms.dge.workspace.modules.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ogms.dge.workspace.common.utils.PageUtils;
import com.ogms.dge.workspace.modules.project.entity.DataInsEntity;

import java.io.IOException;
import java.util.Map;

/**
 * 工作空间数据实例
 *
 * @author Lingkai Shi
 * @email lingkai.shi@nnu.edu.cn
 * @date 2024-11-08 21:48:58
 */
public interface DataInsService extends IService<DataInsEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void sync(DataInsEntity dataIns) throws IOException;
}

