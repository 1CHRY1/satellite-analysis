package com.ogms.dge.workspace.modules.workflow.processor.handler;

/**
 * @name: ExistingFileHandler
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 12/19/2024 4:06 PM
 * @version: 1.0
 */

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ogms.dge.workspace.common.utils.FileUtils;
import com.ogms.dge.workspace.modules.fs.entity.FileInfo;
import com.ogms.dge.workspace.modules.fs.service.IFileInfoService;
import com.ogms.dge.workspace.modules.workflow.dto.CmdContextDto;
import com.ogms.dge.workspace.modules.workflow.dto.CmdDto;
import com.ogms.dge.workspace.modules.workflow.processor.ParameterHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class ExistingFileHandler implements ParameterHandler {
    @Autowired
    private IFileInfoService fileInfoService;

    @Value("${workspace.data.fd}")
    private String data_fd;

    @Override
    public boolean supports(Object parameterType) {
        return parameterType instanceof Map && ((Map<?, ?>) parameterType).containsKey("ExistingFile");
    }

    @Override
    public void parse(Object parameterType, Object rawValue, CmdContextDto context) {
        Map<String, Object> input = (Map<String, Object>) rawValue;
        String value = Objects.toString(input.get("fileId"));
        Map<?, ?> parameterTypeMap = (Map<?, ?>) parameterType;
        CmdDto cmdDto = context.getCmdDto();
        StringBuilder cmdBuilder = context.getCmdBuilder();
        FileInfo existingFile = fileInfoService.getOne(new QueryWrapper<FileInfo>().eq("file_id", value));


        if (context.getDataInsBasePath() == null && !input.containsKey("filePid")) {
            // profile数据输入
            // TODO inputFilePid
            cmdDto.inputFilePid = existingFile.getFilePid();
            // TODO COPY inputSrcFiles to wdFolder
            cmdDto.inputFileIds.add(value);
        } else {
            existingFile = new FileInfo();
            if (input.containsKey("filePid")) {
                // 说明是上一个方法的输出
                String fileGroup = input.get("fileGroup").toString();
                List<FileInfo> inputFiles = fileInfoService.getFilesByGroup(1L, fileGroup); // TODO
                existingFile = inputFiles.get(0);
                cmdDto.inputFilePid = existingFile.getFilePid();
                cmdDto.inputFileIds.add(existingFile.getFileId());
            } else {
                // 数据实例输入
                List<File> inputFiles = FileUtils.getAllFilesFromDirectory(Paths.get(context.getDataInsBasePath(), value).toAbsolutePath().toString());
                // dataIns的uuid下包含了输入的文件列表，对于ExitingFile通常只有一个文件
                existingFile.setFilePath(Paths.get(value, inputFiles.get(0).getName()).toString());
                if (!cmdDto.inputFileIds.contains(context.getDataInsUuid())) {
                    // 避免加重
                    cmdDto.inputFileIds.add(context.getDataInsUuid());
                }
            }
        }
        Object fileTypeObj = parameterTypeMap.get("ExistingFile");
        // ExistingFile里面可能还有json
        if (fileTypeObj instanceof Map) {
            Map<?, ?> fileTypeMap = (Map<?, ?>) fileTypeObj;
            if (fileTypeMap.containsKey("Vector") || fileTypeMap.containsKey("RasterAndVector")) {
                // 假设是shp
                if (existingFile.getFileName().toLowerCase().endsWith(".shp") && context.getDataInsBasePath() == null) { // TODO dataIns
                    // 输入shp时，查找云盘父目录下原文件名同名的
                    fileInfoService.handleShp(existingFile);
                    handleInput(context.getCmdBuilder(), 0, 1, Paths.get(context.getDataInsBasePath() == null ? data_fd : context.getDataInsBasePath(), existingFile.getFilePath()).toString());
                    // TODO shp也应该效法Directory，将shp相关文件拷贝至工作目录
                } else {
                    // RasterAndVector输入的，或者不是shp，直接输入，让exe反馈
                    handleInput(context.getCmdBuilder(), 0, 1, Paths.get(context.getDataInsBasePath() == null ? data_fd : context.getDataInsBasePath(), existingFile.getFilePath()).toString());
                }
            }
        } else {
            // 普通文件
            handleInput(context.getCmdBuilder(), 0, 1, Paths.get(context.getDataInsBasePath() == null ? data_fd : context.getDataInsBasePath(), existingFile.getFilePath()).toString());
        }
    }
}
