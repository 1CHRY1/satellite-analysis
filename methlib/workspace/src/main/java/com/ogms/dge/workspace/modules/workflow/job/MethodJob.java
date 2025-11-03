package com.ogms.dge.workspace.modules.workflow.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ogms.dge.workspace.common.utils.FileUtils;
import com.ogms.dge.workspace.common.utils.HttpUtils;
import com.ogms.dge.workspace.modules.fs.service.IFileInfoService;
import com.ogms.dge.workspace.modules.workflow.converter.MethodConverter;
import com.ogms.dge.workspace.modules.workflow.dto.CmdDto;
import com.ogms.dge.workspace.modules.workflow.dto.MethodDto;
import com.ogms.dge.workspace.modules.workflow.entity.TaskEntity;
import com.ogms.dge.workspace.modules.workflow.enums.TaskStatusEnum;
import com.ogms.dge.workspace.modules.workflow.processor.CommandProcessor;
import com.ogms.dge.workspace.modules.workflow.service.WorkflowExecutionService;
import com.ogms.dge.workspace.modules.workflow.service.impl.TaskServiceImpl;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @name: MethodJob
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 12/18/2024 2:34 PM
 * @version: 1.0
 */
public class MethodJob implements Job {
    private final TaskServiceImpl taskService;
    private final WorkflowExecutionService workflowService;
    private final HttpUtils httpUtils;
    private final MethodConverter methodConverter;
    private IFileInfoService fileInfoService;
    private final CommandProcessor commandProcessor;

