package com.ogms.dge.workspace.modules.workflow.processor.handler;

/**
 * @name: ExistingFileOrFloatHandler
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 12/19/2024 4:20 PM
 * @version: 1.0
 */
import com.ogms.dge.workspace.modules.fs.service.IFileInfoService;
import com.ogms.dge.workspace.modules.workflow.dto.CmdContextDto;
import com.ogms.dge.workspace.modules.workflow.processor.ParameterHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

@Component
public class ExistingFileOrFloatHandler implements ParameterHandler {

    @Autowired
    private IFileInfoService fileInfoService;
    @Autowired
    private ExistingFileHandler existingFileHandler;
    @Autowired
    private PrimitiveTypeHandler primitiveTypeHandler;

    @Override
    public boolean supports(Object parameterType) {
        return parameterType instanceof Map && ((Map<?, ?>) parameterType).containsKey("ExistingFileOrFloat");
    }

    @Override
    public void parse(Object parameterType, Object rawValue, CmdContextDto context) {
        String value = Objects.toString(rawValue);
        boolean isNumeric = value.matches("-?\\d+(\\.\\d+)?");
        if (isNumeric) {
            primitiveTypeHandler.parse("Float", rawValue, context);
        } else {
            // 如果转换失败，假定该值为文件路径，处理为 ExistingFile
            if (parameterType instanceof Map) {
                Map<String, Object> typeMap = (Map<String, Object>) parameterType;
                // 将 ExistingFileOrFloat 替换为 ExistingFile
                typeMap.put("ExistingFile", typeMap.get("ExistingFileOrFloat"));
                typeMap.remove("ExistingFileOrFloat");  // 移除 ExistingFileOrFloat
            }
            // 调用 ExistingFileHandler 处理
            existingFileHandler.parse(parameterType, rawValue, context);
        }
    }
}

