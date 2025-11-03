package com.ogms.dge.container.modules.common.converter;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ogms.dge.container.modules.common.entity.DataSourceEntity;
import com.ogms.dge.container.modules.common.service.DataSourceService;
import com.ogms.dge.container.modules.common.vo.DataSourceVo;
import com.ogms.dge.container.modules.data.entity.ServiceEntity;
import com.ogms.dge.container.modules.data.entity.SourceEntity;
import com.ogms.dge.container.modules.data.service.SourceService;
import com.ogms.dge.container.modules.data.vo.ServiceVo;
import com.ogms.dge.container.modules.sys.service.SysUserService;
import org.mapstruct.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @name: DataSourceConverter
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 10/22/2024 5:22 PM
 * @version: 1.0
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DataSourceConverter {

    ObjectMapper objectMapper = new ObjectMapper();

    @Mapping(target = "jsonSchemaMap", ignore = true)
    DataSourceVo po2Vo(DataSourceEntity dsEntity) throws JsonProcessingException;

    @AfterMapping
    default void handleCustomFields(DataSourceEntity dsEntity, @MappingTarget DataSourceVo dsVo) throws JsonProcessingException {
        if (dsEntity.getJsonSchema() == null)
            dsVo.setJsonSchemaMap(null);
        else dsVo.setJsonSchemaMap(objectMapper.readValue(dsEntity.getJsonSchema(), Map.class));
    }

    default List<DataSourceVo> poList2VoList(List<DataSourceEntity> dsEntityList) {
        return dsEntityList.stream()
                .map(ds -> {
                    try {
                        return po2Vo(ds);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }
}
