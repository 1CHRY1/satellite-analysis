package com.ogms.dge.container.modules.method.processor;

import com.ogms.dge.container.common.utils.FileUtils;
import com.ogms.dge.container.modules.method.dto.CmdContextDto;
import com.ogms.dge.container.modules.method.dto.CmdDto;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
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
    void parse(Object parameterType, Object rawValue, Integer valIndex, CmdContextDto context, Boolean isExternalCall) throws IOException;

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
            cmdBuilder.append("\"").append(filePath).append(";");
        } else if (index == 0 && size == 1) {
            // 单文件时，文件的路径
            cmdBuilder.append("\"").append(filePath).append("\" ");
        } else if (index != size - 1) {
            // 多文件时，中间文件
            cmdBuilder.append(filePath).append(";");
        } else {
            // 多文件时，最后一个文件
            cmdBuilder.append(filePath).append("\" ");
        }
    }

    // External Invocation
    default void handleMultipartFile(File file, CmdDto cmdDto, CmdContextDto context, Integer index, Integer size) throws IOException {
        if (file.getName().toLowerCase().endsWith(".zip")) {
            // 解压缩
            List<File> unzippedFiles = FileUtils.unzip(file.getAbsolutePath(), file.getParentFile());
            cmdDto.inputSrcFiles.addAll(unzippedFiles);
            File rootFile = null;
            for (File f : unzippedFiles) {
                if (f.getName().toLowerCase().endsWith(".shp")) {
                    rootFile = f;
                    break;
                }
            }
            if (rootFile == null) {
                // 直接输入zip,原来是仅file.getName()和rootFile.getName()
                handleInput(context.getCmdBuilder(), index, size, Paths.get(context.getWdFolder().getAbsolutePath(), file.getName()).toString());
            } else {
                handleInput(context.getCmdBuilder(), index, size, Paths.get(context.getWdFolder().getAbsolutePath(), rootFile.getName()).toString());
            }
        } else {
            // 不是shp，直接输入，让exe反馈
            handleInput(context.getCmdBuilder(), index, size, Paths.get(context.getWdFolder().getAbsolutePath(), file.getName()).toString());
        }
    }
}
