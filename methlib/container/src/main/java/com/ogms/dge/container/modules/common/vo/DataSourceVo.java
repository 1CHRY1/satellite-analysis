package com.ogms.dge.container.modules.common.vo;

import com.ogms.dge.container.modules.common.entity.DataSourceEntity;
import lombok.Data;

import java.util.Map;

/**
 * @name: DataSourceVo
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 10/22/2024 5:20 PM
 * @version: 1.0
 */
@Data
public class DataSourceVo extends DataSourceEntity {
    private Map<String, Object> jsonSchemaMap;
}
