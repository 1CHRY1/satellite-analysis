package com.ogms.dge.workspace.modules.workflow.processor;

import com.ogms.dge.workspace.modules.workflow.dto.CmdContextDto;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/**
 * @name: ParameterHandler
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 12/19/2024 3:26 PM
 * @version: 1.0
 */
public interface ParameterHandler {
    /**
     * 判断是否支持某种类型
     */
    boolean supports(Object parameterType);

    /**
     * 解析参数类型
     */
    void parse(Object parameterType, Object rawValue, CmdContextDto context) throws IOException;

    /**
     * 处理空对象
     */
    default void handleNull(Object rawValue, StringBuilder cmdBuilder, Map<String, Object> paramSpecs) {
        String value = Objects.toString(rawValue);
        if (rawValue == null) {
            if (paramSpecs.get("default_value") == null) {
                // 如果方法配置默认值为null，那就连带删除flag
                if (cmdBuilder.length() > 0) { // TODO?
                    int lastSpaceIndex = cmdBuilder.lastIndexOf(" ");
                    int secondLastSpaceIndex = cmdBuilder.lastIndexOf(" ", lastSpaceIndex - 1);
                    cmdBuilder.delete(secondLastSpaceIndex + 1, lastSpaceIndex + 1);
                }
            } else {
                cmdBuilder.append(paramSpecs.get("default_value")).append(" ");
            }
        } else if (value.equals("true") || value.equals("false") || value.matches("-?\\d+(\\.\\d+)?")) {
            // 原来是Boolean或数字
            cmdBuilder.append(value).append(" ");
        } else {
            // 是纯字符串，加上引号
            cmdBuilder.append("\"").append(value).append("\" ");
        }
    }

    default void handleInput(StringBuilder cmdBuilder, int index, int size, String filePath) {
        if (index == 0 && size > 1) {
            // 多文件时，第一个文件的路径
            cmdBuilder.append("\"").append(filePath).append(",");
        } else if (index == 0 && size == 1) {
            // 单文件时，文件的路径
            cmdBuilder.append("\"").append(filePath).append("\" ");
        } else if (index != size - 1) {
            // 多文件时，中间文件
            cmdBuilder.append(filePath).append(",");
        } else {
            // 多文件时，最后一个文件
            cmdBuilder.append(filePath).append("\" ");
        }
    }
}
