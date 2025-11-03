package com.ogms.dge.workspace.modules.workspace.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ogms.dge.workspace.modules.workspace.entity.WsEntity;
import com.ogms.dge.workspace.modules.workspace.vo.WsVo;
import org.mapstruct.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @name: WsConverter
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 11/2/2024 3:53 PM
 * @version: 1.0
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface WsConverter {
    ObjectMapper objectMapper = new ObjectMapper();

    @Mapping(target = "configMap", ignore = true)
    @Mapping(target = "createUserName", ignore = true)
    WsVo po2Vo(WsEntity wsEntity) throws JsonProcessingException;

    @AfterMapping
    default void handleCustomFields(WsEntity wsEntity, @MappingTarget WsVo wsVo) throws JsonProcessingException {
        // TODO TEMP PROCESS
        wsVo.setCreateUserName("admin");
        if (wsEntity.getConfig() == null)
            wsVo.setConfigMap(null);
        else
            wsVo.setConfigMap(objectMapper.readValue(wsEntity.getConfig(), Map.class));
    }

    default List<WsVo> poList2VoList(List<WsEntity> wsEntityList) {
        return wsEntityList.stream()
                .map(ws -> {
                    try {
                        return po2Vo(ws);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }
}
