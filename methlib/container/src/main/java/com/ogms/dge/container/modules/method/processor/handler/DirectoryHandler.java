package com.ogms.dge.container.modules.method.processor.handler;

import com.ogms.dge.container.common.utils.FileUtils;
import com.ogms.dge.container.modules.fs.service.IFileInfoService;
import com.ogms.dge.container.modules.method.dto.CmdContextDto;
import com.ogms.dge.container.modules.method.dto.CmdDto;
import com.ogms.dge.container.modules.method.processor.ParameterHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * @name: DirectoryHandler
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 12/19/2024 11:05 PM
 * @version: 1.0
 */
@Component
public class DirectoryHandler implements ParameterHandler {

    @Autowired
    private IFileInfoService fileInfoService;

    @Override
    public boolean supports(Object parameterType) {
        if (parameterType instanceof String) {
            String type = (String) parameterType;
            return type.equals("Directory");
        }
        return false;
    }

    @Override
    public void parse(Object parameterType, Object rawValue, Integer valIndex, CmdContextDto context, Boolean isExternalCall) throws IOException {
        StringBuilder cmdBuilder = context.getCmdBuilder();
        CmdDto cmdDto = context.getCmdDto();
        // 判断是输入还是输出Directory?
        if ((context.getParamSpecs().get("Type")).equals("DataInput")) {
            // 设置输入路径为工作路径，具体是拷贝用户选择的源文件夹内的所有文件到工作路径
            // 在程序执行完后、拷贝最终结果至用户数据文件夹之前把这些文件删除
            Map<?, ?> fileParent = (Map<?, ?>) rawValue;
            String filePid = Objects.toString(fileParent.get("filePid"));
            List<File> srcFiles = fileInfoService.getFilesByPid(context.getUserId(), filePid); // TODO userId
            cmdDto.inputSrcFiles = FileUtils.copyFilesToDirectory(srcFiles, context.getWdFolder().getAbsolutePath());
            cmdDto.inputFileIds.add(filePid); // TODO 获取所有文件的id，而不是pid
            cmdBuilder.append("\"").append(context.getWdFolder().getAbsolutePath()).append("\" ");
        } else if ((context.getParamSpecs().get("Type")).equals("DataOutput")) {
            // DirectoryOutputFlagInfo
            // 获取 String
            cmdDto.hasOutput = true;
            cmdDto.outputFileGroup = UUID.randomUUID().toString(); // TODO
            String directoryValue;
            if (rawValue == null) {
                // 默认保存与输入路径相同
                directoryValue = cmdDto.inputFilePid; // inputFilePid
            } else {
                Map<?, ?> fileParent = (Map<?, ?>) rawValue;
                directoryValue = Objects.toString(fileParent.get("filePid"));
            }
            // 凡是输出，就要做类似以下的操作
            String outputFileRealName = "DirectoryOutputFlagInfo"; // 对于Directory,
            // outputFileRealName不起作用
            cmdDto.outputFileRealNameList.add(outputFileRealName);
            cmdDto.fileFrontNameList.add(outputFileRealName);
            cmdDto.newFilePidList.add(directoryValue);
            cmdDto.extensionList.add("");
            // 此处要写绝对路径，exe要求指明路径
            cmdBuilder.append("\"").append(context.getWdFolder().getAbsolutePath()).append("\" ");
        }
    }
}
