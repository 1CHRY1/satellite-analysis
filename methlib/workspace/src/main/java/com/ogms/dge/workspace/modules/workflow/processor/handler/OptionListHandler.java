package com.ogms.dge.workspace.modules.workflow.processor.handler;

/**
 * @name: OptionListHandler
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 12/19/2024 4:10 PM
 * @version: 1.0
 */
import com.ogms.dge.workspace.modules.workflow.dto.CmdContextDto;
import com.ogms.dge.workspace.modules.workflow.dto.CmdDto;
import com.ogms.dge.workspace.modules.workflow.processor.ParameterHandler;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class OptionListHandler implements ParameterHandler {

    @Override
    public boolean supports(Object parameterType) {
        return parameterType instanceof Map && ((Map<?, ?>) parameterType).containsKey("OptionList");
    }

    @Override
    public void parse(Object parameterType, Object rawValue, CmdContextDto context) {
        Map<?, ?> parameterTypeMap = (Map<?, ?>) parameterType;
        List<?> options = (List<?>) parameterTypeMap.get("OptionList");
        StringBuilder cmdBuilder = context.getCmdBuilder();

        if (rawValue != null) {
            if (!options.contains(rawValue.toString())) {
                throw new IllegalArgumentException("Value " + rawValue + " is not in allowed options: " + options);
            }
        }

        if (parameterTypeMap.get("OptionList") instanceof List) {
            List<String> optionList = (List<String>) parameterTypeMap.get("OptionList");
            handleNull(rawValue, cmdBuilder, context.getParamSpecs());
        } else {
            // TODO OptionList里面不是option?
        }
    }
}
