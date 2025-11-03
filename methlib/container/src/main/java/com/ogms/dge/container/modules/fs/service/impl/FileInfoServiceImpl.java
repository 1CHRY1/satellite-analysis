package com.ogms.dge.container.modules.fs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ogms.dge.container.common.exception.BusinessException;
import com.ogms.dge.container.common.utils.Constant;
import com.ogms.dge.container.common.utils.StringTools;
import com.ogms.dge.container.config.AppConfig;
import com.ogms.dge.container.modules.fs.entity.FileInfo;
import com.ogms.dge.container.modules.fs.entity.dto.UploadResultDto;
import com.ogms.dge.container.modules.fs.entity.enums.*;
import com.ogms.dge.container.modules.fs.entity.query.FileInfoQuery;
import com.ogms.dge.container.modules.fs.entity.vo.PaginationResultVO;
import com.ogms.dge.container.modules.fs.mapper.FileInfoMapper;
import com.ogms.dge.container.modules.fs.service.IFileInfoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.ogms.dge.container.common.utils.FileUtils.*;

/**
 * <p>
 * 文件信息 服务实现类
 * </p>
 *
 * @author jin wang
 * @since 2023-08-30
 */
@Slf4j
@Service
public class FileInfoServiceImpl extends ServiceImpl<FileInfoMapper, FileInfo> implements IFileInfoService {

    @Resource
    private AppConfig appConfig;

    @Resource
    // 延迟加载，类加载的时候不进行初始化，只有在第一次访问该字段的时候再加载
    @Lazy
    private FileInfoServiceImpl fileInfoService;

    //! 数据处理方法的工作目录
    @Value("${container.method.wd}")
    private String method_wd;

    @Value("${container.data.fd}")
    private String data_fd;

    @Value("${container.data.td}")
    private String data_td;

    @Value("${container.data.server}")
    private String data_server;

    /**
     * 根据页面查询列表
     *
     * @param query
     */
    @Override
    public PaginationResultVO<FileInfo> findListByPage(FileInfoQuery query) {
        LambdaQueryWrapper<FileInfo> lqw = new LambdaQueryWrapper<>();
        // 默认每页显示15条数据
        int pageSize = query.getPageSize() == null ? PageSize.SIZE15.getSize() : query.getPageSize();
        int pageNo = query.getPageNo() == null ? 1 : query.getPageNo();

        IPage page = new Page(pageNo, pageSize);

        lqw.eq(FileInfo::getUserId, query.getUserId()).
                // 未删除
                        eq(FileInfo::getDelFlag, query.getDelFlag()).
                // 降序
                        orderByDesc(FileInfo::getLastUpdateTime);

        if (null != query.getFilePid()) {
            // 指定查询哪个目录下的文件 (为了兼容 打开分享链接时，只显示分享的文件（或目录），而此时没必须显示其分享前所在的目录)
            lqw.eq(FileInfo::getFilePid, query.getFilePid());
        } else {
            // 如果没有父id，则查询文件
            lqw.eq(FileInfo::getFileId, query.getFileId());
        }

        this.baseMapper.selectPage(page, lqw);

        PaginationResultVO<FileInfo> paginationResultVO = new PaginationResultVO<>();
        paginationResultVO.setTotalCount(page.getTotal());
        paginationResultVO.setPageSize(pageSize);
        paginationResultVO.setPageNo(pageNo);
        paginationResultVO.setPageTotal(page.getPages());
        paginationResultVO.setList(page.getRecords());

        return paginationResultVO;
    }

