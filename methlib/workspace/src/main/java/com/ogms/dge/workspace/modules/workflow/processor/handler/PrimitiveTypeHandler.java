package com.ogms.dge.workspace.modules.workflow.processor.handler;

/**
 * @name: PrimitiveTypeHandler
 * @description: 基础类型处理器
 * @author: Lingkai Shi
 * @date: 12/19/2024 4:00 PM
 * @version: 1.0
 */
import com.ogms.dge.workspace.modules.workflow.dto.CmdContextDto;
import com.ogms.dge.workspace.modules.workflow.processor.ParameterHandler;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Objects;

@Component
public class PrimitiveTypeHandler implements ParameterHandler {

    @Override
    public boolean supports(Object parameterType) {
        if (parameterType instanceof String) {
            String type = (String) parameterType;
            return Arrays.asList("Boolean", "Integer", "Float", "String", "StringOrNumber").contains(type);
        }
        return false;
    }

    @Override
    public void parse(Object parameterType, Object rawValue, CmdContextDto context) {
        if (rawValue == null) {
            handleNull(rawValue, context.getCmdBuilder(), context.getParamSpecs());
        } else {
            context.getCmdBuilder().append(Objects.toString(rawValue)).append(" ");
        }
    }
}
