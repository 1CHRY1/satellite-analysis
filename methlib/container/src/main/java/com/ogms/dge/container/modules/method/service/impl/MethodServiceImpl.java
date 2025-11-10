package com.ogms.dge.container.modules.method.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.networknt.schema.ValidationMessage;
import com.ogms.dge.container.common.exception.BusinessException;
import com.ogms.dge.container.common.utils.*;
import com.ogms.dge.container.modules.common.entity.UploadRecordEntity;
import com.ogms.dge.container.modules.common.enums.UploadRecordStatusEnum;
import com.ogms.dge.container.modules.common.enums.UploadRecordTypeEnum;
import com.ogms.dge.container.modules.common.service.UploadRecordService;
import com.ogms.dge.container.modules.data.service.ServiceService;
import com.ogms.dge.container.modules.fs.entity.FileInfo;
import com.ogms.dge.container.modules.fs.entity.dto.UploadResultDto;
import com.ogms.dge.container.modules.fs.entity.enums.FileTypeEnums;
import com.ogms.dge.container.modules.fs.entity.enums.UploadStatusEnums;
import com.ogms.dge.container.modules.fs.service.IFileInfoService;
import com.ogms.dge.container.modules.fs.service.impl.FileInfoServiceImpl;
import com.ogms.dge.container.modules.method.converter.MethodConverter;
import com.ogms.dge.container.modules.method.dao.MethodDao;
import com.ogms.dge.container.modules.method.dto.CmdDto;
import com.ogms.dge.container.modules.method.dto.CmdOutputDto;
import com.ogms.dge.container.modules.method.entity.MethodEntity;
import com.ogms.dge.container.modules.method.entity.MethodLogEntity;
import com.ogms.dge.container.modules.method.entity.TagEntity;
import com.ogms.dge.container.modules.method.processor.CommandProcessor;
import com.ogms.dge.container.modules.method.service.MethodLogService;
import com.ogms.dge.container.modules.method.service.MethodService;
import com.ogms.dge.container.modules.method.service.MethodTagService;
import com.ogms.dge.container.modules.method.service.TagService;
import com.ogms.dge.container.modules.method.vo.MethodVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static com.ogms.dge.container.common.utils.FileUtils.getFileNameWithoutSuffix;

@Service("methodService")
public class MethodServiceImpl extends ServiceImpl<MethodDao, MethodEntity> implements MethodService {

    @Autowired
    private MethodTagService methodTagService;

    @Autowired
    private TagService tagService;

    @Autowired
    private MethodLogService methodLogService;

    @Autowired
    private IFileInfoService fileInfoService;

    @Autowired
    private UploadRecordService uploadRecordService;

    @Autowired
    private ServiceService serviceService;

    @Value("${container.method.wd}")
    private String method_wd;

    @Value("${container.data.fd}")
    private String data_fd;

    @Value("${container.data.td}")
    private String data_td;

    @Value("${container.data.server}")
    private String data_server;

    @Value("${container.method.pd}")
    private String method_pd;

    @Value("${container.env.py}")
    private String env_py;

    private ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private MethodConverter methodConverter;

    @Autowired
    private CommandProcessor commandProcessor;

    private final Map<String, List<CmdOutputDto>> outputStore = new ConcurrentHashMap<>();

    @Override
    public PageUtils queryPage(Map<String, Object> params) throws JsonProcessingException {
        String methodName = (String) params.get("key");
        String methodType = (String) params.get("type");
        Long createUserId = (Long) params.get("createUserId");

        // 处理 createUserId 和 Constant.SUPER_ADMIN 的逻辑, 默认同时包含用户自己和超级管理员默认上传的（即whitebox）
        List<Long> userIds = new ArrayList<>();
        if (createUserId != null) {
            userIds.add(createUserId);
        }
        userIds.add((long) Constant.SUPER_ADMIN); // 始终添加 SUPER_ADMIN

        IPage<MethodEntity> page = this.page(
                new Query<MethodEntity>().getPage(params),
                new QueryWrapper<MethodEntity>()
                        .like(StringUtils.isNotBlank(methodName), "name", methodName)
                        .eq(StringUtils.isNotBlank(methodType), "type", methodType)
                        .in(!userIds.isEmpty(), "create_user_id", userIds));
        PageUtils pageUtils = new PageUtils(page);
        pageUtils.setList(methodConverter.poList2VoList((List<MethodEntity>) pageUtils.getList(), methodTagService));
        return pageUtils;
    }

