package com.ogms.dge.workspace.modules.workflow.vo;

import com.ogms.dge.workspace.modules.workflow.entity.WorkflowEntity;
import lombok.Data;

import java.util.Map;

/**
 * @name: WorkflowVo
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 11/19/2024 4:40 PM
 * @version: 1.0
 */
@Data
public class WorkflowVo extends WorkflowEntity {
    /**
     * 工作流结构信息(String)
     */
    private String json;

    /**
     * 工作流结构信息(JSON)
     */
    private Map<String, Object> jsonMap;
}
