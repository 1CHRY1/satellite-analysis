package nnu.mnr.satellite.service.common;

import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.model.pojo.modeling.DockerServerProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.stream.Stream;

/**
 * 本地文件服务
 * 用于本地Docker模式下的文件操作，替代SftpDataService的SFTP操作
 *
 * @Author: Assistant
 * @Date: 2025/7/9
 * @Description: 本地文件系统操作服务
 */
@Service
@Slf4j
public class LocalFileService {

    @Autowired
    DockerServerProperties dockerServerProperties;

    /**
     * 读取本地文件内容
     *
     * @param filePath 文件路径
     * @return 文件内容
     */
    public String readLocalFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                log.warn("File not found: {}", filePath);
                return "";
            }
            
            byte[] content = Files.readAllBytes(path);
            return new String(content, "UTF-8");
        } catch (IOException e) {
            log.error("Failed to read local file: {}", filePath, e);
            throw new RuntimeException("Read operation failed", e);
        }
    }

    /**
     * 写入本地文件
     *
     * @param filePath 文件路径
     * @param content  文件内容
     */
    public void writeLocalFile(String filePath, String content) {
        try {
            Path path = Paths.get(filePath);
            
            // 确保父目录存在
            Path parentDir = path.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            
            Files.write(path, content.getBytes("UTF-8"));
            log.info("File written successfully: {}", filePath);
        } catch (IOException e) {
            log.error("Failed to write local file: {}", filePath, e);
            throw new RuntimeException("Write operation failed", e);
        }
    }

    /**
     * 创建本地目录
     *
     * @param dirPath     目录路径
     * @param permissions 权限（在Windows上可能被忽略）
     */
    public void createLocalDir(String dirPath, int permissions) {
        try {
            Path path = Paths.get(dirPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                
                // 尝试设置权限（在Unix/Linux系统上）
                if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
                    try {
                        Files.setPosixFilePermissions(path, 
                            PosixFilePermissions.fromString(String.format("%03o", permissions)));
                    } catch (Exception e) {
                        log.warn("Failed to set permissions for directory: {}", dirPath, e);
                    }
                }
                
                log.info("Directory created successfully: {}", dirPath);
            } else {
                log.info("Directory already exists: {}", dirPath);
            }
        } catch (IOException e) {
            log.error("Failed to create directory: {}", dirPath, e);
            throw new RuntimeException("Failed to create directory: " + dirPath, e);
        }
    }

    /**
     * 从输入流上传文件到本地
     *
     * @param inputStream 输入流
     * @param filePath    目标文件路径
     */
    public void uploadFileFromStream(InputStream inputStream, String filePath) {
        try {
            Path path = Paths.get(filePath);
            
            // 确保父目录存在
            Path parentDir = path.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            
            Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
            log.info("File uploaded successfully from stream to: {}", filePath);
        } catch (IOException e) {
            log.error("Failed to upload file from stream to: {}", filePath, e);
            throw new RuntimeException("Upload from stream operation failed", e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    log.error("Failed to close input stream", e);
                }
            }
        }
    }

    /**
     * 上传文件（复制文件）
     *
     * @param sourceFilePath 源文件路径
     * @param targetFilePath 目标文件路径
     */
    public void uploadFile(String sourceFilePath, String targetFilePath) {
        try {
            Path sourcePath = Paths.get(sourceFilePath);
            Path targetPath = Paths.get(targetFilePath);
            
            // 确保目标目录存在
            Path targetDir = targetPath.getParent();
            if (targetDir != null && !Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }
            
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("File uploaded successfully from {} to {}", sourceFilePath, targetFilePath);
        } catch (IOException e) {
            log.error("Failed to upload file from {} to {}", sourceFilePath, targetFilePath, e);
            throw new RuntimeException("Upload operation failed", e);
        }
    }

    /**
     * 删除文件夹及其内容
     *
     * @param folderPath 文件夹路径
     */
    public void deleteFolder(String folderPath) {
        try {
            Path path = Paths.get(folderPath);
            if (Files.exists(path)) {
                deleteFolderRecursive(path);
                log.info("Folder deleted successfully: {}", folderPath);
            } else {
                log.info("Folder does not exist: {}", folderPath);
            }
        } catch (IOException e) {
            log.error("Failed to delete folder: {}", folderPath, e);
            throw new RuntimeException("Delete folder operation failed", e);
        }
    }

    /**
     * 删除文件夹内容但保留文件夹
     *
     * @param folderPath 文件夹路径
     */
    public void deleteFolderContents(String folderPath) {
        try {
            Path path = Paths.get(folderPath);
            if (Files.exists(path) && Files.isDirectory(path)) {
                try (Stream<Path> stream = Files.list(path)) {
                    stream.forEach(this::deleteRecursive);
                }
                log.info("Folder contents deleted successfully: {}", folderPath);
            } else {
                log.info("Folder does not exist or is not a directory: {}", folderPath);
            }
        } catch (IOException e) {
            log.error("Failed to delete folder contents: {}", folderPath, e);
            throw new RuntimeException("Delete folder contents operation failed", e);
        }
    }

    /**
     * 递归删除文件夹
     *
     * @param path 路径
     * @throws IOException IO异常
     */
    private void deleteFolderRecursive(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (Stream<Path> stream = Files.list(path)) {
                stream.forEach(this::deleteRecursive);
            }
        }
        Files.delete(path);
    }

    /**
     * 递归删除文件或目录
     *
     * @param path 路径
     */
    private void deleteRecursive(Path path) {
        try {
            if (Files.isDirectory(path)) {
                try (Stream<Path> stream = Files.list(path)) {
                    stream.forEach(this::deleteRecursive);
                }
            }
            Files.delete(path);
        } catch (IOException e) {
            log.error("Failed to delete: {}", path, e);
        }
    }

    /**
     * 递归上传目录
     *
     * @param sourceDir 源目录
     * @param targetDir 目标目录
     */
    private void uploadDirectory(String sourceDir, String targetDir) throws IOException {
        Path sourcePath = Paths.get(sourceDir);
        Path targetPath = Paths.get(targetDir);
        
        if (!Files.exists(sourcePath)) {
            log.warn("Source directory does not exist: {}", sourceDir);
            return;
        }
        
        // 确保目标目录存在
        if (!Files.exists(targetPath)) {
            Files.createDirectories(targetPath);
        }
        
        try (Stream<Path> stream = Files.walk(sourcePath)) {
            stream.forEach(source -> {
                try {
                    Path destination = targetPath.resolve(sourcePath.relativize(source));
                    if (Files.isDirectory(source)) {
                        if (!Files.exists(destination)) {
                            Files.createDirectories(destination);
                        }
                    } else {
                        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    log.error("Failed to copy {} to {}", source, targetPath, e);
                }
            });
        }
    }

    /**
     * 创建项目目录和文件结构
     *
     * @param projectPath 项目路径
     */
    public void createProjectDirAndFile(String projectPath) {
        try {
            // 创建主工作目录
            createLocalDir(projectPath, 0777);
            
            // 创建数据目录
            String dataPath = projectPath + "/data/";
            createLocalDir(dataPath, 0777);
            
            // 创建输出目录
            String outputPath = projectPath + "/output/";
            createLocalDir(outputPath, 0777);
            
            // 上传 ogms_xfer 目录
            String packagePath = projectPath + "/ogms_xfer/";
            String sourcePackagePath = dockerServerProperties.getLocalPath() + "/devCli/ogms_xfer";
            uploadDirectory(sourcePackagePath, packagePath);
            
            // 上传 main.py 文件
            String mainSourcePath = dockerServerProperties.getLocalPath() + "/devCli/main.py";
            String mainTargetPath = projectPath + "/main.py";
            uploadFile(mainSourcePath, mainTargetPath);
            
            // 上传 watcher.py 文件
            String watcherSourcePath = dockerServerProperties.getLocalPath() + "/devCli/watcher.py";
            String watcherTargetPath = projectPath + "/watcher.py";
            uploadFile(watcherSourcePath, watcherTargetPath);
            
            // 上传 ray_optimization 目录
            String rayOptimizationPath = projectPath + "/ray_optimization/";
            String sourceRayOptimizationPath = dockerServerProperties.getLocalPath() + "/devCli/ray_optimization";
            uploadDirectory(sourceRayOptimizationPath, rayOptimizationPath);
            
            log.info("Project directory and files created successfully: {}", projectPath);
        } catch (IOException e) {
            log.error("Failed to create project directory and files for path: {}", projectPath, e);
            throw new RuntimeException("Create project operation failed", e);
        }
    }

    /**
     * 检查文件是否存在
     *
     * @param filePath 文件路径
     * @return 文件是否存在
     */
    public boolean fileExists(String filePath) {
        return Files.exists(Paths.get(filePath));
    }

    /**
     * 检查目录是否存在
     *
     * @param dirPath 目录路径
     * @return 目录是否存在
     */
    public boolean dirExists(String dirPath) {
        Path path = Paths.get(dirPath);
        return Files.exists(path) && Files.isDirectory(path);
    }

    /**
     * 获取文件大小
     *
     * @param filePath 文件路径
     * @return 文件大小（字节）
     */
    public long getFileSize(String filePath) {
        try {
            return Files.size(Paths.get(filePath));
        } catch (IOException e) {
            log.error("Failed to get file size: {}", filePath, e);
            return 0;
        }
    }
} 