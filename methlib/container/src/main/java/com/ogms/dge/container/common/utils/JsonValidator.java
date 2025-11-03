package com.ogms.dge.container.common.utils;

import com.fasterxml.jackson.core.JsonLocation;
import com.networknt.schema.*;
import com.networknt.schema.serialization.JsonNodeReader;
import com.networknt.schema.utils.JsonNodes;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @name: JsonValidator
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 9/26/2024 9:51 AM
 * @version: 1.0
 */
public class JsonValidator {
    public static List<ValidationMessage> validateJson(String inputData, String schemaData) {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7,
                builder -> builder.jsonNodeReader(JsonNodeReader.builder().locationAware().build()));
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        JsonSchema schema = factory.getSchema(schemaData, InputFormat.JSON, config);
        Set<ValidationMessage> messages = schema.validate(inputData, InputFormat.JSON, executionContext -> {
            executionContext.getExecutionConfig().setFormatAssertionsEnabled(true);
        });
        List<ValidationMessage> list = messages.stream().collect(Collectors.toList());
        return list;
    }
}
