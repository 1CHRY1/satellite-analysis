package com.ogms.dge.workspace.modules.workflow.vo;

import com.ogms.dge.workspace.modules.workflow.entity.TaskEntity;
import lombok.Data;

import java.util.Map;

/**
 * @name: TaskVo
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 12/17/2024 9:14 PM
 * @version: 1.0
 */
@Data
public class TaskVo extends TaskEntity {
    private Map<String, Object> configMap;
}
