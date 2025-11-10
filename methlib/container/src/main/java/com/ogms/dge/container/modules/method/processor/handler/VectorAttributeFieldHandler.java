package com.ogms.dge.container.modules.method.processor.handler;

import com.ogms.dge.container.modules.method.dto.CmdContextDto;
import com.ogms.dge.container.modules.method.processor.ParameterHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * @name: VectorAttributeFielldHandler
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 12/20/2024 11:31 AM
 * @version: 1.0
 */
@Component
public class VectorAttributeFieldHandler implements ParameterHandler {
    @Override
    public boolean supports(Object parameterType) {
        return parameterType instanceof Map && ((Map<?, ?>) parameterType).containsKey("VectorAttributeField");
    }

    @Override
    public void parse(Object parameterType, Object rawValue, Integer valIndex, CmdContextDto context, Boolean isExternalCall) throws IOException {
        handleNull(rawValue, context.getCmdBuilder(), context.getParamSpecs());
    }
}
