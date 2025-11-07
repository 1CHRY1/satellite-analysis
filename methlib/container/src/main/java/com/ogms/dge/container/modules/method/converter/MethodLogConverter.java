package com.ogms.dge.container.modules.method.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ogms.dge.container.modules.method.entity.MethodLogEntity;
import com.ogms.dge.container.modules.method.vo.MethodLogVo;
import org.mapstruct.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @name: MethodLogConverter
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 9/10/2024 9:07 PM
 * @version: 1.0
 */
@Mapper(componentModel = "spring")
public interface MethodLogConverter {
    ObjectMapper objectMapper = new ObjectMapper();

    @Mappings({
            @Mapping(target = "inputFiles", ignore = true),
            @Mapping(target = "outputFiles", ignore = true)
    })
    MethodLogVo po2Vo(MethodLogEntity methodLog);

    default List<MethodLogVo> poList2VoList(List<MethodLogEntity> methodLogList) {
        return methodLogList.stream()
                .map(this::po2Vo)
                .collect(Collectors.toList());
    }

    @AfterMapping
    default void mapFiles(@MappingTarget MethodLogVo vo, MethodLogEntity entity) {
        // 将 inputFiles 和 outputFiles 从 JSON 字符串转换为 List<String>
        try {
            if (entity.getInputFiles() != null) {
                vo.setInputFiles(objectMapper.readValue(entity.getInputFiles(), List.class));
            }
            if (entity.getOutputFiles() != null) {
                vo.setOutputFiles(objectMapper.readValue(entity.getOutputFiles(), List.class));
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();  // 处理异常
        }
    }

    @AfterMapping
    default void mapFilesToEntity(@MappingTarget MethodLogEntity entity, MethodLogVo vo) {
        // 将 List<String> 转换为 JSON 字符串
        try {
            if (vo.getInputFiles() != null) {
                entity.setInputFiles(objectMapper.writeValueAsString(vo.getInputFiles()));
            }
            if (vo.getOutputFiles() != null) {
                entity.setOutputFiles(objectMapper.writeValueAsString(vo.getOutputFiles()));
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();  // 处理异常
        }
    }
}
