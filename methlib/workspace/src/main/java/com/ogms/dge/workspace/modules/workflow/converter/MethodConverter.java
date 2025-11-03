package com.ogms.dge.workspace.modules.workflow.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ogms.dge.workspace.modules.workflow.dto.MethodDto;
import org.mapstruct.*;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @name: MethodConverter
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 12/20/2024 2:15 PM
 * @version: 1.0
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MethodConverter {

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id", qualifiedByName = "objectToLong")
    @Mapping(target = "name", source = "name", qualifiedByName = "objectToString")
    @Mapping(target = "description", source = "description", qualifiedByName = "objectToString")
    @Mapping(target = "longDesc", source = "longDesc", qualifiedByName = "objectToString")
    @Mapping(target = "copyright", source = "copyright", qualifiedByName = "objectToString")
    @Mapping(target = "category", source = "category", qualifiedByName = "objectToString")
    @Mapping(target = "uuid", source = "uuid", qualifiedByName = "objectToString")
    @Mapping(target = "type", source = "type", qualifiedByName = "objectToString")
    @Mapping(target = "params", source = "params", qualifiedByName = "listToString")
    @Mapping(target = "execution", source = "execution", qualifiedByName = "objectToString")
    @Mapping(target = "inputSchema", source = "inputSchema", qualifiedByName = "objectToString")
    @Mapping(target = "outputSchema", source = "outputSchema", qualifiedByName = "objectToString")
    @Mapping(target = "createUserId", source = "createUserId", qualifiedByName = "objectToLong")
    @Mapping(target = "createTime", source = "createTime", qualifiedByName = "objectToDate")
    MethodDto map2Dto(Map<String, Object> method);

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

    @Named("listToString")
    default String listToString(Object value) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(value);
    }

    @Named("objectToDate")
    default Date objectToDate(Object value) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // 更改为需要的格式

        if (value instanceof Date) {
            return (Date) value;
        } else if (value instanceof String) {
            try {
                return dateFormat.parse((String) value);
            } catch (ParseException e) {
                throw new IllegalArgumentException("Invalid date format: " + value, e);
            }
        } else if (value instanceof Long) {
            return new Date((Long) value); // 假设是时间戳
        }
        throw new IllegalArgumentException("Unsupported type for date conversion: " + value.getClass().getName());
    }
}
