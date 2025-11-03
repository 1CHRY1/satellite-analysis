package com.ogms.dge.workspace.modules.fs.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ogms.dge.workspace.modules.fs.entity.FileInfo;
import com.ogms.dge.workspace.modules.fs.entity.dto.UploadResultDto;
import com.ogms.dge.workspace.modules.fs.entity.query.FileInfoQuery;
import com.ogms.dge.workspace.modules.fs.entity.vo.PaginationResultVO;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * <p>
 * 文件信息 服务类
 * </p>
 * @author jin wang
 * @since 2023-08-30
 */
public interface IFileInfoService extends IService<FileInfo> {
    /**
     * 上传文件
     * @param userId 用户信息
     * @param fileId
     * @param file
     * @param fileName
     * @param filePid
     * @param fileMd5
     * @param chunkIndex
     * @param chunks
     * @return
     */
    UploadResultDto uploadFile(Long userId,
                               String fileId,
                               MultipartFile file,
                               String fileName,
                               String filePid,
                               String fileMd5,
                               Integer chunkIndex,
                               Integer chunks);

    /**
     * 查找文件集合（按页面）
     * @param query
     * @return
     */
    PaginationResultVO<FileInfo> findListByPage(FileInfoQuery query);

    // 读取文件存储路径下所有与outputFileRealName同名的文件，保存至数据库
    @Transactional(rollbackFor = Exception.class)
    List<String> handleSaveToDb(Long userId, String tmpFilePath, String outputFilePath, String fileFrontName, String outputFileRealName, String newFilePid, String fileGroup) throws IOException;

    void handleShp(FileInfo existingFile);

    File downloadFile(String fileUrl, String saveDir, boolean isUuid) throws IOException;

    String uploadFileToServer(File file) throws IOException;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class) // invoke即使抛出异常，handleShp里执行的数据库操作依然起作用
    List<File> getFilesByPid(Long userId, String pid);

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    // invoke即使抛出异常，handleShp里执行的数据库操作依然起作用
    List<FileInfo> getFilesByGroup(Long userId, String fileGroup);

    /**
     * 批量删除文件到回收站
     * @param userId
     * @param fileIds
     */
    void removeFile2RecycleBatch(Long userId, String fileIds);

    /**
     * 新建文件夹
     * @param filePid
     * @param userId
     * @param folderName
     * @return
     */
    FileInfo newFolder(String filePid, Long userId, String folderName);

    /**
     * 获取文件夹信息
     * @param lqw
     * @return
     */
    List<FileInfo> getFolderInfo(LambdaQueryWrapper<FileInfo> lqw);

    /**
     * 批量移除文件到回收站
     * @param userId
     * @param fileIds
     */
    void recoverFileBatchFromRecycle(Long userId, String fileIds);

    /**
     * 批量删除回收站中的文件
     * @param userId
     * @param fileIds
     * @param adminOp
     */
    void delFileBatchFromRecycle(Long userId, String fileIds, Boolean adminOp);

    /**
     * 重命名
     * @param fileId
     * @param userId
     * @param fileName
     * @return
     */
    FileInfo rename(String fileId, Long userId, String fileName);

    /**
     * 改变文件目录
     * @param fileIds
     * @param filePid
     * @param userId
     */
    void changeFileFolder(String fileIds, String filePid, Long userId);

//    /**
//     * 根据 fileId 和 userId 获取文件信息
//     * @param fileId
//     * @param userId
//     * @return
//     */
    //FileInfo getFileInfoByFileIdAndUserId(String fileId, String userId);

    void checkRootFilePid(String fileId, Long shareUserId, String filePid);
}
