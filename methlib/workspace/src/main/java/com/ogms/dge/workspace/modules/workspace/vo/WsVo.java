package com.ogms.dge.workspace.modules.workspace.vo;

import com.ogms.dge.workspace.modules.workspace.entity.WsEntity;
import lombok.Data;

import java.util.Map;

/**
 * @name: WsVo
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 11/2/2024 3:54 PM
 * @version: 1.0
 */
@Data
public class WsVo extends WsEntity {
    private Map<String, Object> configMap;
    /**
     * 创建者
     */
    private String createUserName;
}