    /**
     * 上传文件
     *
     * @param userId     用户信息
     * @param fileId     文件id
     * @param file
     * @param fileName
     * @param filePid
     * @param fileMd5
     * @param chunkIndex
     * @param chunks
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UploadResultDto uploadFile(Long userId,
                                      String fileId,
                                      MultipartFile file,
                                      String fileName,
                                      String filePid,
                                      String fileMd5,
                                      Integer chunkIndex,
                                      Integer chunks) {
        if (filePid == null || filePid.isEmpty()) {
            filePid = "0";
        }
        File tempFileFolder = null;
        Boolean uploadSuccess = true;
        try {
            UploadResultDto resultDto = new UploadResultDto();
            // fileId 如果没有，则给一个随机10位数字，当上传第一片后，把这个id发生到前端，下载前端再携带这个id上传第二片
            if (StringTools.isEmpty(fileId)) {
                fileId = StringTools.getRandomString(Constant.LENGTH_10);
            }
            resultDto.setFileId(fileId);
            Date curDate = new Date();

            // TODO:
            // UserSpaceDto spaceDto = redisComponent.getUserSpaceUse(webUserDto.getUserId());
            if (chunkIndex == 0) {
                FileInfoQuery infoQuery = new FileInfoQuery();
                infoQuery.setFileMd5(fileMd5);
                // 设置页面信息
//                infoQuery.setSimplePage(new SimplePage(0, 1));
                // 设置文件状态信息
//                infoQuery.setStatus(FileStatusEnums.USING.getStatus());
                // 根据文件的md5值和状态信息（使用中），查询数据库
//                List<FileInfo> dbFileList = this.fileInfoMapper.selectList(infoQuery);
                //秒传（如果上传的文件已存在，则妙传）
//                if (!dbFileList.isEmpty()) {
//                    FileInfo dbFile = dbFileList.get(0);
//                    //判断文件状态
//                    // TODO： 判断用户空间够不够
////                    if (dbFile.getFileSize() + spaceDto.getUseSpace() > spaceDto.getTotalSpace()) {
////                        throw new BusinessException(ResponseCodeEnum.CODE_904);
////                    }
//                    dbFile.setFileId(fileId);
//                    dbFile.setFilePid(filePid);
//                    dbFile.setUserId(webUserDto.getUserId());
//                    dbFile.setFileMd5(null);
//                    dbFile.setCreateTime(curDate);
//                    dbFile.setLastUpdateTime(curDate);
//                    dbFile.setStatus(FileStatusEnums.USING.getStatus());
//                    dbFile.setDelFlag(FileDelFlagEnums.USING.getFlag());
//                    dbFile.setFileMd5(fileMd5);
//                    fileName = autoRename(filePid, webUserDto.getUserId(), fileName);
//                    dbFile.setFileName(fileName);
//                    this.fileInfoMapper.insert(dbFile);
//                    resultDto.setStatus(UploadStatusEnums.UPLOAD_SECONDS.getCode());
//                    // TODO 更新用户空间使用
//                    // updateUserSpace(webUserDto, dbFile.getFileSize());
//
//                    return resultDto;
//                }
            }
            //暂存在临时目录
            String tempFolderName = data_td;
            String currentUserFolderName = userId + fileId;
            //创建临时目录
            tempFileFolder = new File(tempFolderName + currentUserFolderName);
            if (!tempFileFolder.exists()) {
                tempFileFolder.mkdirs();
            }

            //TODO 判断磁盘空间
//            Long currentTempSize = redisComponent.getFileTempSize(webUserDto.getUserId(), fileId);
//            if (file.getSize() + currentTempSize + spaceDto.getUseSpace() > spaceDto.getTotalSpace()) {
//                throw new BusinessException(ResponseCodeEnum.CODE_904);
//            }

            File newFile = new File(tempFileFolder.getPath() + "/" + chunkIndex);
            file.transferTo(newFile);
            // TODO 保存临时大小
            //redisComponent.saveFileTempSize(webUserDto.getUserId(), fileId, file.getSize());
            // 不是最后一个分片，直接返回
            if (chunkIndex < chunks - 1) {
                resultDto.setStatus(UploadStatusEnums.UPLOADING.getCode());
                return resultDto;
            }
            // 最后一个分片上传完成，记录数据库，异步合并分片
            String fileSuffix = StringTools.getFileSuffix(fileName);
            // 真实文件名 3178033358LJOgdfrYOj.ico
            String realFileName = currentUserFolderName + fileSuffix;
            FileTypeEnums fileTypeEnum = FileTypeEnums.getFileTypeBySuffix(fileSuffix);
            // 自动重命名
            fileName = autoRename(filePid, userId, fileName);
            FileInfo fileInfo = new FileInfo();
            fileInfo.setFileId(fileId);
            fileInfo.setUserId(userId);
            fileInfo.setFileMd5(fileMd5);
            fileInfo.setFileName(fileName);
            // 用用户id分割目录
            fileInfo.setFilePath(Objects.toString(userId) + "/" + realFileName);
            fileInfo.setFilePid(filePid);
            fileInfo.setCreateTime(curDate);
            fileInfo.setLastUpdateTime(curDate);
            fileInfo.setFileCategory(fileTypeEnum.getCategory().getCategory());
            fileInfo.setFileType(fileTypeEnum.getType());
            fileInfo.setStatus(FileStatusEnums.TRANSFER.getStatus());
            fileInfo.setFolderType(FileFolderTypeEnums.FILE.getType());
            fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());
            this.baseMapper.insert(fileInfo);

//            Long totalSize = redisComponent.getFileTempSize(webUserDto.getUserId(), fileId);
//            updateUserSpace(webUserDto, totalSize);

            resultDto.setStatus(UploadStatusEnums.UPLOAD_FINISH.getCode());
            //事务提交后调用异步方法
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    System.out.println("上传结束，开始转码...");
                    fileInfoService.transferFile(fileInfo.getFileId(), userId);
                }
            });
            return resultDto;
        } catch (BusinessException e) {
            uploadSuccess = false;
            log.error("文件上传失败", e);
            throw e;
        } catch (Exception e) {
            uploadSuccess = false;
            log.error("文件上传失败", e);
            throw new BusinessException("文件上传失败");
        } finally {
            // 如果上传失败，清除临时目录
            if (tempFileFolder != null && !uploadSuccess) {
                try {
                    FileUtils.deleteDirectory(tempFileFolder);
                } catch (IOException e) {
                    log.error("删除临时目录失败");
                }

            }
        }
    }

    // 读取文件存储路径下所有与outputFileRealName同名的文件，保存至数据库
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<String> handleSaveToDb(Long userId, String tmpFilePath, String outputFilePath, String fileFrontName, String outputFileRealName, String newFilePid) throws IOException {
        File directory = new File(tmpFilePath);
        // TODO 定期删除临时工作文件夹
        if (!directory.isDirectory()) {
            System.err.println("Provided path is not a directory.");
        }

        // outputFileRealName和fileFrontName都有可能是既包含扩展名的、扩展不包含的，对于raster exe自动加上了扩展名，outputFileRealName就没有，对于csv exe不自动加上的，outputFileRealName有扩展名
        File[] files = directory.listFiles(file -> file.isFile() && getFileNameWithoutSuffix(outputFileRealName).equalsIgnoreCase(getFileNameWithoutSuffix(file.getName())));
        // 拷贝真正文件到用户数据文件夹
        // TODO 唯一id名字before copy
        com.ogms.dge.container.common.utils.FileUtils.copyDirectory(Paths.get(tmpFilePath), Paths.get(outputFilePath));
        List<String> fileIds = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                try {
                    String fileMd5 = calculateFileMd5(file);
                    long fileSize = file.length();
                    String fileName = file.getName();
                    String fileSuffix = getFileSuffix(fileName);
                    FileTypeEnums fileTypeEnum = FileTypeEnums.getFileTypeBySuffix(fileSuffix);
                    if (outputFileRealName == "DirectoryOutputFlagInfo") {
                        fileFrontName = fileName; // TODO 临时处理
                    }
                    String fileFullFrontName = getFileNameWithoutSuffix(fileFrontName) + "." + fileSuffix;
                    // 调用 saveOutputFile 处理文件信息
                    // String month = DateUtil.format(new Date(), DateTimePatternEnum.YYYYMM.getPattern());
                    String fileId = saveOutputFile(userId, Paths.get(Objects.toString(userId), fileName).toString(), fileFullFrontName, newFilePid, fileTypeEnum, fileMd5, fileSize);
                    fileIds.add(fileId);
                } catch (NoSuchAlgorithmException | IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return fileIds;
    }

    // Method处理方法之后的输出文件数据库保存
    private String saveOutputFile(Long userId, String filePath, String fileFullFrontName, String newFilePid, FileTypeEnums fileTypeEnum, String fileMd5, Long fileSize) {
        FileInfo fileInfo = new FileInfo();
        String fileId = StringTools.getRandomString(Constant.LENGTH_10);
        fileInfo.setFileId(fileId);
        fileInfo.setFileSize(fileSize);
        fileInfo.setUserId(userId); // 暂时！
        fileInfo.setFileMd5(fileMd5);
        fileInfo.setFileName(fileFullFrontName);
        fileInfo.setFilePath(filePath);
        fileInfo.setFilePid(newFilePid);
        fileInfo.setCreateTime(new Date());
        fileInfo.setLastUpdateTime(new Date());
        fileInfo.setFileCategory(fileTypeEnum.getCategory().getCategory());
        fileInfo.setFileType(fileTypeEnum.getType());
        fileInfo.setStatus(FileStatusEnums.USING.getStatus());
        fileInfo.setFolderType(FileFolderTypeEnums.FILE.getType());
        fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());
        this.baseMapper.insert(fileInfo);
        return fileId;
    }

    /**
     * 上传的文件自动重命名
     *
     * @param filePid
     * @param userId
     * @param fileName
     * @return
     */
    private String autoRename(String filePid, Long userId, String fileName) {
        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setUserId(userId);
        fileInfoQuery.setFilePid(filePid);
        fileInfoQuery.setDelFlag(FileDelFlagEnums.USING.getFlag());
        fileInfoQuery.setFileName(fileName);
        Integer count = 0;//this.baseMapper.selectCount(fileInfoQuery);
        if (count > 0) {
            return StringTools.rename(fileName);
        }

        return fileName;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    // invoke即使抛出异常，handleShp里执行的数据库操作依然起作用
    public void handleShp(FileInfo existingFile) {
        // 输入shp时，查找云盘父目录下原文件名同名的
        String shpName = com.ogms.dge.container.common.utils.FileUtils.getFileNameWithoutSuffix(existingFile.getFileName());
        List<FileInfo> fileInfoList = fileInfoService.list(new QueryWrapper<FileInfo>()
                .eq("file_pid", existingFile.getFilePid())
                .apply("SUBSTRING_INDEX(file_name, '.', 1) = {0}", shpName));
        String shpRealName = existingFile.getFilePath().substring(existingFile.getFilePath().lastIndexOf("/") + 1, existingFile.getFilePath().lastIndexOf("."));
        for (int j = 0; j < fileInfoList.size(); j++) {
            FileInfo file = fileInfoList.get(j);
            String newPath = file.getFilePath().substring(0, file.getFilePath().lastIndexOf("/") + 1)
                    + shpRealName + "." + com.ogms.dge.container.common.utils.FileUtils.getFileSuffix(file.getFilePath());
            File oldFile = new File(data_fd + file.getFilePath());
            file.setFilePath(newPath);
            FileInfo dbInfo = new FileInfo();
            dbInfo.setLastUpdateTime(new Date());
            dbInfo.setFilePath(newPath);
            LambdaQueryWrapper<FileInfo> lqw = new LambdaQueryWrapper();
            lqw.eq(FileInfo::getFileId, file.getFileId());
            this.baseMapper.update(dbInfo, lqw);
            // 重命名其他使其与shp同名
            oldFile.renameTo(new File(data_fd + newPath));
        }
    }

    @Override
    public File downloadFile(String fileUrl, String saveDir, boolean isUuid) throws IOException {
        // Create URL object
        URL url = new URL(fileUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();

        // Get the filename from Content-Disposition header, if present
        String fileName = getFileName(connection);
        if (fileName == null || fileName.isEmpty()) {
            // Fallback to URL path if header doesn't contain filename
            fileName = Paths.get(url.getPath()).getFileName().toString();
        }

        // 应当防止重名
        if (isUuid)
            fileName = UUID.randomUUID() + "." + com.ogms.dge.container.common.utils.FileUtils.getFileSuffix(fileName);

        // Create directory if it does not exist
        Path directory = Paths.get(saveDir);
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }

        // Create the full path to the destination file
        Path targetPath = directory.resolve(fileName);

        // Open a connection to the URL and download the file
        try (BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
             FileOutputStream fileOutputStream = new FileOutputStream(targetPath.toFile())) {
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        } catch (IOException e) {
            throw new IOException("Failed to download file from " + fileUrl, e);
        } finally {
            connection.disconnect();
        }
        System.out.println("File downloaded to: " + targetPath.toString());
        return targetPath.toFile();
    }

    private static String getFileName(HttpURLConnection conn) {
        try {
            conn.getResponseCode(); // 触发响应

            // 获取 Content-Disposition 头部
            String contentDisposition = conn.getHeaderField("Content-Disposition");
            if (contentDisposition != null) {
                // 解码头部内容，避免乱码
                contentDisposition = new String(contentDisposition.getBytes("ISO-8859-1"), "UTF-8");

                // 查找 fileName= 后面的部分
                String filename = "";
                int filenameIndex = contentDisposition.indexOf("fileName=");
                if (filenameIndex > -1) {
                    filename = contentDisposition.substring(filenameIndex + "fileName=".length());

                    // 移除可能的引号
                    if (filename.startsWith("\"") && filename.endsWith("\"")) {
                        filename = filename.substring(1, filename.length() - 1);
                    }
                }

                return filename;
            }
        } catch (IOException e) {
            System.out.println("解析文件名失败: " + e.getMessage());
        }

        // 如果没有文件名，则返回 null 或适当的默认值
        return null;
    }

    @Override
    public String uploadFileToServer(File file) throws IOException {
        // Prepare headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // Create the body of the request
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("datafile", new FileSystemResource(file));
        body.add("name", file.getName());
        // Prepare the HTTP entity
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        // Send the request
        ResponseEntity<String> response = new RestTemplate(new HttpComponentsClientHttpRequestFactory()).exchange(
                data_server, HttpMethod.POST, requestEntity, String.class);

        // Parse the response to extract data.data.id
        JsonNode rootNode = new ObjectMapper().readTree(response.getBody());
        JsonNode dataIdNode = rootNode.path("data").path("id");

        return dataIdNode.asText();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    // invoke即使抛出异常，handleShp里执行的数据库操作依然起作用
    public List<File> getFilesByPid(Long userId, String pid) {
        // 输入Directory时，查找云盘父目录下原文件名同名的
        List<FileInfo> fileInfoList = fileInfoService.list(new QueryWrapper<FileInfo>()
                .eq("file_pid", pid).eq("del_flag", 2).eq("user_id", userId));
        List<File> files = new ArrayList<>();
        for (int j = 0; j < fileInfoList.size(); j++) {
            File file = new File(Paths.get(data_fd, fileInfoList.get(j).getFilePath()).toString());
            if (file.exists()) {
                files.add(file);
            }
        }
        return files;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public List<FileInfo> getFilesByGroup(Long userId, String fileGroup) {
        // 输入Directory时，查找云盘父目录下原文件名同名的
        List<FileInfo> fileInfoList = fileInfoService.list(new QueryWrapper<FileInfo>()
                .eq("file_group", fileGroup).eq("del_flag", 2).eq("user_id", userId));
        return fileInfoList;
    }

    /**
     * 文件转码
     *
     * @param fileId
     * @param userId
     */
    @Async
    public void transferFile(String fileId, Long userId) {
        Boolean transferSuccess = true;
        String targetFilePath = null;
        String cover = null;
        FileTypeEnums fileTypeEnum = null;
//        FileInfo fileInfo = fileInfoMapper.selectByFileIdAndUserId(fileId, webUserDto.getUserId());

        LambdaQueryWrapper<FileInfo> lqw = new LambdaQueryWrapper<FileInfo>();
        lqw.eq(FileInfo::getFileId, fileId).eq(FileInfo::getUserId, userId);
        FileInfo fileInfo = this.baseMapper.selectOne(lqw);

        try {
            if (fileInfo == null || !FileStatusEnums.TRANSFER.getStatus().equals(fileInfo.getStatus())) {
                return;
            }
            //临时目录
            String tempFolderName = data_td;
            String currentUserFolderName = userId + fileId;
            File fileFolder = new File(tempFolderName + currentUserFolderName);
            if (!fileFolder.exists()) {
                fileFolder.mkdirs();
            }
            //文件后缀
            String fileSuffix = StringTools.getFileSuffix(fileInfo.getFileName());
            // String month = DateUtil.format(fileInfo.getCreateTime(), DateTimePatternEnum.YYYYMM.getPattern());
            //目标目录 D:/24PostGraduate/datacenter/dsc-backend-master/file/
            String targetFolderName = data_fd;
            File targetFolder = new File(targetFolderName + "/" + Objects.toString(userId));
            if (!targetFolder.exists()) {
                targetFolder.mkdirs();
            }
            //真实文件名 3178033358aTczIVb78c.ico
            String realFileName = currentUserFolderName + fileSuffix;
            //真实文件路径 D:\code\dsc\dsc-backend\file\202308/3178033358aTczIVb78c.ico
            targetFilePath = targetFolder.getPath() + "/" + realFileName;
            //合并文件
            union(fileFolder.getPath(), targetFilePath, fileInfo.getFileName(), true);


            //视频文件切割
//            fileTypeEnum = FileTypeEnums.getFileTypeBySuffix(fileSuffix);
//            if (FileTypeEnums.VIDEO == fileTypeEnum) {
//                cutFile4Video(fileId, targetFilePath);
//                //视频生成缩略图
//                cover = month + "/" + currentUserFolderName + Constants.IMAGE_PNG_SUFFIX;
//                String coverPath = targetFolderName + "/" + cover;
//               // ScaleFilter.createCover4Video(new File(targetFilePath), Constants.LENGTH_150, new File(coverPath));
//            } else if (FileTypeEnums.IMAGE == fileTypeEnum) {
//                //生成缩略图
//                cover = month + "/" + realFileName.replace(".", "_.");
//                String coverPath = targetFolderName + "/" + cover;
//                Boolean created = false;// ScaleFilter.createThumbnailWidthFFmpeg(new File(targetFilePath), Constants.LENGTH_150, new File(coverPath), false);
//                if (!created) {
//                    //FileUtils.copyFile(new File(targetFilePath), new File(coverPath));
//                }
//            }
        } catch (Exception e) {
            log.error("文件转码失败，文件Id:{},userId:{}", fileId, userId, e);
            transferSuccess = false;
        } finally {
            FileInfo updateInfo = new FileInfo();
            // 更新文件大小
            updateInfo.setFileSize(new File(targetFilePath).length());
            // 更新文件封面
            updateInfo.setFileCover(cover);
            // 更新 转码状态
            updateInfo.setStatus(transferSuccess ? FileStatusEnums.USING.getStatus() : FileStatusEnums.TRANSFER_FAIL.getStatus());

            LambdaQueryWrapper<FileInfo> lqw2 = new LambdaQueryWrapper<>();
            lqw2.eq(FileInfo::getFileId, fileId).
                    eq(FileInfo::getUserId, userId).
                    eq(FileInfo::getStatus, FileStatusEnums.TRANSFER.getStatus());
            this.baseMapper.update(updateInfo, lqw2);
        }
    }

    /**
     * 合并文件
     *
     * @param dirPath
     * @param toFilePath
     * @param fileName
     * @param delSource
     * @throws BusinessException
     */
    public static void union(String dirPath, String toFilePath, String fileName, boolean delSource) throws BusinessException {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            throw new BusinessException("目录不存在");
        }
        File fileList[] = dir.listFiles();
        File targetFile = new File(toFilePath);
        RandomAccessFile writeFile = null;
        try {
            writeFile = new RandomAccessFile(targetFile, "rw");
            byte[] b = new byte[1024 * 10];
            for (int i = 0; i < fileList.length; i++) {
                int len = -1;
                //创建读块文件的对象
                File chunkFile = new File(dirPath + File.separator + i);
                RandomAccessFile readFile = null;
                try {
                    readFile = new RandomAccessFile(chunkFile, "r");
                    while ((len = readFile.read(b)) != -1) {
                        writeFile.write(b, 0, len);
                    }
                } catch (Exception e) {
                    log.error("合并分片失败", e);
                    throw new BusinessException("合并文件失败");
                } finally {
                    readFile.close();
                }
            }
        } catch (Exception e) {
            log.error("合并文件:{}失败", fileName, e);
            throw new BusinessException("合并文件" + fileName + "出错了");
        } finally {
            try {
                if (null != writeFile) {
                    writeFile.close();
                }
            } catch (IOException e) {
                log.error("关闭流失败", e);
            }
            if (delSource) {
                if (dir.exists()) {
                    try {
                        FileUtils.deleteDirectory(dir);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * 视频文件切割
     *
     * @param fileId
     * @param videoFilePath
     */
    private void cutFile4Video(String fileId, String videoFilePath) {
        //创建同名切片目录
//        File tsFolder = new File(videoFilePath.substring(0, videoFilePath.lastIndexOf(".")));
//        if (!tsFolder.exists()) {
//            tsFolder.mkdirs();
//        }
//        final String CMD_TRANSFER_2TS = "ffmpeg -y -i %s  -vcodec copy -acodec copy -vbsf h264_mp4toannexb %s";
//        final String CMD_CUT_TS = "ffmpeg -i %s -c copy -map 0 -f segment -segment_list %s -segment_time 30 %s/%s_%%4d.ts";
//
//        String tsPath = tsFolder + "/" + Constants.TS_NAME;
//        //生成.ts
//        String cmd = String.format(CMD_TRANSFER_2TS, videoFilePath, tsPath);
//        ProcessUtils.executeCommand(cmd, false);
//        //生成索引文件.m3u8 和切片.ts
//        cmd = String.format(CMD_CUT_TS, tsPath, tsFolder.getPath() + "/" + Constants.M3U8_NAME, tsFolder.getPath(), fileId);
//        ProcessUtils.executeCommand(cmd, false);
//        //删除index.ts
//        new File(tsPath).delete();
    }

    /**
     * 批量移除文件到回收站
     *
     * @param userId
     * @param fileIds
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeFile2RecycleBatch(Long userId, String fileIds) {
        String[] fileIdArray = fileIds.split(",");

        // 1. 查找选中的所有文件(包括目录）
        LambdaQueryWrapper<FileInfo> lqw = new LambdaQueryWrapper<>();
        lqw.eq(FileInfo::getUserId, userId).
                eq(FileInfo::getDelFlag, FileDelFlagEnums.USING.getFlag()).
                in(FileInfo::getFileId, fileIdArray);
        List<FileInfo> fileInfoList = this.baseMapper.selectList(lqw);

        if (fileInfoList.isEmpty()) {
            return;
        }

        // 2. 把文件夹中的子文件都找出来（子文件夹没有找）
        List<String> delSubFileIdList = new ArrayList<>();
        for (FileInfo fileInfo : fileInfoList) {
            findAllSubFolderFileIdList(delSubFileIdList, userId, fileInfo, FileDelFlagEnums.USING.getFlag(), true);
        }

        //将目录下的所有文件更新为已删除
        // ***********  子文件夹中的文件标记为 ‘删除’=0，但是不标记为 '在回收站'=1，因为在回收站中看不到他它们
        // 标记为 删除的 文件就不会出现在查询列表中
        if (!delSubFileIdList.isEmpty()) {
            FileInfo updateInfo = new FileInfo();
            updateInfo.setDelFlag(FileDelFlagEnums.DEL.getFlag());

            LambdaQueryWrapper<FileInfo> lqw2 = new LambdaQueryWrapper<>();
            lqw2.eq(FileInfo::getUserId, userId).
                    eq(FileInfo::getDelFlag, FileDelFlagEnums.USING.getFlag()).
                    in(FileInfo::getFileId, delSubFileIdList);
            this.baseMapper.update(updateInfo, lqw2);
        }

        //将选中的文件更新为回收站 （只有直接选择的文件或文件夹会出现在 回收站中，所以更新这些文件或文件夹的 del_file = 1）
        List<String> delFileIdList = Arrays.asList(fileIdArray);
        FileInfo fileInfo = new FileInfo();
        fileInfo.setRecoveryTime(new Date());
        fileInfo.setDelFlag(FileDelFlagEnums.RECYCLE.getFlag());

        LambdaQueryWrapper<FileInfo> lqw2 = new LambdaQueryWrapper<>();
        lqw2.eq(FileInfo::getUserId, userId).
                eq(FileInfo::getDelFlag, FileDelFlagEnums.USING.getFlag()).
                in(FileInfo::getFileId, delFileIdList);
        this.baseMapper.update(fileInfo, lqw2);

    }

    /**
     * 查找指定目录下的所有子文件
     *
     * @param fileIdList
     * @param userId
     * @param fileInfo
     * @param delFlag
     */
    private void findAllSubFolderFileIdList(List<String> fileIdList,
                                            Long userId,
                                            FileInfo fileInfo,
                                            Integer delFlag,
                                            boolean isFirst) {
        // 如果是目录，则遍历其下的子文件或子文件夹
        if (fileInfo.getFolderType().equals(FileFolderTypeEnums.FOLDER.getType())) {

            // 第一次不作任何操作
            if (!isFirst) {
                // 将子文件夹也添加到列表中返回
                fileIdList.add(fileInfo.getFileId());
            }

            LambdaQueryWrapper<FileInfo> lqw = new LambdaQueryWrapper<>();
            lqw.eq(FileInfo::getUserId, userId).
                    eq(FileInfo::getDelFlag, delFlag).
                    eq(FileInfo::getFilePid, fileInfo.getFileId());
            List<FileInfo> fileInfoList = this.baseMapper.selectList(lqw);

            for (FileInfo fInfo : fileInfoList) {
                findAllSubFolderFileIdList(fileIdList, userId, fInfo, delFlag, false);
            }
        } else {
            // 第一次不作任何操作
            if (!isFirst) {
                // 如果不是目录，则直接添加到 fileIdList中返回
                fileIdList.add(fileInfo.getFileId());
            }
        }
    }


    /**
     * 新建目录
     *
     * @param filePid
     * @param userId
     * @param folderName
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileInfo newFolder(String filePid, Long userId, String folderName) {
        checkFileName(filePid, userId, folderName, FileFolderTypeEnums.FOLDER.getType());

        Date curDate = new Date();
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileId(StringTools.getRandomString(Constant.LENGTH_10));
        fileInfo.setUserId(userId);
        fileInfo.setFilePid(filePid);
        fileInfo.setFileName(folderName);
        fileInfo.setFolderType(FileFolderTypeEnums.FOLDER.getType());
        fileInfo.setCreateTime(curDate);
        fileInfo.setLastUpdateTime(curDate);
        fileInfo.setStatus(FileStatusEnums.USING.getStatus());
        fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());
        this.baseMapper.insert(fileInfo);

        LambdaQueryWrapper<FileInfo> lqw = new LambdaQueryWrapper<>();
        lqw.eq(FileInfo::getFolderType, FileFolderTypeEnums.FOLDER.getType()).
                eq(FileInfo::getFileName, folderName).
                eq(FileInfo::getFilePid, filePid).
                eq(FileInfo::getUserId, userId).
                eq(FileInfo::getDelFlag, FileDelFlagEnums.USING.getFlag());
        Long count = Long.valueOf(this.baseMapper.selectCount(lqw));

        if (count > 1) {
            throw new BusinessException("文件夹" + folderName + "已经存在");
        }
        fileInfo.setFileName(folderName);
        fileInfo.setLastUpdateTime(curDate);
        return fileInfo;
    }

    /**
     * 新建目录或重命名时检查是否同名
     *
     * @param filePid    当前目录
     * @param userId     用户id
     * @param fileName   新建目录名 或 要重命名之后的名称
     * @param folderType 0 文件 1 目录
     */
    private void checkFileName(String filePid, Long userId, String fileName, Integer folderType) {
        LambdaQueryWrapper<FileInfo> lqw = new LambdaQueryWrapper<>();
        lqw.eq(FileInfo::getFolderType, folderType).
                eq(FileInfo::getFileName, fileName).
                eq(FileInfo::getFilePid, filePid).
                eq(FileInfo::getUserId, userId).
                eq(FileInfo::getDelFlag, FileDelFlagEnums.USING.getFlag());

        Long count = Long.valueOf(this.baseMapper.selectCount(lqw));

        if (count > 0) {
            throw new BusinessException("此目录下已存在同名文件，请修改名称");
        }
    }

    /**
     * 获取文件夹信息
     *
     * @param lqw
     * @return
     */
    @Override
    public List<FileInfo> getFolderInfo(LambdaQueryWrapper<FileInfo> lqw) {
        return this.baseMapper.selectList(lqw);
    }

    /**
     * 从回收站删除文件
     *
     * @param userId
     * @param fileIds
     * @param adminOp
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delFileBatchFromRecycle(Long userId, String fileIds, Boolean adminOp) {
        String[] fileIdArray = fileIds.split(",");

        LambdaQueryWrapper<FileInfo> lqw = new LambdaQueryWrapper<>();
        lqw.eq(FileInfo::getUserId, userId);

        if (!adminOp) {
            // 删除回收站中的数据
            lqw.eq(FileInfo::getDelFlag, FileDelFlagEnums.RECYCLE.getFlag());
        }
        lqw.in(FileInfo::getFileId, fileIdArray);
        // 从数据库中查询到要删除的文件（包括目录）
        List<FileInfo> fileInfoList = this.baseMapper.selectList(lqw);

        // 删除文件夹中的子目录
        List<String> delFileSubFolderFileIdList = new ArrayList<>();
        // 找到所选文件子目录文件ID
        for (FileInfo fileInfo : fileInfoList) {
            if (FileFolderTypeEnums.FOLDER.getType().equals(fileInfo.getFolderType())) {
                findAllSubFolderFileIdList(delFileSubFolderFileIdList, userId, fileInfo, FileDelFlagEnums.DEL.getFlag(), true);
            }
        }

        //删除目录中的子文件
        if (!delFileSubFolderFileIdList.isEmpty()) {
            // 先删除子文件（标记为 删除的文件【即在目录中的文件， 回收站是看不到的，回收站只看到文件夹】）
            LambdaQueryWrapper<FileInfo> lqw2 = new LambdaQueryWrapper<>();
            lqw2.eq(FileInfo::getUserId, userId).
                    in(FileInfo::getFileId, delFileSubFolderFileIdList).
                    eq(FileInfo::getDelFlag, adminOp ? null : FileDelFlagEnums.DEL.getFlag());
            this.baseMapper.delete(lqw2);
        }
        // 删除所选文件（删除回收站中显示的文件）
        LambdaQueryWrapper<FileInfo> lqw3 = new LambdaQueryWrapper<>();
        lqw3.eq(FileInfo::getUserId, userId).
                in(FileInfo::getFileId, fileIdArray).
                eq(FileInfo::getDelFlag, adminOp ? null : FileDelFlagEnums.RECYCLE.getFlag());
        this.baseMapper.delete(lqw3);


//        Long useSpace = this.fileInfoMapper.selectUseSpace(userId);
//        UserInfo userInfo = new UserInfo();
//        userInfo.setUseSpace(useSpace);
//        this.userInfoMapper.updateByUserId(userInfo, userId);

        //设置缓存
//        UserSpaceDto userSpaceDto = redisComponent.getUserSpaceUse(userId);
//        userSpaceDto.setUseSpace(useSpace);
//        redisComponent.saveUserSpaceUse(userId, userSpaceDto);
    }

    /**
     * 从回收站恢复文件
     *
     * @param userId
     * @param fileIds
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recoverFileBatchFromRecycle(Long userId, String fileIds) {
        String[] fileIdArray = fileIds.split(",");

        // 找到回收站根目录下（用户选中想要恢复）的 文件夹 及其 平级的文件
        LambdaQueryWrapper<FileInfo> lqw = new LambdaQueryWrapper<>();
        lqw.eq(FileInfo::getUserId, userId).
                in(FileInfo::getFileId, fileIdArray).
                eq(FileInfo::getDelFlag, FileDelFlagEnums.RECYCLE.getFlag());
        List<FileInfo> fileInfoList = this.baseMapper.selectList(lqw);

        // 找到想要恢复的文件夹中的子文件（只有文件夹的del_flag被标识为回收站，而文件夹中的子文件的del_flag被标志为删除）
        List<String> delFileSubFolderFileIdList = new ArrayList<>();
        //找到所选文件子目录文件ID
        for (FileInfo fileInfo : fileInfoList) {
            if (FileFolderTypeEnums.FOLDER.getType().equals(fileInfo.getFolderType())) {
                // 目录子文件的 del_flag 是 0  表示已删除
                findAllSubFolderFileIdList(delFileSubFolderFileIdList, userId, fileInfo, FileDelFlagEnums.DEL.getFlag(), true);
            }
        }
        //查询所有根目录的文件 (用于重命名）
        LambdaQueryWrapper<FileInfo> lqw2 = new LambdaQueryWrapper<>();
        lqw2.eq(FileInfo::getUserId, userId).
                eq(FileInfo::getDelFlag, FileDelFlagEnums.USING.getFlag()).
                eq(FileInfo::getFilePid, Constant.ZERO_STR);
        List<FileInfo> allRootFileList = this.baseMapper.selectList(lqw2);

        // 将 allRootFileList 转为 Map 集合 （key为文件名，value为FileInfo）
        Map<String, FileInfo> rootFileMap = allRootFileList.stream().
                collect(Collectors.toMap(FileInfo::getFileName, Function.identity(), (file1, file2) -> file2));

        //查询所有所选文件
        //将目录下的所有删除的文件更新为正常
        if (!delFileSubFolderFileIdList.isEmpty()) {
            FileInfo fileInfo = new FileInfo();
            fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());

            LambdaQueryWrapper<FileInfo> lqw3 = new LambdaQueryWrapper<>();
            lqw3.eq(FileInfo::getUserId, userId).
                    eq(FileInfo::getDelFlag, FileDelFlagEnums.DEL.getFlag()).
                    in(FileInfo::getFileId, delFileSubFolderFileIdList);

            this.baseMapper.update(fileInfo, lqw3);
        }

        //将选中的文件(有文件和文件夹，del_flag为回收站，即 1)更新为正常（del_flag为2）
        List<String> delFileIdList = Arrays.asList(fileIdArray);
        FileInfo fileInfo = new FileInfo();
        fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());
//        fileInfo.setFilePid(Constants.ZERO_STR);
        fileInfo.setLastUpdateTime(new Date());

        LambdaQueryWrapper<FileInfo> lqw4 = new LambdaQueryWrapper<>();
        lqw4.eq(FileInfo::getUserId, userId).
                eq(FileInfo::getDelFlag, FileDelFlagEnums.RECYCLE.getFlag()).
                in(FileInfo::getFileId, delFileIdList);
        this.baseMapper.update(fileInfo, lqw4);

        // 将所选文件重命名
        for (FileInfo item : fileInfoList) { // 遍历选择的文件或目录
            FileInfo rootFileInfo = rootFileMap.get(item.getFileName());
            // 文件名已经存在，重命名被还原的文件名
            if (rootFileInfo != null) {
                String fileName = StringTools.rename(item.getFileName());
                FileInfo updateInfo = new FileInfo();
                updateInfo.setFileName(fileName);

                LambdaQueryWrapper<FileInfo> lqw5 = new LambdaQueryWrapper<>();
                lqw5.eq(FileInfo::getUserId, userId).
                        eq(FileInfo::getFileId, item.getFileId());
                this.baseMapper.update(updateInfo, lqw5);
            }
        }
    }


    /**
     * 重命名
     *
     * @param fileId
     * @param userId
     * @param fileName
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileInfo rename(String fileId, Long userId, String fileName) {
        // 查看文件是否存在
        LambdaQueryWrapper<FileInfo> lqw = new LambdaQueryWrapper();
        lqw.eq(FileInfo::getFileId, fileId).eq(FileInfo::getUserId, userId);
        FileInfo fileInfo = this.baseMapper.selectOne(lqw);

        if (fileInfo == null) {
            throw new BusinessException("文件不存在");
        }
        // 如果修改后的名字和之前的一样，直接返回
        if (fileInfo.getFileName().equals(fileName)) {
            return fileInfo;
        }
        // 获取当前文件所在目录id
        String filePid = fileInfo.getFilePid();
        checkFileName(filePid, userId, fileName, fileInfo.getFolderType());
        //文件获取后缀，前端传递过来的 fileName 不包括文件后缀
        if (FileFolderTypeEnums.FILE.getType().equals(fileInfo.getFolderType())) {
            fileName = fileName + StringTools.getFileSuffix(fileInfo.getFileName());
        }

        // 更新数据库，修改名称
        Date curDate = new Date();
        FileInfo dbInfo = new FileInfo();
        dbInfo.setFileName(fileName);
        dbInfo.setLastUpdateTime(curDate);
        //this.fileInfoMapper.updateByFileIdAndUserId(dbInfo, fileId, userId);
        LambdaQueryWrapper<FileInfo> lqw2 = new LambdaQueryWrapper();
        lqw2.eq(FileInfo::getFileId, fileId).eq(FileInfo::getUserId, userId);
        this.baseMapper.update(dbInfo, lqw2);


        // 再次查询是否同名
        LambdaQueryWrapper<FileInfo> lqw3 = new LambdaQueryWrapper();
        lqw3.eq(FileInfo::getFilePid, filePid)
                .eq(FileInfo::getUserId, userId)
                .eq(FileInfo::getFileName, fileName)
                .eq(FileInfo::getDelFlag, FileDelFlagEnums.USING.getFlag());
        Long count = Long.valueOf(this.baseMapper.selectCount(lqw3));

        if (count > 1) {
            throw new BusinessException("文件名" + fileName + "已经存在");
        }

        // 将修改后的实体返回到前端（用于前端展示）
        fileInfo.setFileName(fileName);
        fileInfo.setLastUpdateTime(curDate);
        return fileInfo;
    }

    /**
     * 改变文件目录
     *
     * @param fileIds 文件id，是以逗号分割的文件id字符串
     * @param filePid 文件父目录
     * @param userId  用户id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeFileFolder(String fileIds, String filePid, Long userId) {
        if (fileIds.equals(filePid)) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        // 判断要移动到的目标文件夹是不是根目录
        if (!Constant.ZERO_STR.equals(filePid)) {
            // 如果不是要移动到根目录下，则先查询到 父目录 文件夹信息
            LambdaQueryWrapper<FileInfo> lqw = new LambdaQueryWrapper<>();
            lqw.eq(FileInfo::getFileId, filePid)
                    .eq(FileInfo::getUserId, userId);
            // 目标文件夹信息
            FileInfo fileInfo = this.baseMapper.selectOne(lqw);

            // FileInfo fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(filePid, userId);
            if (fileInfo == null || !FileDelFlagEnums.USING.getFlag().equals(fileInfo.getDelFlag())) {
                throw new BusinessException(ResponseCodeEnum.CODE_600);
            }
        }

        // 查询要移动的目标目录中所有的文件信息
        LambdaQueryWrapper<FileInfo> lqw2 = new LambdaQueryWrapper<>();
        lqw2.eq(FileInfo::getFilePid, filePid)
                .eq(FileInfo::getUserId, userId);
        List<FileInfo> dbFileList = this.baseMapper.selectList(lqw2);
        // 得到一个Map，key是文件名，value是文件信息对象(FileInfo)
        Map<String, FileInfo> dbFileNameMap = dbFileList.stream()
                .collect(Collectors.toMap(FileInfo::getFileName, Function.identity(), (file1, file2) -> file2));

        // 查询所有的要移动的文件信息（即在前端中选中的那些文件）
        String[] fileIdArray = fileIds.split(",");
        LambdaQueryWrapper<FileInfo> lqw3 = new LambdaQueryWrapper<>();
        lqw3.eq(FileInfo::getUserId, userId)
                .in(FileInfo::getFileId, fileIdArray);
        List<FileInfo> selectFileList = this.baseMapper.selectList(lqw3);

        //将所选文件重命名(如果目标文件夹中已经包含了要移动的文件名）
        for (FileInfo item : selectFileList) {
            FileInfo rootFileInfo = dbFileNameMap.get(item.getFileName());
            //文件名已经存在，重命名被还原的文件名
            FileInfo updateInfo = new FileInfo();
            // 说明有重名文件
            if (rootFileInfo != null) {
                String fileName = StringTools.rename(item.getFileName());
                updateInfo.setFileName(fileName);
            }
            updateInfo.setFilePid(filePid);

            LambdaQueryWrapper<FileInfo> lqw4 = new LambdaQueryWrapper<>();
            lqw4.eq(FileInfo::getUserId, userId)
                    .eq(FileInfo::getFileId, item.getFileId());
            this.baseMapper.update(updateInfo, lqw4);
        }
    }

//    /**
//     * 根据 fileId 和 userId 获取文件信息
//     * @param fileId
//     * @param userId
//     * @return
//     */
//    @Override
//    public FileInfo getFileInfoByFileIdAndUserId(String fileId, String userId) {
//            LambdaQueryWrapper<FileInfo> lqw = new LambdaQueryWrapper<>();
//            lqw.eq(FileInfo::getUserId, userId)
//                    .eq(FileInfo::getFileId, fileId);
//            return this.baseMapper.selectOne(lqw);
//    }

    ////////////////////// 分享 //////////////////////////////////////

    @Override
    public void checkRootFilePid(String rootFilePid, Long shareUserId, String filePid) {
        if (StringTools.isEmpty(filePid)) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (rootFilePid.equals(filePid)) {
            return;
        }
        checkFilePid(rootFilePid, filePid, shareUserId);
    }

    private void checkFilePid(String rootFilePid, String fileId, Long userId) {
        FileInfo fileInfo = this.baseMapper.getFileInfoByFileIdAndUserId(fileId, userId);
        if (fileInfo == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (Constant.ZERO_STR.equals(fileInfo.getFilePid())) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (fileInfo.getFilePid().equals(rootFilePid)) {
            return;
        }
        checkFilePid(rootFilePid, fileInfo.getFilePid(), userId);
    }
}
