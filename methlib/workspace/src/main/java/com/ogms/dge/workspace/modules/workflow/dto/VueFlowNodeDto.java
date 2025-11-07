package com.ogms.dge.workspace.modules.workflow.dto;

import lombok.Data;

import java.util.Map;

/**
 * @name: VueFlowNodeDto
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 12/17/2024 9:38 PM
 * @version: 1.0
 */
@Data
public class VueFlowNodeDto {
    private String id;
    private String type;
    private Map<String, Object> data;
}