    @Override
    public PageUtils queryPageWithTag(Map<String, Object> params) throws JsonProcessingException {
        String methodName = (String) params.get("key");
        Long createUserId = (Long) params.get("createUserId");

        // 处理 createUserId 和 Constant.SUPER_ADMIN 的逻辑, 默认同时包含用户自己和超级管理员默认上传的（即whitebox）
        List<Long> userIds = new ArrayList<>();
        if (createUserId != null) {
            userIds.add(createUserId);
        }
        userIds.add((long) Constant.SUPER_ADMIN); // 始终添加 SUPER_ADMIN

        IPage<MethodEntity> page = this.page(
                new Query<MethodEntity>().getPage(params),
                new QueryWrapper<MethodEntity>()
                        .like(StringUtils.isNotBlank(methodName), "name", methodName));
        PageUtils pageUtils = new PageUtils(page);
        List<Map<String, Object>> methodList = new ArrayList<>();
        for (int i = 0; i < pageUtils.getList().size(); i++) {
            MethodEntity method = (MethodEntity) pageUtils.getList().get(i);
            objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
            Map<String, Object> result = objectMapper.convertValue((MethodEntity) pageUtils.getList().get(i),
                    Map.class);
            List<TagEntity> tagEntityList = tagService.getTagListByMethodId(method.getId());
            List<String> tagNameList = new ArrayList<>();
            for (TagEntity tagEntity : tagEntityList) {
                tagNameList.add(tagEntity.getName());
            }
            result.put("tagList", tagNameList);
            result.put("params",
                    objectMapper.readValue(method.getParams(), new TypeReference<List<Map<String, Object>>>() {
                    }));
            methodList.add(result);
        }
        pageUtils.setList(methodList);
        return pageUtils;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> uploadMethod(Long userId,
                                            MultipartFile file,
                                            String fileName,
                                            Integer chunkIndex,
                                            Integer chunks) {
        String uuid = UUID.randomUUID().toString();
        File tempFileFolder = null;
        Long recordId = 0L; // 上传记录的id
        boolean uploadSuccess = true;
        try {
            UploadResultDto resultDto = new UploadResultDto();
            // resultDto.setFileId(fileId);
            Date curDate = new Date();

            // 暂存在临时目录
            String tempFolderName = data_td;
            String currentUserFolderName = getFileNameWithoutSuffix(fileName);
            // 创建临时目录
            tempFileFolder = new File(tempFolderName + currentUserFolderName);
            if (!tempFileFolder.exists()) {
                tempFileFolder.mkdirs();
            }
            String tmpFolderPath = tempFileFolder.getPath();
            File newFile = new File(tempFileFolder.getPath() + "/" + chunkIndex);
            file.transferTo(newFile);
            // 不是最后一个分片，直接返回
            if (chunkIndex < chunks - 1) {
                resultDto.setStatus(UploadStatusEnums.UPLOADING.getCode());
                Map<String, Object> result = new HashMap<>();
                result.put("data", resultDto);
                return result;
            }
            // 最后一个分片上传完成，记录数据库，异步合并分片
            String fileSuffix = StringTools.getFileSuffix(fileName);
            // 真实文件名 3178033358LJOgdfrYOj.ico
            String realFileName = currentUserFolderName + fileSuffix;
            FileTypeEnums fileTypeEnum = FileTypeEnums.getFileTypeBySuffix(fileSuffix);

            // Long totalSize = redisComponent.getFileTempSize(webUserDto.getUserId(),
            // fileId);
            // updateUserSpace(webUserDto, totalSize);

            resultDto.setStatus(UploadStatusEnums.UPLOAD_FINISH.getCode());
            UploadRecordEntity record = uploadRecordService.saveOrUpdate(null, userId, fileName, UploadRecordTypeEnum.METHOD, UploadRecordStatusEnum.UPLOAD_TRANSFER, uuid, "");
            recordId = record.getId();
            // 事务提交后调用异步方法
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    System.out.println("上传结束，开始转码...");
                    File targetFolder = new File(method_pd + uuid);
                    if (!targetFolder.exists()) {
                        targetFolder.mkdirs();
                    }
                    String realFileName = currentUserFolderName + fileSuffix;
                    String targetFilePath = targetFolder.getPath() + "/" + realFileName;
                    FileInfoServiceImpl.union(tmpFolderPath, targetFilePath, fileName, true);
                    try {
                        FileUtils.unzip(targetFilePath, targetFolder); // 解压
                        new File(targetFilePath).delete(); // 删除原压缩包
                        // 寻找第一个 .json 文件
                        File jsonFile = FileUtils.findFirstFileWithName(targetFolder, "config.json");
                        if (jsonFile == null) {
                            uploadRecordService.saveOrUpdate(record.getId(), userId, fileName, UploadRecordTypeEnum.METHOD, UploadRecordStatusEnum.UPLOAD_FAIL, uuid, "config.json Not Found");
                            FileUtils.deleteDirectory(targetFolder.getAbsolutePath()); // 删除创建的目录和里面解压的文件
                            return;
                        }

                        // 读取和解析 JSON 文件
                        Map<String, Object> resultMap = objectMapper.readValue(jsonFile, Map.class);
                        if (!validate(resultMap).isEmpty()) {
                            uploadRecordService.saveOrUpdate(record.getId(), userId, fileName, UploadRecordTypeEnum.METHOD, UploadRecordStatusEnum.UPLOAD_FAIL, uuid, "Validation failed: the config.json file is invalid. Please validate it first.");
                            FileUtils.deleteDirectory(targetFolder.getAbsolutePath()); // 删除创建的目录和里面解压的文件
                            return;
                        }
                        MethodEntity methodEntity = FileUtils.parseJsonFile(jsonFile);
                        methodEntity.setCreateTime(curDate);
                        methodEntity.setCreateUserId(userId);
                        methodEntity.setUuid(uuid);
                        // 寻找第一个以 .py、.exe 或 .jar 结尾的文件
                        File scriptFile = FileUtils.findFirstFileWithExtensions(targetFolder,
                                new String[]{methodEntity.getExecution()});
                        if (scriptFile != null) {
                            methodEntity.setExecution(FileUtils.getFileSuffix(scriptFile.getName()));
                            System.out.println("Found script file: " + scriptFile.getAbsolutePath());
                        } else {
                            uploadRecordService.saveOrUpdate(record.getId(), userId, fileName, UploadRecordTypeEnum.METHOD, UploadRecordStatusEnum.UPLOAD_FAIL, uuid, "Script File Not Found");
                            FileUtils.deleteDirectory(targetFolder.getAbsolutePath()); // 删除创建的目录和里面解压的文件
                            return;
                        }
                        saveOrUpdate(methodEntity);
                        methodTagService.saveOrUpdate(methodEntity.getId(), getTagIdList(jsonFile));
                        // End
                        uploadRecordService.saveOrUpdate(record.getId(), userId, fileName, UploadRecordTypeEnum.METHOD, UploadRecordStatusEnum.SUCCESS, uuid, "上传成功");
                    } catch (IOException e) {
                        uploadRecordService.saveOrUpdate(record.getId(), userId, fileName, UploadRecordTypeEnum.METHOD, UploadRecordStatusEnum.UPLOAD_FAIL, uuid, "Internal Server Error");
                        FileUtils.deleteDirectory(targetFolder.getAbsolutePath()); // 删除创建的目录和里面解压的文件
                        e.printStackTrace();
                    }
                }
            });
            Map<String, Object> result = new HashMap<>();
            result.put("data", resultDto);
            result.put("record", record);
            return result;
        } catch (Exception e) {
            uploadSuccess = false;
            uploadRecordService.saveOrUpdate(recordId, userId, fileName, UploadRecordTypeEnum.METHOD, UploadRecordStatusEnum.UPLOAD_FAIL, uuid, "Upload Failed");
            throw new BusinessException("文件上传失败");
        } finally {
            // 如果上传失败，清除临时目录
            if (tempFileFolder != null && !uploadSuccess) {
                try {
                    FileUtils.deleteDirectory(tempFileFolder.getPath());
                } catch (Exception e) {
                    log.error("删除临时目录失败");
                }
            }
        }
    }

    private List<Long> getTagIdList(File jsonFile) throws IOException {
        JsonNode rootNode = objectMapper.readTree(jsonFile);
        JsonNode tagsNode = rootNode.path("Tags");
        List<String> tagArray = new ArrayList<>();
        if (tagsNode.isArray()) {
            ArrayNode tagsArray = (ArrayNode) tagsNode;
            for (JsonNode tag : tagsArray) {
                tagArray.add(tag.asText()); // 将每个元素转化为 String 并添加到列表中
            }
        }
        List<Long> tagIdList = new ArrayList<>();
        // 对于每一个tag字符串，如果数据库已有，记录id，没有的话增加它再记录id
        for (String tag : tagArray) {
            TagEntity tagEntity = tagService.findByName(tag);
            if (tagEntity == null) {
                TagEntity containerTag = new TagEntity();
                containerTag.setName(tag.trim());
                containerTag.setCreateUserId(1L);
                containerTag.setCreateTime(new Date());
                tagService.save(containerTag);
                tagIdList.add(containerTag.getId());
            } else {
                tagIdList.add(tagEntity.getId());
            }
        }
        return tagIdList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveMethod(MethodVo methodVo) throws JsonProcessingException {
        MethodEntity method = new MethodEntity();
        BeanUtils.copyProperties(methodVo, method);
        method.setParams(objectMapper.writeValueAsString(methodVo.getParams()));
        method.setCreateTime(new Date());
        this.save(method);

        // TODO 保存用户自定义的标签

        // 保存方法与标签的关系
        methodTagService.saveOrUpdate(method.getId(), methodVo.getTagIdList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(MethodVo methodVo) throws JsonProcessingException {
        MethodEntity method = new MethodEntity();
        BeanUtils.copyProperties(methodVo, method);
        method.setParams(objectMapper.writeValueAsString(methodVo.getParams()));
        this.updateById(method);

        // 更新方法与标签关系
        methodTagService.saveOrUpdate(method.getId(), methodVo.getTagIdList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(Long[] methodIds) {
        QueryWrapper<MethodEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id", (Object[]) methodIds);  // 使用 in 条件查询
        List<MethodEntity> methodList = this.getBaseMapper().selectList(queryWrapper);

        // 删除方法
        this.removeByIds(Arrays.asList(methodIds));

        // 删除方法与标签和方法运行记录的关联
        methodTagService.deleteBatchByMethodIds(methodIds);
        methodLogService.deleteBatchByMethodIds(methodIds);

        // 事务提交后调用异步方法
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // 删除方法实体
                for (MethodEntity method : methodList) {
                    FileUtils.deleteDirectory(Paths.get(method_pd, method.getUuid()).toAbsolutePath().toString());
                }
            }
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> invoke(Long userId, MethodEntity method, Map<String, Object> params) throws IOException {
        Date startTime = new Date();
        CmdDto cmdDto = commandProcessor.getCmd(userId, method, null, null, params, startTime, true);
        if (cmdDto == null) {
            addMethodLog(method.getId(), userId, null, null, 0, null, 1,
                    "Failed to download file or missing necessary parameters.", startTime);
            return R.error(1, "Failed to download file or missing necessary parameters.");
        }
        // TODO 后续改成 DTO数据传输对象
        ExecutorService executorService = Executors.newFixedThreadPool(10); // 创建一个线程池，处理并发操作
        Future future = executorService.submit(() -> {
            // 方法执行信息
            String info = "";
            List<String> allFileId = new ArrayList<>();
            Map<String, Object> fileIdMap = new HashMap<>();
            try {
                // 启动进程
                ProcessBuilder builder = new ProcessBuilder("cmd", "/c", cmdDto.getCmd());
                Process process = builder.start();

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
                    log.error("发生异常");
                    info += "\r\n Process finished with errors.";
                    addMethodLog(method.getId(), userId, new File(cmdDto.getTmpFilePath()).getName(),
                            cmdDto.getInputFileIds(), 0, null, 1, info, startTime);
                    return R.error(1, info);
                } else {
                    // Upload OutputFile
                    if (cmdDto.getHasOutput()) {
                        // TODO DirectoryOutput
                        File directory = new File(cmdDto.getTmpFilePath());
                        for (Map.Entry<String, Object> entry : cmdDto.getOutputFileNameList().entrySet()) {
                            String key = entry.getKey();
                            Object value = entry.getValue();
                            File[] files = directory.listFiles(file -> {
                                if (value == "DirectoryOutputFlagInfo")
                                    return true;
                                if (file.isFile()) {
                                    String fileNameWithoutSuffix = getFileNameWithoutSuffix(file.getName());
                                    System.out.println("Checking file: " + file.getName() + " (without suffix: "
                                            + fileNameWithoutSuffix + ")");
                                    System.out.println(cmdDto.getOutputFileNameList().get(key).toString());
                                    return getFileNameWithoutSuffix(cmdDto.getOutputFileNameList().get(key).toString())
                                            .equalsIgnoreCase(fileNameWithoutSuffix);
                                }
                                return false;
                            });
                            List<String> fileIdList = new ArrayList<>();
                            for (File file : files) {
                                // 上传文件
                                try {
                                    String fileId = fileInfoService.uploadFileToServer(file);
                                    fileIdList.add(fileId);
                                    allFileId.add(fileId);
                                } catch (Exception e) {
                                    log.error("File upload failed: " + e);
                                    addMethodLog(method.getId(), userId, new File(cmdDto.getTmpFilePath()).getName(),
                                            cmdDto.getInputFileIds(), 0, allFileId, 1,
                                            "File upload failed: " + e.getMessage(), startTime);
                                    return R.error(1, "File upload failed: " + e.getMessage());
                                }
                            }
                            // key为用户输入文件名，没有则是uuid，value为数据中转服务器文件id
                            fileIdMap.put(cmdDto.getOutputFileNameList().get(key).toString(), fileIdList);
                        }
                    } else {
                        // 针对invoke，如果没有输出纯输入，那就返回上传原文件
                        List<File> files = FileUtils.getAllFilesFromDirectory(cmdDto.getTmpFilePath());
                        List<String> fileIdList = new ArrayList<>();
                        for (File file : files) {
                            // 上传文件
                            try {
                                String fileId = fileInfoService.uploadFileToServer(file);
                                fileIdList.add(fileId);
                                allFileId.add(fileId);
                            } catch (Exception e) {
                                log.error("File upload failed: " + e);
                                addMethodLog(method.getId(), userId, new File(cmdDto.getTmpFilePath()).getName(),
                                        cmdDto.getInputFileIds(), 0, allFileId, 1, "File upload failed: " + e.getMessage(),
                                        startTime);
                                return R.error(1, "File upload failed: " + e.getMessage());
                            }
                        }
                        fileIdMap.put("Origin", fileIdList);
                    }
                }

            } catch (IOException | InterruptedException e) {
                // TODO: 异常统一处理
                log.error("Error executing command: " + e);

                info += "Error executing command: " + e.getMessage();
                e.printStackTrace();
                addMethodLog(method.getId(), userId, new File(cmdDto.getTmpFilePath()).getName(),
                        cmdDto.getInputFileIds(), 0, allFileId, 1, info, startTime);
                return R.error(1, info);
            }
            Map<String, Object> result = new HashMap<>();
            result.put("info", info);
            result.put("output", fileIdMap);
            result.put("code", 0);
            return result;
        });
        try {
            // 获取异步任务的结果，这里会阻塞直到任务完成
            Map<String, Object> result = (Map<String, Object>) future.get();
            if ((int) result.get("code") != 0) { // 失败
                return result;
            } else { // 成功
                List<String> allFileId = new ArrayList<>();
                for (Map.Entry<String, Object> entry : ((Map<String, Object>) result.get("output")).entrySet()) {
                    String key = entry.getKey();
                    List<String> fileIdList = (List<String>) entry.getValue();
                    for (String fileId : fileIdList) {
                        allFileId.add(fileId);
                    }
                }
                if (allFileId.size() == 0) {
                    // 如果是空的列表，就置为NULL
                    allFileId = null;
                }
                addMethodLog(method.getId(), userId, new File(cmdDto.getTmpFilePath()).getName(),
                        cmdDto.getInputFileIds().size() == 0 ? null : cmdDto.getInputFileIds(), 0, allFileId, 0,
                        (String) result.get("info"), startTime);
                return R.ok(result);
            } // 确保任务完成后再返回 info 的值
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            addMethodLog(method.getId(), userId, new File(cmdDto.getTmpFilePath()).getName(), cmdDto.getInputFileIds(), 0,
                    null, 1, "Failed to execute async task: " + e.getMessage(), startTime);
            return R.error(1, "Failed to execute async task: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> run(Long userId, MethodEntity method, String executionId, String serviceUuid, Map<String, Object> params) throws IOException {
        Date startTime = new Date();
        String serviceBasePath = null;
        if (serviceUuid != null) serviceBasePath = serviceService.getBasePath(serviceUuid);
        CmdDto cmdDto = commandProcessor.getCmd(userId, method, serviceBasePath, serviceUuid, params, startTime, false);
        if (cmdDto == null) {
            addMethodLog(method.getId(), userId, null, null, serviceUuid == null ? 0 : 1, null, 1, "Missing necessary parameters.", startTime);
            return R.error(1, "Missing necessary parameters.");
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

                List<CmdOutputDto> outputs = new ArrayList<>();
                // 创建读取标准输出流的线程
                BufferedReader stdOutput = new BufferedReader(new InputStreamReader(process.getInputStream(), "GBK"));
                StringBuilder stdInfo = new StringBuilder();
                Thread stdThread = new Thread(() -> {
                    String line;
                    try {
                        while ((line = stdOutput.readLine()) != null) {
                            stdInfo.append(line).append("\n");
                            outputs.add(CmdOutputDto.builder().id(executionId).content(line).finished(false).error(false).timestamp(LocalDateTime.now()).build());
                            outputStore.put(executionId, outputs);
                            Thread.sleep(100); // 模拟输出延迟
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                // 创建读取标准错误流的线程
                BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream(), "GBK"));
                StringBuilder errInfo = new StringBuilder();
                Thread errThread = new Thread(() -> {
                    String line;
                    try {
                        while ((line = stdError.readLine()) != null) {
                            errInfo.append(line).append("\n");
                        }
                    } catch (Exception e) {
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

                // 把错误输出全部添加到outputStore标准输出结束后
                if (!errInfo.toString().isEmpty()) {
                    for (String errLine : errInfo.toString().split("\n")) {
                        outputs.add(CmdOutputDto.builder()
                                .id(executionId).content(errLine).finished(false).error(true).timestamp(LocalDateTime.now()).build());
                    }
                }
                if (!outputs.isEmpty()) {
                    outputs.get(outputs.size() - 1).setFinished(true);
                }

                // 程序执行完后，工作目录的inputSrcFiles删掉，否则影响工作目录内结果的拷贝（因为到时候一股脑把他们全部拷到用户数据文件夹了）
                FileUtils.deleteFilesFromDirectory(cmdDto.getInputSrcFiles());

                if (exitCode != 0) {
                    log.error("发生异常");
                    info += "\r\n Process finished with errors.";
                    addMethodLog(method.getId(), userId, new File(cmdDto.getTmpFilePath()).getName(),
                            cmdDto.getInputFileIds(), serviceUuid == null ? 0 : 1, null, 1, info, startTime);
                    return R.error(1, info);
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
                            List<String> fileIds = fileInfoService.handleSaveToDb(userId, cmdDto.getTmpFilePath(),
                                    cmdDto.getOutputFilePath(), cmdDto.getFileFrontNameList().get(i),
                                    cmdDto.getOutputFileRealNameList().get(i), cmdDto.getNewFilePidList().get(i));
                            fileIdList.addAll(fileIds);
                        }
                    }
                }

            } catch (IOException | InterruptedException e) {
                // TODO: 异常统一处理
                log.error("Error executing command: " + e);

                info += "Error executing command: " + e.getMessage();

                e.printStackTrace();
                addMethodLog(method.getId(), userId, new File(cmdDto.getTmpFilePath()).getName(),
                        cmdDto.getInputFileIds(), serviceUuid == null ? 0 : 1, fileIdList, 1, info, startTime);
                return R.error(1, info);
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
                return result;
            } else {
                addMethodLog(method.getId(), userId, new File(cmdDto.getTmpFilePath()).getName(),
                        cmdDto.getInputFileIds(), serviceUuid == null ? 0 : 1, (List<String>) result.get("output"), 0, (String) result.get("info"),
                        startTime);
                return R.ok(result); // 确保任务完成后再返回 info 的值
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            addMethodLog(method.getId(), userId, new File(cmdDto.getTmpFilePath()).getName(), cmdDto.getInputFileIds(), serviceUuid == null ? 0 : 1,
                    null, 1, "Failed to execute async task: " + e.getMessage(), startTime);
            return R.error(1, "Failed to execute async task: " + e.getMessage());
        }
    }

    @Override
    public List<CmdOutputDto> getOutput(String executionId) {
        return outputStore.getOrDefault(executionId, Collections.emptyList());
    }

    private void addMethodLog(Long methodId, Long userId, String workingDir, List<String> inputFiles, Integer inputType,
                              List<String> outputFiles, int status, String info, Date startTime) throws JsonProcessingException {
        MethodLogEntity methodLog = new MethodLogEntity();
        methodLog.setMethodId(methodId);
        methodLog.setInputType(inputType);
        methodLog.setStatus(status);
        methodLog.setInfo(info);
        methodLog.setStartTime(startTime);
        methodLog.setEndTime(new Date());
        methodLog.setUserId(userId);
        if (workingDir != null)
            methodLog.setWorkingDir(workingDir);
        if (inputFiles != null)
            methodLog.setInputFiles(objectMapper.writeValueAsString(inputFiles));
        if (outputFiles != null)
            methodLog.setOutputFiles(objectMapper.writeValueAsString(outputFiles));
        methodLogService.save(methodLog);
    }

    /**
     * @param userId    用户ID
     * @param method    方法实体
     * @param reqParams 前端传递过来的参数实体
     * @return java.util.Map<java.lang.String, java.lang.Object>
     * @author: Lingkai Shi
     * @description: 得到Cmd命令
     * @date: 8/22/2024 3:50 PM
     */
    // 本函数有图解，在同级目录，以防后面忘记
    private CmdDto getCmd(Long userId, MethodEntity method, String serviceBasePath, String serviceUuid, Map<String, Object> reqParams, Date startTime)
            throws IOException {
        List<Map<String, Object>> paramSpecsList = objectMapper.readValue(method.getParams(),
                new TypeReference<List<Map<String, Object>>>() {
                });
        String cmd = "";

        File wdFolder = new File(method_wd + UUID.randomUUID().toString());
        if (!wdFolder.exists()) {
            wdFolder.mkdirs();
        }
        if (!method.getUuid().equals("whitebox")) {
            // 非Whitebox方法
            cmd += "cd /d \"" + wdFolder.getAbsolutePath() + "\" && ";
        }
        cmd += getToolCmd(method) + " ";

        // 针对 Whitebox，添加方法名和临时工作目录
        if (method.getUuid().equals("whitebox")) {
            cmd += "-r=" + method.getName() + " ";
            cmd += "--wd=" + wdFolder.getAbsolutePath() + " ";
        }

        // 初始化多个变量，方便后期记录，命令执行后处理
        CmdDto cmdDto = new CmdDto();
        boolean isSuccess = true;
        try {
            // 以下开始参数处理
            for (int i = 0; i < paramSpecsList.size(); i++) {
                if (!reqParams.containsKey("val" + i)) {
                    // 用户没有操作此项参数
                    continue;
                }
                Map<String, Object> paramSpecs = paramSpecsList.get(i);

                // 先获取参数输入标识flag
                // 获取 Flags 数组的第一个元素
                Object flagsObj = paramSpecs.get("Flags");
                if (flagsObj instanceof List) {
                    List<String> flagsList = (List<String>) flagsObj;
                    if (!flagsList.isEmpty()) {
                        String firstFlag = flagsList.get(0);
                        // 使用 firstFlag 进行后续处理
                        cmd += firstFlag + " ";
                    } else {
                        // 如果是空就直接输入
                        cmd += " ";
                    }
                }

                // 获取 parameter_type 属性并分类处理
                Object parameterTypeObj = paramSpecs.get("parameter_type");
                String parameterType;
                // 如果parameterType是字符串
                if (parameterTypeObj instanceof String) {
                    // 字符串可能有Boolean、Float、Integer
                    parameterType = (String) parameterTypeObj;
                    // 直接处理字符串
                    if (parameterType.equals("Boolean") || parameterType.equals("Float")
                            || parameterType.equals("Integer") || parameterType.equals("String")
                            || parameterType.equals("StringOrNumber")) {
                        // 获取Boolean
                        String value = Objects.toString(reqParams.get("val" + i));
                        if (value.equals("null")) {
                            cmd = handleNull(cmd, null, paramSpecs);
                        } else {
                            cmd += value + " ";
                        }
                    } else if (parameterType.equals("Directory")) {
                        // 判断是输入还是输出Directory?
                        if ((paramSpecs.get("Type")).equals("DataInput")) {
                            // 设置输入路径为工作路径，具体是拷贝用户选择的源文件夹内的所有文件到工作路径
                            // 在程序执行完后、拷贝最终结果至用户数据文件夹之前把这些文件删除
                            Map<String, Object> fileParent = (Map<String, Object>) reqParams.get("val" + i);
                            String filePid = Objects.toString(fileParent.get("filePid"));
                            List<File> srcFiles = fileInfoService.getFilesByPid(userId, filePid);
                            cmdDto.inputSrcFiles = FileUtils.copyFilesToDirectory(srcFiles, wdFolder.getAbsolutePath());
                            cmdDto.inputFileIds.add(filePid); // TODO 获取所有文件的id，而不是pid
                            cmd += "\"" + wdFolder.getAbsolutePath() + "\" ";
                        } else if ((paramSpecs.get("Type")).equals("DataOutput")) {
                            // DirectoryOutputFlagInfo
                            // 获取 String
                            cmdDto.hasOutput = true;
                            String directoryValue;
                            if (reqParams.get("val" + i) == null) {
                                // 默认保存与输入路径相同
                                directoryValue = cmdDto.inputFilePid; // inputFilePid
                            } else {
                                Map<String, Object> fileParent = (Map<String, Object>) reqParams.get("val" + i);
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
                            cmd += "\"" + wdFolder.getAbsolutePath() + "\" ";
                        }
                    }
                } // 如果parameterType是json
                else if (parameterTypeObj instanceof Map) {
                    // parameter_type 是一个JSON对象（反序列化后的Map）
                    // ExistingFile/NewFile
                    Map<String, Object> parameterTypeMap = (Map<String, Object>) parameterTypeObj;
                    // 如果是FileList
                    List<String> reqParamObjs = new ArrayList<>(); // 不管是多项还是单项，都记录为一个list
                    if (parameterTypeMap.containsKey("FileList")) {
                        // 多项（多文件）
                        reqParamObjs = (List<String>) reqParams.get("val" + i);
                    } else {
                        // 对于单项，就直接添加一个文件id元素或者string（OptionList等）
                        reqParamObjs.add(Objects.toString(reqParams.get("val" + i)));
                    }

                    // 遍历reqParamObjs，此项措施主要为了应对FileList，可以在以下循环中发现，OptionList、NewFile等类型只会执行一次，将他们写到循环体里面主要为了处理方便，实际循环体在作用的只有ExistingFile
                    for (int j = 0; j < reqParamObjs.size(); j++) {
                        String paramObj = reqParamObjs.get(j);
                        if (parameterTypeMap.containsKey("ExistingFile")
                                || parameterTypeMap.containsKey("ExistingFileOrFloat")
                                || parameterTypeMap.containsKey("Vector") || parameterTypeMap.containsKey("FileList")) {
                            // 先判断是不是file
                            // String existingFileId = (String) params.get("val" + i);
                            boolean isNumeric = paramObj.matches("-?\\d+(\\.\\d+)?");
                            if (isNumeric) {
                                cmd = handleNull(cmd, paramObj, paramSpecs);
                            } else {
                                // 是file，获取 ExistingFile
                                FileInfo existingFile = fileInfoService
                                        .getOne(new QueryWrapper<FileInfo>().eq("file_id", paramObj));
                                if (serviceBasePath == null) {
                                    // TODO inputFilePid
                                    cmdDto.inputFilePid = existingFile.getFilePid();
                                    // TODO COPY inputSrcFiles to wdFolder
                                    cmdDto.inputFileIds.add(paramObj);
                                } else {
                                    existingFile = new FileInfo();
                                    existingFile.setFilePath(paramObj);
                                    if (!cmdDto.inputFileIds.contains(serviceUuid)) {
                                        // 避免加重
                                        cmdDto.inputFileIds.add(serviceUuid);
                                    }
                                }

                                Object fileTypeObj;
                                if (parameterTypeMap.containsKey("FileList")) {
                                    fileTypeObj = parameterTypeMap.get("FileList");
                                } else {
                                    fileTypeObj = parameterTypeMap.get("ExistingFile");
                                }
                                // FileList或ExistingFile里面可能还有json
                                if (fileTypeObj instanceof Map) {
                                    Map<String, Object> fileTypeMap = (Map<String, Object>) fileTypeObj;
                                    if (fileTypeMap.containsKey("Vector")
                                            || fileTypeMap.containsKey("RasterAndVector")) {
                                        // 假设是shp
                                        if (existingFile.getFileName().toLowerCase().endsWith(".shp")) {
                                            // 输入shp时，查找云盘父目录下原文件名同名的
                                            fileInfoService.handleShp(existingFile);
                                            cmd = handleInput(cmd, j, reqParamObjs.size(),
                                                    Paths.get(serviceBasePath == null ? data_fd : serviceBasePath, existingFile.getFilePath()).toString());
                                            // TODO shp也应该效法Directory，将shp相关文件拷贝至工作目录
                                        } else {
                                            // RasterAndVector输入的，或者不是shp，直接输入，让exe反馈
                                            cmd = handleInput(cmd, j, reqParamObjs.size(),
                                                    Paths.get(serviceBasePath == null ? data_fd : serviceBasePath, existingFile.getFilePath()).toString());
                                        }
                                    }
                                } else {
                                    // 普通文件
                                    cmd = handleInput(cmd, j, reqParamObjs.size(),
                                            Paths.get(serviceBasePath == null ? data_fd : serviceBasePath, existingFile.getFilePath()).toString());
                                }
                            }
                        } else if (parameterTypeMap.containsKey("NewFile")) {
                            // TODO 暂时不会有FileList
                            // 获取 NewFile
                            String filePid = ((Map<String, Object>) reqParams.get("val" + i)).get("filePid").toString();
                            String fileFrontName = ((Map<String, Object>) reqParams.get("val" + i)).get("outputFileName").toString();
                            cmdDto.newFilePidList.add(filePid);
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
                            cmd += outputFileRealName + " ";
                            // cmd += outputFilePath + File.separator + outputFileRealName + " ";
                        } else if (parameterTypeMap.containsKey("OptionList")) {
                            // TODO 暂时不会有FileList
                            if (parameterTypeMap.get("OptionList") instanceof List) {
                                List<String> optionList = (List<String>) parameterTypeMap.get("OptionList");
                                cmd = handleNull(cmd, (String) reqParams.get("val" + i), paramSpecs);
                            } else {
                                // TODO OptionList里面不是option?
                            }
                        } else if (parameterTypeMap.containsKey("VectorAttributeField")) {
                            cmd = handleNull(cmd, (String) reqParams.get("val" + i), paramSpecs);
                        } else {
                            // CSV等
                            FileInfo existingFile = fileInfoService
                                    .getOne(new QueryWrapper<FileInfo>().eq("file_id", paramObj));
                            if (serviceBasePath == null) {
                                // TODO inputFilePid
                                cmdDto.inputFilePid = existingFile.getFilePid();
                                // TODO COPY inputSrcFiles to wdFolder
                                cmdDto.inputFileIds.add(paramObj);
                            } else {
                                existingFile = new FileInfo();
                                existingFile.setFilePath(paramObj);
                                if (!cmdDto.inputFileIds.contains(serviceUuid)) {
                                    // 避免加重
                                    cmdDto.inputFileIds.add(serviceUuid);
                                }
                            }
                            cmd = handleInput(cmd, j, reqParamObjs.size(),
                                    Paths.get(serviceBasePath == null ? data_fd : serviceBasePath, existingFile.getFilePath()).toString());
                        }
                        // 处理parameterTypeMap
                        parameterType = objectMapper.writeValueAsString(parameterTypeMap);
                    }
                } // 其他数据类型Whitebox没有
                else {
                    // 其他可能的类型处理，例如null值
                    parameterType = "Unknown type";
                }
            }
        } catch (Exception e) {
            isSuccess = false;
            addMethodLog(method.getId(), userId, wdFolder.getName(), null, serviceUuid == null ? 0 : 1, null, 1, "Parameters Error", startTime);
        }
        if (!isSuccess) {
            return null;
        }
        cmdDto.cmd = cmd;
        // cmdDto.fileFrontNameList = cmdDto.getOutputFileRealNameList();
        cmdDto.outputFilePath = Paths.get(data_fd, Objects.toString(userId)).toString();
        cmdDto.tmpFilePath = wdFolder.getAbsolutePath();
        return cmdDto;
    }

    private CmdDto downloadFileAndGetCmd(Long userId, MethodEntity method, Map<String, Object> reqParams,
                                         Date startTime) throws IOException {
        List<Map<String, Object>> paramSpecsList = objectMapper.readValue(method.getParams(),
                new TypeReference<List<Map<String, Object>>>() {
                });
        String cmd = "";
        // TODO 创建临时文件夹作为本次处理的工作目录，目录名称为 UUID，后期要记录到数据库
        File wdFolder = new File(method_wd + UUID.randomUUID().toString());
        if (!wdFolder.exists()) {
            wdFolder.mkdirs();
        }
        if (!method.getUuid().equals("whitebox")) {
            // 非Whitebox方法
            cmd += "cd /d \"" + wdFolder.getAbsolutePath() + "\" && ";
        }
        cmd += getToolCmd(method) + " ";

        // 针对 Whitebox，添加方法名和临时工作目录
        if (method.getUuid().equals("whitebox")) {
            cmd += "-r=" + method.getName() + " ";
            cmd += "--wd=" + wdFolder.getAbsolutePath() + " ";
        }

        // 初始化多个变量，方便后期记录，命令执行后处理
        // String inputFilePid = "0"; //
        // 用以保存输入文件的所在云盘中的的父目录，因为如果方法的输出Directory没有输入，默认应该是以输入路径为输出路径
        // List<File> inputSrcFiles = new ArrayList<>(); //
        // Directory输入需要特殊处理，用以保存用户输入Directory内的所有文件（需要拷贝到临时工作目录，后期方便删除）
        // List<String> inputFileIds = new ArrayList<>();
        // Boolean hasOutput = false; // 记录该方法是否有输出，如果没有后期命令执行完就跳过保存数据库的操作
        // Map<String, Object> outputFileNameList = new HashMap<>();
        CmdDto cmdDto = new CmdDto();
        boolean isSuccess = true;
        try {
            // 以下开始参数处理
            for (int i = 0; i < paramSpecsList.size(); i++) {
                if (!reqParams.containsKey("val" + i)) {
                    // 用户没有操作此项参数
                    continue;
                }
                Map<String, Object> paramSpecs = paramSpecsList.get(i);

                // 先获取参数输入标识flag
                // 获取 Flags 数组的第一个元素
                Object flagsObj = paramSpecs.get("Flags");
                if (flagsObj instanceof List) {
                    List<String> flagsList = (List<String>) flagsObj;
                    if (!flagsList.isEmpty()) {
                        String firstFlag = flagsList.get(0);
                        // 使用 firstFlag 进行后续处理
                        cmd += firstFlag + " ";
                    }
                }

                // 获取 parameter_type 属性并分类处理
                Object parameterTypeObj = paramSpecs.get("parameter_type");
                String parameterType;
                // 如果parameterType是字符串
                if (parameterTypeObj instanceof String) {
                    // 字符串可能有Boolean、Float、Integer
                    parameterType = (String) parameterTypeObj;
                    // 直接处理字符串
                    if (parameterType.equals("Boolean") || parameterType.equals("Float")
                            || parameterType.equals("Integer") || parameterType.equals("String")
                            || parameterType.equals("StringOrNumber")) {
                        // 获取Boolean
                        String value = Objects.toString(reqParams.get("val" + i));
                        if (value.equals("null")) {
                            cmd = handleNull(cmd, null, paramSpecs);
                        } else {
                            cmd += value + " ";
                        }
                    } else if (parameterType.equals("Directory")) {
                        // 判断是输入还是输出Directory？
                        if (((List<String>) paramSpecs.get("Flags")).get(0).equals("--indir")) {
                            // TODO 输入
                        } else if (((List<String>) paramSpecs.get("Flags")).get(0).equals("--outdir")) {
                            cmdDto.setHasOutput(true);
                            cmdDto.cmd += wdFolder.getAbsolutePath() + " ";
                            cmdDto.outputFileNameList.put("val" + i, "DirectoryOutputFlagInfo");
                        }
                    }
                } // 如果parameterType是json
                else if (parameterTypeObj instanceof Map) {
                    // parameter_type 是一个JSON对象（反序列化后的Map）
                    // ExistingFile/NewFile
                    Map<String, Object> parameterTypeMap = (Map<String, Object>) parameterTypeObj;
                    // 如果是FileList
                    List<String> reqParamObjs = new ArrayList<>(); // 不管是多项还是单项，都记录为一个list
                    if (parameterTypeMap.containsKey("FileList")) {
                        // 多项（多文件）
                        reqParamObjs = (List<String>) reqParams.get("val" + i);
                    } else {
                        Object paramObj = reqParams.get("val" + i);
                        if (paramObj instanceof String[]) {
                            // 如果paramObj是字符串数组
                            reqParamObjs = Arrays.asList((String[]) paramObj);
                        } else if (paramObj instanceof Collection<?>) {
                            // 如果paramObj是集合类型，转成字符串数组
                            reqParamObjs = ((Collection<?>) paramObj).stream()
                                    .map(Objects::toString)
                                    .collect(Collectors.toList());
                        } else {
                            // 对于单项，就直接添加一个文件id元素或者string（OptionList等）
                            reqParamObjs.add(Objects.toString(paramObj));
                        }
                    }

                    // 遍历reqParamObjs，此项措施主要为了应对FileList，可以在以下循环中发现，OptionList、NewFile等类型只会执行一次，将他们写到循环体里面主要为了处理方便，实际循环体在作用的只有ExistingFile
                    for (int j = 0; j < reqParamObjs.size(); j++) {
                        String paramObj = reqParamObjs.get(j);
                        if (parameterTypeMap.containsKey("ExistingFile")
                                || parameterTypeMap.containsKey("ExistingFileOrFloat")
                                || parameterTypeMap.containsKey("Vector") || parameterTypeMap.containsKey("FileList")) {
                            // 先判断是不是file
                            // String existingFileId = (String) params.get("val" + i);
                            boolean isNumeric = paramObj.matches("-?\\d+(\\.\\d+)?");
                            if (isNumeric) {
                                cmd = handleNull(cmd, paramObj, paramSpecs);
                            } else {
                                // 是file，下载到临时目录
                                File file = fileInfoService.downloadFile(data_server + '/' + paramObj,
                                        wdFolder.getAbsolutePath(), false);
                                cmdDto.inputSrcFiles.add(file);
                                cmdDto.inputFileIds.add(paramObj);
                                // cmd = handleInput(cmd, j, reqParamObjs.size(), file.getName());

                                Object fileTypeObj;
                                if (parameterTypeMap.containsKey("FileList")) {
                                    fileTypeObj = parameterTypeMap.get("FileList");
                                } else {
                                    fileTypeObj = parameterTypeMap.get("ExistingFile");
                                }
                                // FileList或ExistingFile里面可能还有json
                                if (fileTypeObj instanceof Map) {
                                    Map<String, Object> fileTypeMap = (Map<String, Object>) fileTypeObj;
                                    if (fileTypeMap.containsKey("Vector")
                                            || fileTypeMap.containsKey("RasterAndVector")) {
                                        // 假设是shp
                                        if (file.getName().toLowerCase().endsWith(".zip")) {
                                            // 输入shp时，解压缩
                                            List<File> unzippedFiles = FileUtils.unzip(file.getAbsolutePath(),
                                                    file.getParentFile());
                                            cmdDto.inputSrcFiles.addAll(unzippedFiles);
                                            File shpFile = null;
                                            for (File f : unzippedFiles) {
                                                if (f.getName().toLowerCase().endsWith(".shp")) {
                                                    shpFile = f;
                                                    break;
                                                }
                                            }
                                            if (shpFile == null) {
                                                // 直接输入zip
                                                cmd = handleInput(cmd, j, reqParamObjs.size(), file.getName());
                                            } else {
                                                cmd = handleInput(cmd, j, reqParamObjs.size(), shpFile.getName());
                                            }
                                        } else {
                                            // RasterAndVector输入的，或者不是shp，直接输入，让exe反馈
                                            cmd = handleInput(cmd, j, reqParamObjs.size(), file.getName());
                                        }
                                    }
                                } else {
                                    // 普通文件
                                    cmd = handleInput(cmd, j, reqParamObjs.size(), file.getName());
                                }
                            }
                        } else if (parameterTypeMap.containsKey("NewFile")) {
                            // 获取输出文件的扩展名
                            Object fileTypeObj = parameterTypeMap.get("NewFile");
                            String extension = "";
                            if (fileTypeObj instanceof Map) {
                                // Vector会自动加扩展名
                            } else if (fileTypeObj instanceof String) {
                                if (fileTypeObj.equals("Raster") || fileTypeObj.equals("Vector")) {
                                    extension = "";
                                } else if (fileTypeObj.equals("Lidar")) {
                                    extension = "las";
                                } else {
                                    // 普通文件html\csv等
                                    extension = Objects.toString(fileTypeObj).toLowerCase();
                                }
                            }
                            if (!FileUtils.getFileSuffix((String) reqParams.get("val" + i)).isEmpty()) {
                                extension = FileUtils.getFileSuffix((String) reqParams.get("val" + i));
                            }
                            // 使用 newFile 进行后续处理
                            cmdDto.setHasOutput(true);
                            String outputFileRealName;
                            // 外部invoke，output就是真实的name，因为也不需要存储到云盘里
                            String outputFileRealNameWithoutSuffix = (String) reqParams.get("val" + i);
                            if (outputFileRealNameWithoutSuffix == null) {
                                outputFileRealNameWithoutSuffix = UUID.randomUUID().toString();
                            }
                            if (extension.equals("")) {
                                outputFileRealName = outputFileRealNameWithoutSuffix;
                            } else {
                                outputFileRealName = outputFileRealNameWithoutSuffix + "." + extension;
                            }
                            cmdDto.outputFileNameList.put("val" + i, outputFileRealName);
                            // outputFileRealNameList.add(outputFileRealName);
                            cmd += outputFileRealName + " ";
                            // cmd += outputFilePath + File.separator + outputFileRealName + " ";
                        } else if (parameterTypeMap.containsKey("OptionList")) {
                            if (parameterTypeMap.get("OptionList") instanceof List) {
                                List<String> optionList = (List<String>) parameterTypeMap.get("OptionList");
                                cmd = handleNull(cmd, (String) reqParams.get("val" + i), paramSpecs);
                            } else {
                                // TODO OptionList里面不是option?
                            }
                        } else if (parameterTypeMap.containsKey("VectorAttributeField")) {
                            cmd = handleNull(cmd, (String) reqParams.get("val" + i), paramSpecs);
                        } else {
                            // CSV等
                            File file = fileInfoService.downloadFile(data_server + '/' + paramObj,
                                    wdFolder.getAbsolutePath(), true);
                            cmdDto.inputSrcFiles.add(file);
                            cmdDto.inputFileIds.add(paramObj);
                            cmd = handleInput(cmd, j, reqParamObjs.size(), file.getName());
                        }
                        // 处理parameterTypeMap
                        parameterType = objectMapper.writeValueAsString(parameterTypeMap);
                    }
                } // 其他数据类型Whitebox没有
                else {
                    // 其他可能的类型处理，例如null值
                    parameterType = "Unknown type";
                }
            }
        } catch (Exception e) {
            isSuccess = false;
            addMethodLog(method.getId(), userId, wdFolder.getName(), null, 0, null, 1, "Parameters Error", startTime);
            e.printStackTrace();
        }
        if (!isSuccess) {
            return null;
        }
        cmdDto.setCmd(cmd);
        cmdDto.setTmpFilePath(wdFolder.getAbsolutePath());
        return cmdDto;
    }

    @Override
    public Map<String, Object> getParamType(List<Map<String, Object>> paramSpecsList) {
        Map<String, Object> paramTypes = new HashMap<>();
        List<String> outputList = new ArrayList<>();
        List<String> fileInputList = new ArrayList<>();
        List<String> paramInputList = new ArrayList<>();
        for (int i = 0; i < paramSpecsList.size(); i++) {
            Map<String, Object> paramSpecs = paramSpecsList.get(i);
            Object parameterTypeObj = paramSpecs.get("parameter_type");
            // 若含有FileList/ExistingFile/ExistingFileOrFloat、Directory输入
            if (parameterTypeObj.toString().contains("FileList") || parameterTypeObj.toString().contains("ExistingFile")
                    || parameterTypeObj.toString().contains("ExistingFileOrFloat")
                    || ((Objects.toString(parameterTypeObj)).equals("Directory")
                    && ((List<String>) paramSpecs.get("Flags")).get(0).equals("--indir"))) {
                fileInputList.add("val" + i);
            } else if (parameterTypeObj.toString().contains("NewFile")
                    || ((Objects.toString(parameterTypeObj)).equals("Directory")
                    && ((List<String>) paramSpecs.get("Flags")).get(0).equals("--outdir"))) {
                outputList.add("val" + i);
            } else {
                paramInputList.add("val" + i);
            }
            // 若含有NewFile、Directory输出
        }
        paramTypes.put("Output", outputList);
        paramTypes.put("FileInput", fileInputList);
        paramTypes.put("ParamInput", paramInputList);
        return paramTypes;
    }

    @Override
    public List<ValidationMessage> validate(Map<String, Object> json) throws JsonProcessingException {
        String schema = "{\n" +
                "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" +
                "  \"type\": \"object\",\n" +
                "  \"properties\": {\n" +
                "    \"Name\": { \"type\": \"string\" },\n" +
                "    \"Type\": { \"type\": \"string\", \"enum\": [\"mapping\", \"refactoring\"] },\n" +
                "    \"InputSchema\": { \"type\": \"string\", \"format\": \"textarea\" },\n" +
                "    \"OutputSchema\": { \"type\": \"string\", \"format\": \"textarea\" },\n" +
                "    \"Description\": { \"type\": [\"string\", \"null\"] },\n" +
                "    \"Tags\": { \"type\": \"array\", \"items\": { \"type\": \"string\" } },\n" +
                "    \"Category\": { \"type\": [\"string\", \"null\"] },\n" +
                "    \"Copyright\": { \"type\": [\"string\", \"null\"] },\n" +
                "    \"Parameters\": {\n" +
                "      \"type\": \"array\",\n" +
                "      \"items\": {\n" +
                "        \"type\": \"object\",\n" +
                "        \"properties\": {\n" +
                "          \"Name\": { \"type\": \"string\" },\n" +
                "          \"Flags\": { \"type\": \"array\", \"items\": { \"type\": \"string\" } },\n" +
                "          \"Description\": { \"type\": [\"string\", \"null\"] },\n" +
                "          \"Type\": { \"type\": \"string\", \"enum\": [\"DataInput\", \"DataOutput\", \"ParamInput\"] },\n" +
                "          \"parameter_type\": {\n" +
                "            \"anyOf\": [\n" +
                "              { \"type\": \"string\", \"enum\": [\"Boolean\", \"Integer\", \"Float\", \"String\", \"StringOrNumber\", \"Directory\"] },\n" +
                "              { \"type\": \"object\", \"additionalProperties\": false, \"properties\": { \"ExistingFile\": { \"type\": \"string\" } } },\n" +
                "              { \"type\": \"object\", \"additionalProperties\": false, \"properties\": { \"ExistingFile\": { \"type\": \"object\", \"properties\": { \"Vector\": { \"type\": \"string\", \"enum\": [\"Point\", \"Line\", \"Polygon\", \"Any\"] } } } } },\n" +
                "              { \"type\": \"object\", \"additionalProperties\": false, \"properties\": { \"ExistingFile\": { \"type\": \"object\", \"properties\": { \"RasterAndVector\": { \"type\": \"string\", \"enum\": [\"Point\", \"Line\", \"Polygon\", \"Any\"] } } } } },\n" +
                "              { \"type\": \"object\", \"additionalProperties\": false, \"properties\": { \"ExistingFileOrFloat\": { \"anyOf\": [{ \"type\": \"string\" }, { \"type\": \"object\", \"properties\": { \"Vector\": { \"type\": \"string\", \"enum\": [\"Point\", \"Line\", \"Polygon\", \"Any\"] } } }] } } },\n" +
                "              { \"type\": \"object\", \"additionalProperties\": false, \"properties\": { \"FileList\": { \"anyOf\": [{ \"type\": \"string\" }, { \"type\": \"object\", \"properties\": { \"ExistingFile\": { \"type\": \"string\", \"enum\": [\"Raster\"] } } }, { \"type\": \"object\", \"properties\": { \"Vector\": { \"type\": \"string\", \"enum\": [\"Point\", \"Line\", \"Polygon\", \"Any\"] } } }, { \"type\": \"object\", \"properties\": { \"RasterAndVector\": { \"type\": \"string\", \"enum\": [\"Point\", \"Line\", \"Polygon\", \"Any\"] } } }] } } },\n" +
                "              { \"type\": \"object\", \"additionalProperties\": false, \"properties\": { \"NewFile\": { \"anyOf\": [{ \"type\": \"string\" }, { \"type\": \"object\", \"properties\": { \"Vector\": { \"type\": \"string\", \"enum\": [\"Point\", \"Line\", \"Polygon\", \"Any\"] } } }] } } },\n" +
                "              { \"type\": \"object\", \"additionalProperties\": false, \"properties\": { \"OptionList\": { \"type\": \"array\", \"items\": { \"type\": \"string\" } } } },\n" +
                "              { \"type\": \"object\", \"additionalProperties\": false, \"properties\": { \"VectorAttributeField\": { \"type\": \"array\", \"items\": { \"type\": \"string\" } } } }\n" +
                "            ]\n" +
                "          },\n" +
                "          \"default_value\": { \"anyOf\": [{ \"type\": \"string\" }, { \"type\": \"number\" }, { \"type\": \"boolean\" }, { \"type\": \"null\" }] },\n" +
                "          \"Optional\": { \"type\": \"boolean\" }\n" +
                "        },\n" +
                "        \"required\": [\"Name\", \"Flags\", \"Description\", \"Type\", \"parameter_type\", \"default_value\", \"Optional\"]\n" +
                "      }\n" +
                "    },\n" +
                "    \"Execution\": { \"type\": \"string\", \"enum\": [\"exe\", \"py\", \"jar\"] }\n" +
                "  },\n" +
                "  \"required\": [\"Name\", \"Type\", \"Description\", \"Tags\", \"Category\", \"Copyright\", \"Parameters\", \"Execution\"]\n" +
                "}";
        List<ValidationMessage> list = JsonValidator.validateJson(objectMapper.writeValueAsString(json), schema);
        return list;
    }

    private String handleNull(String cmd, String value, Map<String, Object> param) {
        if (value == null) {
            if (param.get("default_value") == null) {
                // 如果方法配置默认值为null，那就连带删除flag
                cmd = cmd.substring(0, cmd.lastIndexOf(" ", cmd.lastIndexOf(" ") - 1) + 1);
            } else {
                cmd += param.get("default_value") + " ";
            }
        } else if (value.equals("true") || value.equals("false") || value.matches("-?\\d+(\\.\\d+)?")) {
            // 原来是Boolean或数字
            cmd += value + " ";
        } else {
            // 是纯字符串，加上引号
            cmd += "\"" + value + "\" ";
        }
        return cmd;
    }

    private String handleInput(String cmd, int index, int size, String filePath) {
        if (index == 0 && size > 1) {
            // 多文件时，第一个文件的路径
            cmd += "\"" + filePath + ",";
        } else if (index == 0 && size == 1) {
            // 单文件时，文件的路径
            cmd += "\"" + filePath + "\" ";
        } else if (index != size - 1) {
            // 多文件时
            cmd += filePath + ",";
        } else {
            // 多文件时,最后一个
            cmd += filePath + "\" ";
        }
        return cmd;
    }

    private String getToolCmd(MethodEntity method) {
        // 获取exe地址，TODO 需要确保环境路径无空格
        // Whitebox工具专属地址
        String path = "";
        if (method.getUuid().equals("whitebox")) {
            Path directoryPath = Paths.get(method_pd + "whitebox");
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(directoryPath, "*.exe")) {
                for (Path entry : stream) {
                    path = entry.toAbsolutePath().toString();
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (method.getExecution().equals("py")) {
            Path directoryPath = Paths.get(method_pd + method.getUuid());
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(directoryPath, "*.py")) {
                for (Path entry : stream) {
                    path = Paths.get(env_py + "python.exe").toAbsolutePath() + " " + entry.toAbsolutePath().toString() + " "; // python test.py
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (method.getExecution().equals("exe")) {
            Path directoryPath = Paths.get(method_pd + method.getUuid());
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(directoryPath, "*.exe")) {
                for (Path entry : stream) {
                    path = entry.toAbsolutePath().toString() + " ";
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (method.getExecution().equals("jar")) {
            Path directoryPath = Paths.get(method_pd + method.getUuid());
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
