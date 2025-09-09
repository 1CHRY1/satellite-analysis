package nnu.mnr.satellite.service.minIO;

import com.baomidou.dynamic.datasource.annotation.DS;
import io.minio.*;
import io.minio.messages.Item;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.model.dto.minIO.FileEditDTO;
import nnu.mnr.satellite.model.dto.minIO.FileUploadDTO;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.model.vo.user.UserMinIOFilesVO;
import nnu.mnr.satellite.utils.dt.MinioUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@DS("mysql_ard_iam")
public class MinIOService {
    private final MinioClient minioClient;
    private final MinioUtil minioUtil;

    public MinIOService(MinioClient minioClient, MinioUtil minioUtil) {
        this.minioClient = minioClient;
        this.minioUtil = minioUtil;
    }

    public CommonResultVO getUserMinIODirectoryTree(String userId) throws Exception {
        String bucketName = "user";
        String prefix = "user-files/" + userId + "/";

        // 确保bucket存在
        boolean found = minioUtil.existBucket(bucketName);
        if (!found) {
            throw new RuntimeException("Bucket not found");
        }

        // 获取对象列表
        Iterable<Result<Item>> objects = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(prefix)
                        .recursive(true)
                        .build());

        // 构建目录树
        UserMinIOFilesVO userMinIOFilesVO = buildFileTree(objects, prefix, bucketName);
        return CommonResultVO.builder().status(1).message("Get User Files Success").data(userMinIOFilesVO).build();
    }

    private UserMinIOFilesVO buildFileTree(Iterable<Result<Item>> objects, String prefix, String bucketName) throws Exception {
        Map<String, UserMinIOFilesVO> nodeMap = new HashMap<>();
        UserMinIOFilesVO root = new UserMinIOFilesVO();
        if (prefix.isEmpty()) {
            root.setName(bucketName); // 根目录显示 bucket 名称
        } else {
            // 去掉末尾的 "/" 并取最后一层作为名称
            String trimmedPrefix = prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
            String lastPart = trimmedPrefix.substring(trimmedPrefix.lastIndexOf("/") + 1);
            root.setName(lastPart);
        } // 根节点名称
        root.setPath(bucketName + "/" + prefix); // 完整路径（如 "my-bucket/user-files/123/"）
        root.setDir(true);
        root.setChildren(new ArrayList<>());
        nodeMap.put(prefix, root);

        // 第一遍：构建所有节点
        for (Result<Item> result : objects) {
            Item item = result.get();
            String objectName = item.objectName();

            if (objectName.equals(prefix)) {
                continue;
            }

            String relativePath = objectName.substring(prefix.length());
            String[] parts = relativePath.split("/");
            String currentPath = prefix;
            UserMinIOFilesVO parent = root;

            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                if (part.isEmpty()) {
                    continue;
                }

                currentPath += part + "/";
                boolean isLastPart = (i == parts.length - 1);
                boolean isDir = !isLastPart || (isLastPart && objectName.endsWith("/"));

                if (!nodeMap.containsKey(currentPath)) {
                    UserMinIOFilesVO node = new UserMinIOFilesVO();
                    node.setName(part); // 关键修改：仅取文件夹名
                    node.setPath(bucketName + "/" + currentPath); // 关键修改：加上 bucketName
                    node.setDir(isDir);
                    node.setChildren(isDir ? new ArrayList<>() : null);
                    nodeMap.put(currentPath, node);

                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(node);
                }

                parent = nodeMap.get(currentPath);
            }

            // 如果是文件，设置大小和修改时间
            if (!parent.isDir()) {
                parent.setSize(item.size());
                parent.setLastModified(item.lastModified().toOffsetDateTime());
            }
        }

        // 第二遍：递归计算文件夹大小 + 设置 lastModified（取最新子文件时间）
        calculateFolderSizeAndLastModified(root);

        return root;
    }

    // 递归计算文件夹大小 + 设置 lastModified（取最新子文件时间）
    private void calculateFolderSizeAndLastModified(UserMinIOFilesVO node) {
        if (!node.isDir() || node.getChildren() == null) {
            return;
        }

        long totalSize = 0;
        OffsetDateTime latestModified = null;

        for (UserMinIOFilesVO child : node.getChildren()) {
            if (child.isDir()) {
                calculateFolderSizeAndLastModified(child); // 递归计算子文件夹
            }
            totalSize += child.getSize();
            // 更新最新修改时间
            if (child.getLastModified() != null) {
                if (latestModified == null || child.getLastModified().isAfter(latestModified)) {
                    latestModified = child.getLastModified();
                }
            }
        }

        node.setSize(totalSize);
        node.setLastModified(latestModified); // 文件夹的 lastModified 取最新子文件时间
    }

    // 获取单个文件的详细信息
    public CommonResultVO getUserFileInfo(String filePath) throws Exception {
        // 1. 解析完整路径（格式应为 "bucketName/objectPath"）
        if (filePath == null || !filePath.contains("/")) {
            throw new IllegalArgumentException("Invalid file path: must include bucket name (e.g., 'user/path/to/file')");
        }

        // 2. 拆分 bucketName 和 objectPath
        String bucketName = filePath.substring(0, filePath.indexOf("/"));
        String objectPath = filePath.substring(filePath.indexOf("/") + 1);

        // 3. 检查路径是否为空
        if (objectPath.isEmpty()) {
            throw new IllegalArgumentException("Object path cannot be empty");
        }

        StatObjectResponse stat = minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectPath)
                        .build());

        UserMinIOFilesVO node = new UserMinIOFilesVO();
        node.setName(stat.object().split("/")[stat.object().split("/").length - 1]);
        node.setPath(stat.bucket() + "/" + stat.object());
        node.setDir(stat.object().endsWith("/"));
        node.setSize(stat.size());
        node.setLastModified(stat.lastModified().toOffsetDateTime());

        return CommonResultVO.builder().status(1).message("Get User File Success").data(node).build();
    }

    public CommonResultVO uploadFile(FileUploadDTO fileUploadDTO) throws Exception {
        String[] parts = fileUploadDTO.getFilePath().split("/", 2);
        String bucketName = parts[0];
        String filePath = parts[1];
        MultipartFile file = fileUploadDTO.getFile();
        minioUtil.upload(file, filePath, bucketName);
        return CommonResultVO.builder()
                .message("文件上传成功")
                .status(1)
                .build();
    }

    public CommonResultVO downloadFile(String fullPath, HttpServletResponse response) throws Exception {
        String[] parts = fullPath.split("/", 2);
        String bucketName = parts[0];
        String filePath = parts[1];
        minioUtil.download(response, filePath, bucketName);
        return CommonResultVO.builder()
                .message("文件下载成功")
                .status(1)
                .build();
    }

    public CommonResultVO deleteFile(String fullPath) throws Exception {
        String[] parts = fullPath.split("/", 2);
        String bucketName = parts[0];
        String filePath = parts[1];
        minioUtil.delete(filePath, bucketName);
        return CommonResultVO.builder()
                .message("文件删除成功")
                .status(1)
                .build();
    }

    public CommonResultVO editFile(FileEditDTO fileEditDTO) throws Exception {
        String[] oldFileParts = fileEditDTO.getOldFilePath().split("/", 2);
        String oldBucketName = oldFileParts[0];
        String oldFilePath = oldFileParts[1];
        String[] newFileParts = fileEditDTO.getNewFilePath().split("/", 2);
        String newBucketName = newFileParts[0];
        String newFilePath = newFileParts[1];
        minioUtil.copyObject(oldBucketName, oldFilePath, newBucketName, newFilePath);
        minioUtil.delete(oldFilePath, oldBucketName);
        return CommonResultVO.builder()
                .message("文件修改成功")
                .status(1)
                .build();
    }
}
