package com.ogms.dge.workspace.modules.workspace.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ogms.dge.workspace.common.utils.PageUtils;
import com.ogms.dge.workspace.modules.workspace.entity.WsEntity;

import java.util.Map;

/**
 * 工作空间
 *
 * @author ${author}
 * @email ${email}
 * @date 2024-11-02 14:41:49
 */
public interface WsService extends IService<WsEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