    public MethodJob(TaskServiceImpl taskService, WorkflowExecutionService workflowService, HttpUtils httpUtils, MethodConverter methodConverter, IFileInfoService fileInfoService, CommandProcessor commandProcessor) {
        this.taskService = taskService;
        this.workflowService = workflowService;
        this.httpUtils = httpUtils;
        this.methodConverter = methodConverter;
        this.fileInfoService = fileInfoService;
        this.commandProcessor = commandProcessor;
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${container.api}")
    private String container_api;

    @Override
    public void execute(JobExecutionContext context) {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        String jobKey = context.getJobDetail().getKey().toString();

        String taskUuid = dataMap.getString("uuid");

        TaskEntity task = taskService.getOne(new QueryWrapper<TaskEntity>().eq("job_key", jobKey));
        task.setStartTime(new Date());
        task.setStatus(TaskStatusEnum.IN_PROGRESS.getCode());
        taskService.saveOrUpdate(task);

        try {
            // 执行任务逻辑
            executeTaskLogic(task, dataMap);
            // TODO 异常处理
            // 更新任务状态
            task.setEndTime(new Date());
            task.setStatus(TaskStatusEnum.COMPLETED.getCode());
            taskService.saveOrUpdate(task);

            // 通知 WorkflowExecutionService
            workflowService.onTaskCompleted(taskUuid, task.getWorkflowUuid());

        } catch (Exception e) {
            task.setStatus(TaskStatusEnum.FAILED.getCode());
            taskService.saveOrUpdate(task);
        }
    }

    private void executeTaskLogic(TaskEntity task, JobDataMap dataMap) throws JsonProcessingException {
        Date startTime = new Date();
        MethodDto method = getMethodDto(dataMap.getString("methodId"));
        // TODO Figure serviceUuid out(原来是只有一个service输入（映射）)
        Map<String, Object> reqParams = objectMapper.readValue(dataMap.getString("inputParams"), Map.class);
        CmdDto cmdDto = commandProcessor.getCmd(method, null, null, reqParams, startTime);
        if (cmdDto == null) {
            // addMethodLog(method.getId(), userId, null, null, serviceUuid==null?0:1, null, 1, "Missing necessary parameters.", startTime);
            // return R.error(1, "Missing necessary parameters.");
        }
        ExecutorService executorService = Executors.newFixedThreadPool(10); // 创建一个线程池，处理并发操作
        Future future = executorService.submit(() -> {
            // 方法执行信息
            String info = "";
            List<String> fileIdList = new ArrayList<>();
            try {
                // 启动进程
                ProcessBuilder builder = new ProcessBuilder("cmd", "/c", cmdDto.getCmd());
                Process process = builder.start();
                // Process process = Runtime.getRuntime().exec(cmdDto.getCmd());

                // 创建读取标准输出流的线程
                BufferedReader stdOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder stdInfo = new StringBuilder();
                Thread stdThread = new Thread(() -> {
                    String line;
                    try {
                        while ((line = stdOutput.readLine()) != null) {
                            stdInfo.append(line).append("\n");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

                // 创建读取标准错误流的线程
                BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                StringBuilder errInfo = new StringBuilder();
                Thread errThread = new Thread(() -> {
                    String line;
                    try {
                        while ((line = stdError.readLine()) != null) {
                            errInfo.append(line).append("\n");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

                // 启动读取线程
                stdThread.start();
                errThread.start();

                // 等待进程结束
                int exitCode = process.waitFor();

                // 等待输出读取线程结束
                stdThread.join();
                errThread.join();

                // 将标准输出和错误流的信息合并
                info = stdInfo.toString() + errInfo.toString();

                // 程序执行完后，工作目录的inputSrcFiles删掉，否则影响工作目录内结果的拷贝（因为到时候一股脑把他们全部拷到用户数据文件夹了）
                FileUtils.deleteFilesFromDirectory(cmdDto.getInputSrcFiles());

                if (exitCode != 0) {
                    System.out.println("发生异常");
                    info += "\r\n Process finished with errors.";
                    /*addMethodLog(method.getId(), userId, new File(cmdDto.getTmpFilePath()).getName(),
                            cmdDto.getInputFileIds(),serviceUuid==null?0:1, null, 1, info, startTime);*/
                    // return R.error(1, info);
                } else {
                    // SaveOutputFile
                    if (cmdDto.getHasOutput()) {
                        // 保存记录至数据库
                        // outputFileRealNameList、fileFrontNameList、newFilePidList长度是一致的，与输出文件的个数相同
                        for (int i = 0; i < cmdDto.getOutputFileRealNameList().size(); i++) {
                            File targetFolder = new File(cmdDto.getOutputFilePath());
                            if (!targetFolder.exists()) {
                                targetFolder.mkdirs();
                            }
                            // 单输出源 TODO 一个输出源应该对应一个fileGroup
                            List<String> fileIds = fileInfoService.handleSaveToDb(1L, cmdDto.getTmpFilePath(),
                                    cmdDto.getOutputFilePath(), cmdDto.getFileFrontNameList().get(i),
                                    cmdDto.getOutputFileRealNameList().get(i), cmdDto.getNewFilePidList().get(i), cmdDto.getOutputFileGroup()); // TODO userId
                            fileIdList.addAll(fileIds);
                        }
                    }
                }

            } catch (IOException | InterruptedException e) {
                // TODO: 异常统一处理
                System.out.println(("Error executing command: " + e));

                info += "Error executing command: " + e.getMessage();

                e.printStackTrace();
                /*addMethodLog(method.getId(), userId, new File(cmdDto.getTmpFilePath()).getName(),
                        cmdDto.getInputFileIds(),serviceUuid==null?0:1, fileIdList, 1, info, startTime);*/
                // return R.error(1, info);
            }
            Map<String, Object> result = new HashMap<>();
            result.put("info", info);
            result.put("output", fileIdList);
            result.put("code", 0);
            return result;
        });
        try {
            // 获取异步任务的结果，这里会阻塞直到任务完成
            Map<String, Object> result = (Map<String, Object>) future.get();
            if ((int) result.get("code") != 0) {
                // return result;
            } else {
                /*addMethodLog(method.getId(), userId, new File(cmdDto.getTmpFilePath()).getName(),
                        cmdDto.getInputFileIds(),serviceUuid==null?0:1, (List<String>) result.get("output"), 0, (String) result.get("info"),
                        startTime);
                return R.ok(result); // 确保任务完成后再返回 info 的值*/
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            /*addMethodLog(method.getId(), userId, new File(cmdDto.getTmpFilePath()).getName(), cmdDto.getInputFileIds(),serviceUuid==null?0:1,
                    null, 1, "Failed to execute async task: " + e.getMessage(), startTime);*/
            // return R.error(1, "Failed to execute async task: " + e.getMessage());
        }
    }

    private MethodDto getMethodDto(String methodId) {
        ResponseEntity<Map> response = httpUtils.get(container_api + "method/info/" + methodId);

        if (response.getStatusCode().is2xxSuccessful()) {
            Map<String, Object> method = (Map<String, Object>) response.getBody().get("method");
            return methodConverter.map2Dto(method);
        }
        return null;
    }
}
