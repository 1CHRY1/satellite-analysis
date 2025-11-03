package com.ogms.dge.workspace.modules.workflow.converter;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ogms.dge.workspace.common.utils.FileUtils;
import com.ogms.dge.workspace.modules.workflow.dto.VueFlowNodeDto;
import com.ogms.dge.workspace.modules.workflow.entity.WorkflowEntity;
import com.ogms.dge.workspace.modules.workflow.vo.WorkflowVo;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @name: WorkflowConverter
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 11/19/2024 4:38 PM
 * @version: 1.0
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface WorkflowConverter {

    ObjectMapper objectMapper = new ObjectMapper();

    @Mapping(target = "jsonMap", ignore = true)
    WorkflowVo po2Vo(WorkflowEntity workflow, @Context String directory) throws IOException;

    WorkflowEntity vo2Po(WorkflowVo workflow) throws JsonProcessingException;

    // 将单个 Node 转换为 VueFlowNodeDto
    @Named("nodeMap2NodeDto")
    @Mapping(target = "id", source = "id", qualifiedByName = "objectToString")
    @Mapping(target = "type", source = "type", qualifiedByName = "objectToString")
    @Mapping(target = "data", source = "data", qualifiedByName = "objectToMap")
    VueFlowNodeDto nodeMap2NodeDto(Map<String, Object> node);

    // 将 workflow 的 nodes 字段转换为 List<VueFlowNodeDto>
    @BeanMapping(ignoreByDefault = true)
    @IterableMapping(qualifiedByName = "nodeMap2NodeDto")
    List<VueFlowNodeDto> map2NodeDto(List<Map<String, Object>> nodes);

    @Named("objectToLong")
    default Long objectToLong(Object value) {
        if (value instanceof Long) {
            return (Long) value; // 如果已经是 Long，直接返回
        } else if (value instanceof Integer) {
            return ((Integer) value).longValue(); // 直接从 Integer 转为 Long
        } else if (value instanceof String) {
            try {
                return Long.parseLong((String) value); // 处理 String 转换为 Long
            } catch (NumberFormatException e) {
                // 根据需要可以选择抛出异常或返回 null
                return null;
            }
        }
        return null; // 如果类型不匹配，返回 null 或抛出异常
    }

    @Named("objectToString")
    default String objectToString(Object value) {
        if (value instanceof String) {
            return (String) value;
        } else if (value instanceof Integer) {
            return String.valueOf(value);
        }
        return null; // 或者可以抛出异常
    }

    @Named("objectToMap")
    default Map<String, Object> objectToListMap(Object value) {
        Map<String, Object> map = new HashMap<>();
        map.putAll((Map<String, Object>) value);
        return map;
    }

    @AfterMapping
    default void handleCustomFields(WorkflowEntity workflow, @MappingTarget WorkflowVo workflowVo, @Context String directory) throws IOException {
        workflowVo.setJsonMap(FileUtils.readFromFile(directory, workflow.getUuid() + ".json"));
    }

    @AfterMapping
    default void handleCustomFields(WorkflowVo workflowVo, @MappingTarget WorkflowEntity workflow) throws JsonProcessingException {
    }

    default List<WorkflowVo> poList2VoList(List<WorkflowEntity> workflowEntities, @Context String directory) {
        return workflowEntities.stream()
                .map(workflow -> {
                    try {
                        return po2Vo(workflow, directory);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }
}
