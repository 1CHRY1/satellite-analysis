package com.ogms.dge.container.modules.common.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ogms.dge.container.modules.common.entity.ContainerEntity;
import com.ogms.dge.container.modules.data.dto.InstanceDto;
import org.mapstruct.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @name: ContainerConverter
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 4/2/2025 3:24 PM
 * @version: 1.0
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ContainerConverter {
    static ObjectMapper objectMapper = new ObjectMapper();

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "serverName", source = "hostName", qualifiedByName = "objectToString")
    @Mapping(target = "mac", source = "mac", qualifiedByName = "objectToString")
    @Mapping(target = "ip", source = "hostIp", qualifiedByName = "objectToString")
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "registerTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    ContainerEntity map2Po(Map<String, Object> map);

    @Named("objectToString")
    default String objectToString(Object value) {
        if (value instanceof String) {
            return (String) value;
        } else if (value instanceof Integer) {
            return String.valueOf(value);
        }
        return null; // 或者可以抛出异常
    }
}
