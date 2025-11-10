package com.ogms.dge.container.modules.method.processor.handler;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ogms.dge.container.common.utils.FileUtils;
import com.ogms.dge.container.modules.fs.entity.FileInfo;
import com.ogms.dge.container.modules.fs.service.IFileInfoService;
import com.ogms.dge.container.modules.method.dto.CmdContextDto;
import com.ogms.dge.container.modules.method.dto.CmdDto;
import com.ogms.dge.container.modules.method.processor.ParameterHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @name: FileListHandler
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 12/19/2024 4:43 PM
 * @version: 1.0
 */
@Component
public class FileListHandler implements ParameterHandler {
    @Autowired
    private IFileInfoService fileInfoService;

    @Value("${container.data.fd}")
    private String data_fd;

    @Value("${container.data.server}")
    private String data_server;

    @Override
    public boolean supports(Object parameterType) {
        return parameterType instanceof Map && ((Map<?, ?>) parameterType).containsKey("FileList");
    }

    @Override
    public void parse(Object parameterType, Object rawValue, Integer valIndex, CmdContextDto context, Boolean isExternalCall) throws IOException {
        if (isExternalCall)
            parseExternal(parameterType, rawValue, context);
        else
            parseInternal(parameterType, rawValue, context);
    }

    private void parseInternal(Object parameterType, Object rawValue, CmdContextDto context) {
        List<String> inputList = (List<String>) rawValue;
        Map<?, ?> parameterTypeMap = (Map<?, ?>) parameterType;
        CmdDto cmdDto = context.getCmdDto();
        StringBuilder cmdBuilder = context.getCmdBuilder();

        Integer i = 0;
        for (String input : inputList) {
            String value = Objects.toString(input);
            FileInfo existingFile = fileInfoService.getOne(new QueryWrapper<FileInfo>().eq("file_id", value));

            if (context.getServiceBasePath() == null) {
                // TODO inputFilePid
                cmdDto.inputFilePid = existingFile.getFilePid();
                // TODO COPY inputSrcFiles to wdFolder
                cmdDto.inputFileIds.add(value);
            } else {
                existingFile = new FileInfo();
                existingFile.setFilePath(value);
                if (!cmdDto.inputFileIds.contains(context.getServiceUuid())) {
                    // 避免加重
                    cmdDto.inputFileIds.add(context.getServiceUuid());
                }
            }


            Object fileTypeObj = parameterTypeMap.get("FileList");
            // FileList里面可能还有json
            if (fileTypeObj instanceof Map) {
                Map<?, ?> fileTypeMap = (Map<?, ?>) fileTypeObj;
                if (fileTypeMap.containsKey("Vector") || fileTypeMap.containsKey("RasterAndVector")) {
                    // 假设是shp
                    if (existingFile.getFileName().toLowerCase().endsWith(".shp")) {
                        // 输入shp时，查找云盘父目录下原文件名同名的
                        fileInfoService.handleShp(existingFile);
                        handleInput(context.getCmdBuilder(), i, inputList.size(), Paths.get(context.getServiceBasePath() == null ? data_fd : context.getServiceBasePath(), existingFile.getFilePath()).toString());
                        // TODO shp也应该效法Directory，将shp相关文件拷贝至工作目录
                    } else {
                        // RasterAndVector输入的，或者不是shp，直接输入，让exe反馈
                        handleInput(context.getCmdBuilder(), i, inputList.size(), Paths.get(context.getServiceBasePath() == null ? data_fd : context.getServiceBasePath(), existingFile.getFilePath()).toString());
                    }
                }
            } else if (fileTypeObj instanceof String && fileTypeObj.equals("Grid")) {
                if (existingFile.getFileName().toLowerCase().endsWith(".sgrd")) {
                    fileInfoService.handleShp(existingFile);
                    handleInput(context.getCmdBuilder(), i, inputList.size(), Paths.get(context.getServiceBasePath() == null ? data_fd : context.getServiceBasePath(), existingFile.getFilePath()).toString());
                }
            } else {
                // 普通文件
                handleInput(context.getCmdBuilder(), i, inputList.size(), Paths.get(context.getServiceBasePath() == null ? data_fd : context.getServiceBasePath(), existingFile.getFilePath()).toString());
            }
            i++;
        }
    }

    private void parseExternal(Object parameterType, Object rawValue, CmdContextDto context) throws IOException {
        List<String> inputList = (List<String>) rawValue;
        Map<?, ?> parameterTypeMap = (Map<?, ?>) parameterType;
        CmdDto cmdDto = context.getCmdDto();
        StringBuilder cmdBuilder = context.getCmdBuilder();

        Integer i = 0;
        for (String input : inputList) {
            String value = Objects.toString(input);
            File file = fileInfoService.downloadFile(data_server + '/' + value, context.getWdFolder().getAbsolutePath(), false);
            cmdDto.inputSrcFiles.add(file);
            cmdDto.inputFileIds.add(value);

            Object fileTypeObj = parameterTypeMap.get("FileList");
            // FileList里面可能还有json
            if (fileTypeObj instanceof Map) {
                Map<?, ?> fileTypeMap = (Map<?, ?>) fileTypeObj;
                if (fileTypeMap.containsKey("Vector") || fileTypeMap.containsKey("RasterAndVector")) {
                    // 假设是shp
                    handleMultipartFile(file, cmdDto, context, i, inputList.size());
                }
            } else if (fileTypeObj instanceof String && fileTypeObj.equals("Grid")) {
                // 假设是sgrd
                handleMultipartFile(file, cmdDto, context, i, inputList.size());
            } else {
                // 普通文件
                handleInput(context.getCmdBuilder(), i, inputList.size(), Paths.get(context.getWdFolder().getAbsolutePath(), file.getName()).toString());
            }
            i++;
        }
    }
}
