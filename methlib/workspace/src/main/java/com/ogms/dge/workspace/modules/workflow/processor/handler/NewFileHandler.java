package com.ogms.dge.workspace.modules.workflow.processor.handler;

import com.ogms.dge.workspace.common.utils.FileUtils;
import com.ogms.dge.workspace.modules.workflow.dto.CmdContextDto;
import com.ogms.dge.workspace.modules.workflow.dto.CmdDto;
import com.ogms.dge.workspace.modules.workflow.processor.ParameterHandler;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * @name: NewFileHandler
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 12/19/2024 4:41 PM
 * @version: 1.0
 */
@Component
public class NewFileHandler implements ParameterHandler {
    @Override
    public boolean supports(Object parameterType) {
        return parameterType instanceof Map && ((Map<?, ?>) parameterType).containsKey("NewFile");
    }

    @Override
    public void parse(Object parameterType, Object rawValue, CmdContextDto context) {
        // 获取 NewFile
        String filePid = ((Map<?, ?>) rawValue).get("filePid").toString();
        String fileFrontName = ((Map<?, ?>) rawValue).get("outputFileName").toString();
        String fileGroup = ((Map<?, ?>) rawValue).get("fileGroup").toString();
        Map<?, ?> parameterTypeMap = (Map<?, ?>) parameterType;
        CmdDto cmdDto = context.getCmdDto();
        cmdDto.outputFileGroup = fileGroup; // TODO
        cmdDto.newFilePidList.add(filePid);
        StringBuilder cmdBuilder = context.getCmdBuilder();
        // cmdDto.newFilePidList.add(Objects.toString(reqParams.get("val" + i))); // 不用（String），因为可能是根目录0
        // 获取输出文件的扩展名
        Object fileTypeObj = parameterTypeMap.get("NewFile");
        String extension = "";
        if (fileTypeObj instanceof Map) {
            // Vector会自动加扩展名
            cmdDto.extensionList.add("");
        } else if (fileTypeObj instanceof String) {
            if (fileTypeObj.equals("Raster") || fileTypeObj.equals("Vector")) {
                extension = "tif";
            } else if (fileTypeObj.equals("Lidar")) {
                extension = "las";
            } else {
                // 普通文件html\csv等
                cmdDto.extensionList.add(Objects.toString(fileTypeObj).toLowerCase());
                extension = Objects.toString(fileTypeObj).toLowerCase();
            }
        }
        // newFilePid = (String) params.get("val" + i);
        // 使用 newFile 进行后续处理
        cmdDto.hasOutput = true;
        String outputFileRealName;
        // outputFileRealName和fileFrontName都有可能是既包含扩展名的、扩展不包含的，对于raster
        // exe自动加上了扩展名，outputFileRealName就没有，对于csv exe不自动加上的，outputFileRealName有扩展名
        if (extension.equals("") || !FileUtils.getFileSuffix(fileFrontName).isEmpty()) {
            extension = FileUtils.getFileSuffix(fileFrontName);
        }
        outputFileRealName = UUID.randomUUID().toString() + "." + extension;
        fileFrontName = fileFrontName.equals("") ? outputFileRealName : fileFrontName;
        cmdDto.outputFileRealNameList.add(outputFileRealName);
        cmdDto.fileFrontNameList.add(fileFrontName);
        cmdBuilder.append(outputFileRealName).append(" ");
        // cmd += outputFilePath + File.separator + outputFileRealName + " ";
    }
}
