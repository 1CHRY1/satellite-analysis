package com.ogms.dge.container.modules.method.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ogms.dge.container.modules.method.entity.MethodEntity;
import com.ogms.dge.container.modules.method.service.MethodTagService;
import com.ogms.dge.container.modules.method.vo.MethodVo;
import org.mapstruct.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @name: MethodConverter
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 9/10/2024 9:34 PM
 * @version: 1.0
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MethodConverter {

    ObjectMapper objectMapper = new ObjectMapper();

    @Mapping(target = "params", ignore = true)  // 自定义处理 params 字段
    @Mapping(target = "tagIdList", ignore = true)  // 自定义处理 tagIdList 字段
    MethodVo po2Vo(MethodEntity method, @Context MethodTagService methodTagService);

    default List<MethodVo> poList2VoList(List<MethodEntity> methodList, @Context MethodTagService methodTagService) {
        return methodList.stream()
                .map(method -> po2Vo(method, methodTagService))  // 传递 methodTagService 和 objectMapper
                .collect(Collectors.toList());
    }

    // 使用 @AfterMapping 处理复杂字段转换
    @AfterMapping
    default void handleCustomFields(MethodEntity method, @MappingTarget MethodVo methodVo,
                                    @Context MethodTagService methodTagService) {
        try {
            // 处理 params 字段
            List<Map<String, Object>> params = objectMapper.readValue(method.getParams(),
                    new TypeReference<List<Map<String, Object>>>() {});
            methodVo.setParams(params);

            // 处理 tagIdList 字段
            List<Long> tagIdList = methodTagService.queryTagIdList(method.getId());
            methodVo.setTagIdList(tagIdList);
        } catch (IOException e) {
            throw new RuntimeException("Error converting params field", e);
        }
    }
}
