package com.ogms.dge.workspace.modules.workflow.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ogms.dge.workspace.modules.workflow.entity.TaskEntity;
import com.ogms.dge.workspace.modules.workflow.vo.TaskVo;
import com.ogms.dge.workspace.modules.workspace.entity.WsEntity;
import com.ogms.dge.workspace.modules.workspace.vo.WsVo;
import org.mapstruct.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @name: TaskConverter
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 12/17/2024 9:17 PM
 * @version: 1.0
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TaskConverter {
    ObjectMapper objectMapper = new ObjectMapper();

    @Mapping(target = "configMap", ignore = true)
    TaskVo po2Vo(TaskEntity taskEntity) throws JsonProcessingException;

    @AfterMapping
    default void handleCustomFields(TaskEntity taskEntity, @MappingTarget TaskVo taskVo) throws JsonProcessingException {
        if (taskEntity.getConfig() == null)
            taskVo.setConfigMap(null);
        else
            taskVo.setConfigMap(objectMapper.readValue(taskEntity.getConfig(), Map.class));
    }

    default List<TaskVo> poList2VoList(List<TaskEntity> taskEntityList) {
        return taskEntityList.stream()
                .map(task -> {
                    try {
                        return po2Vo(task);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }
}
