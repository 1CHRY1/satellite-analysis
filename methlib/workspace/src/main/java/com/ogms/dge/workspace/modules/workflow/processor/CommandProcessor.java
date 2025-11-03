package com.ogms.dge.workspace.modules.workflow.processor;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ogms.dge.workspace.modules.project.entity.MethodInsEntity;
import com.ogms.dge.workspace.modules.project.service.MethodInsService;
import com.ogms.dge.workspace.modules.workflow.dto.CmdContextDto;
import com.ogms.dge.workspace.modules.workflow.dto.CmdDto;
import com.ogms.dge.workspace.modules.workflow.dto.MethodDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * @name: CommandProcessor
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 12/19/2024 3:50 PM
 * @version: 1.0
 */
@Component
public class CommandProcessor {
    @Value("${workspace.method.wd}")
    private String method_wd;

    @Value("${workspace.data.fd}")
    private String data_fd;

    @Value("${workspace.data.insd}")
    private String data_insd;

    @Value("${workspace.data.td}")
    private String data_td;

    @Value("${workspace.data.server}")
    private String data_server;

    @Value("${workspace.method.insd}")
    private String method_insd;

    @Value("${workspace.env.py}")
    private String env_py;

    @Autowired
    private MethodInsService methodInsService;

    @Autowired
    private ParameterHandlerFactory parameterHandlerFactory;

    private ObjectMapper objectMapper = new ObjectMapper();

    public CmdDto getCmd(MethodDto method, String serviceBasePath, String serviceUuid, Map<String, Object> reqParams, Date startTime) throws JsonProcessingException {
        List<Map<String, Object>> paramSpecsList = objectMapper.readValue(method.getParams(), new TypeReference<List<Map<String, Object>>>() {
        });
        StringBuilder cmdBuilder = new StringBuilder();

        File wdFolder = new File(method_wd + UUID.randomUUID().toString());
        wdFolder.mkdirs(); // 如果目录已存在，不会重复创建

        if (method.getId() > 478L) {
            // 非Whitebox方法
            cmdBuilder.append("cd /d \"").append(wdFolder.getAbsolutePath()).append("\" && ");
        }
        MethodInsEntity methodIns = methodInsService.getOne(new QueryWrapper<MethodInsEntity>().eq("method_id", method.getId()));
        cmdBuilder.append(getToolCmd(method, methodIns.getUuid())).append(" ");
        // 针对 Whitebox，添加方法名和临时工作目录
        if (method.getId() < 479L) {
            cmdBuilder.append("-r=").append(method.getName()).append(" ");
            cmdBuilder.append("--wd=").append(wdFolder.getAbsolutePath()).append(" ");
        }

        // 初始化多个变量，方便后期记录，命令执行后处理
        CmdDto cmdDto = new CmdDto();
        CmdContextDto cmdContextDto = new CmdContextDto();
        boolean isSuccess = true;
        try {
            // 以下开始参数处理
            for (int i = 0; i < paramSpecsList.size(); i++) {
                if (!reqParams.containsKey("val" + i)) {
                    // 用户没有操作此项参数
                    continue;
                }
                Map<String, Object> paramSpecs = paramSpecsList.get(i);
                // 先获取参数输入标识flag,获取 Flags 数组的第一个元素
                processFlags(cmdBuilder, paramSpecs);

                // 区分input类型
                if (reqParams.get("val" + i) != null && reqParams.get("val" + i) instanceof Map) {
                    Map<String, Object> input = (Map<String, Object>) reqParams.get("val" + i);
                    if (input.get("type").equals("dataIns")) {
                        cmdContextDto = new CmdContextDto(cmdBuilder, paramSpecs, wdFolder, cmdDto,
                                (String) input.get("fileId"), data_insd);
                    } else if (input.get("type").equals("profile")) {
                        cmdContextDto = new CmdContextDto(cmdBuilder, paramSpecs, wdFolder, cmdDto, null, null);
                    } else {
                        cmdContextDto = new CmdContextDto(cmdBuilder, paramSpecs, wdFolder, cmdDto, null, null);
                    }
                } else {
                    cmdContextDto = new CmdContextDto(cmdBuilder, paramSpecs, wdFolder, cmdDto, null, null);
                }

                // 获取 parameter_type 属性并分类处理
                Object parameterTypeObj = paramSpecs.get("parameter_type");
                ParameterHandler handler = parameterHandlerFactory.getHandler(parameterTypeObj);
                handler.parse(parameterTypeObj, reqParams.get("val" + i), cmdContextDto);
            }
        } catch (Exception e) {
            isSuccess = false;
            // addMethodLog(method.getId(), userId, wdFolder.getName(), null,serviceUuid==null?0:1, null, 1, "Parameters Error", startTime);
        }
        if (!isSuccess) {
            return null;
        }
        cmdDto = cmdContextDto.getCmdDto();
        cmdDto.cmd = cmdContextDto.getCmdBuilder().toString();
        // cmdDto.fileFrontNameList = cmdDto.getOutputFileRealNameList();
        cmdDto.outputFilePath = Paths.get(data_fd, Objects.toString(1L)).toString(); // TODO userId
        cmdDto.tmpFilePath = wdFolder.getAbsolutePath();
        return cmdDto;
    }

    private void processFlags(StringBuilder cmdBuilder, Map<String, Object> paramSpecs) {
        Object flagsObj = paramSpecs.get("Flags");
        if (flagsObj instanceof List<?>) {
            List<?> flagsList = (List<?>) flagsObj;
            if (!flagsList.isEmpty()) {
                cmdBuilder.append(flagsList.get(0)).append(" ");
            } else {
                cmdBuilder.append(" ");
            }
        }
    }

    private String getToolCmd(MethodDto method, String insUuid) {
        // 获取exe地址，TODO 需要确保环境路径无空格
        String path = "";
        if (method.getId() < 479L) {
            Path directoryPath = Paths.get(method_insd + insUuid);
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(directoryPath, "*.exe")) {
                for (Path entry : stream) {
                    path = entry.toAbsolutePath().toString();
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (method.getExecution().equals("py")) {
            Path directoryPath = Paths.get(method_insd + insUuid);
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(directoryPath, "*.py")) {
                for (Path entry : stream) {
                    path = Paths.get(env_py + "python.exe").toAbsolutePath() + " " + entry.toAbsolutePath().toString() + " "; // python test.py
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (method.getExecution().equals("exe")) {
            Path directoryPath = Paths.get(method_insd + insUuid);
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(directoryPath, "*.exe")) {
                for (Path entry : stream) {
                    path = entry.toAbsolutePath().toString() + " ";
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (method.getExecution().equals("jar")) {
            Path directoryPath = Paths.get(method_insd + insUuid);
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(directoryPath, "*.jar")) {
                for (Path entry : stream) {
                    path = "java -jar " + entry.toAbsolutePath().toString() + " ";
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return path;
    }
}
