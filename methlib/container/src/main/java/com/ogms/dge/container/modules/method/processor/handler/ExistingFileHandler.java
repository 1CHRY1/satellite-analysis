package com.ogms.dge.container.modules.method.processor.handler;

/**
 * @name: ExistingFileHandler
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 12/19/2024 4:06 PM
 * @version: 1.0
 */

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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class ExistingFileHandler implements ParameterHandler {
    @Autowired
    private IFileInfoService fileInfoService;

    @Value("${container.data.fd}")
    private String data_fd;

    @Value("${container.data.server}")
    private String data_server;

    @Override
    public boolean supports(Object parameterType) {
        return parameterType instanceof Map && ((Map<?, ?>) parameterType).containsKey("ExistingFile");
    }

    private void parseInternal(Object parameterType, Object rawValue, CmdContextDto context) {
        String value = Objects.toString(rawValue);
        Map<?, ?> parameterTypeMap = (Map<?, ?>) parameterType;
        CmdDto cmdDto = context.getCmdDto();
        StringBuilder cmdBuilder = context.getCmdBuilder();
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

        Object fileTypeObj = parameterTypeMap.get("ExistingFile");
        // ExistingFile里面可能还有json
        if (fileTypeObj instanceof Map) {
            Map<?, ?> fileTypeMap = (Map<?, ?>) fileTypeObj;
            if (fileTypeMap.containsKey("Vector") || fileTypeMap.containsKey("RasterAndVector")) {
                // 假设是shp
                if (existingFile.getFileName().toLowerCase().endsWith(".shp")) {
                    // 输入shp时，查找云盘父目录下原文件名同名的
                    fileInfoService.handleShp(existingFile);
                    handleInput(context.getCmdBuilder(), 0, 1, Paths.get(context.getServiceBasePath() == null ? data_fd : context.getServiceBasePath(), existingFile.getFilePath()).toString());
                    // TODO shp也应该效法Directory，将shp相关文件拷贝至工作目录
                } else {
                    // RasterAndVector输入的，或者不是shp，直接输入，让exe反馈
                    handleInput(context.getCmdBuilder(), 0, 1, Paths.get(context.getServiceBasePath() == null ? data_fd : context.getServiceBasePath(), existingFile.getFilePath()).toString());
                }
            }
        } else if (fileTypeObj instanceof String && fileTypeObj.equals("Grid")) {
            if (existingFile.getFileName().toLowerCase().endsWith(".sgrd")) {
                fileInfoService.handleShp(existingFile);
                handleInput(context.getCmdBuilder(), 0, 1, Paths.get(context.getServiceBasePath() == null ? data_fd : context.getServiceBasePath(), existingFile.getFilePath()).toString());
            }
        } else {
            // 普通文件
            handleInput(context.getCmdBuilder(), 0, 1, Paths.get(context.getServiceBasePath() == null ? data_fd : context.getServiceBasePath(), existingFile.getFilePath()).toString());
        }
    }

    private void parseExternal(Object parameterType, Object rawValue, CmdContextDto context) throws IOException {
        String value;
        // TODO 比parseInternal多，兼容单元素数组
        if (rawValue instanceof ArrayList) {
            if (((ArrayList<?>) rawValue).size() == 0) {
                value = "";
            }
            value = ((ArrayList<?>) rawValue).get(0).toString();
        } else value = Objects.toString(rawValue);
        Map<?, ?> parameterTypeMap = (Map<?, ?>) parameterType;
        CmdDto cmdDto = context.getCmdDto();
        StringBuilder cmdBuilder = context.getCmdBuilder();
        // 下载到临时目录
        File file = fileInfoService.downloadFile(data_server + '/' + value,
                context.getWdFolder().getAbsolutePath(), false);
        cmdDto.inputSrcFiles.add(file);
        cmdDto.inputFileIds.add(value);

        Object fileTypeObj = parameterTypeMap.get("ExistingFile");
        // ExistingFile里面可能还有json
        if (fileTypeObj instanceof Map) {
            Map<?, ?> fileTypeMap = (Map<?, ?>) fileTypeObj;
            if (fileTypeMap.containsKey("Vector") || fileTypeMap.containsKey("RasterAndVector")) {
                // 假设是shp
                handleMultipartFile(file, cmdDto, context, 0, 1);
            }
        } else if (fileTypeObj instanceof String && fileTypeObj.equals("Grid")) {
            // 假设是sgrd
            handleMultipartFile(file, cmdDto, context, 0, 1);
        } else {
            // 普通文件
            handleInput(context.getCmdBuilder(), 0, 1, Paths.get(context.getWdFolder().getAbsolutePath(), file.getName()).toString());
        }
    }

    @Override
    public void parse(Object parameterType, Object rawValue, Integer valIndex, CmdContextDto context, Boolean isExternalCall) throws IOException {
        if (isExternalCall)
            parseExternal(parameterType, rawValue, context);
        else
            parseInternal(parameterType, rawValue, context);
    }
}
