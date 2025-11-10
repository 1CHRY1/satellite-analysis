package com.ogms.dge.container.modules.method.processor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @name: ParameterHandlerFactory
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 12/19/2024 3:45 PM
 * @version: 1.0
 */
@Component
public class ParameterHandlerFactory {
    @Lazy
    @Autowired
    private List<ParameterHandler> handlers; // 自动注入所有的处理器

    public ParameterHandler getHandler(Object parameterType) {
        for (ParameterHandler handler : handlers) {
            if (handler.supports(parameterType)) {
                return handler;
            }
        }
        throw new IllegalArgumentException("No handler found for type: " + parameterType);
    }
}
