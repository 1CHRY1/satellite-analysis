package nnu.mnr.satellite.service.common;

import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.config.DockerModeConfig;
import nnu.mnr.satellite.model.pojo.common.DFileInfo;
import nnu.mnr.satellite.model.pojo.common.SftpConn;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

/**
 * 统一Docker服务适配器
 * 根据配置选择使用本地或远程Docker服务
 *
 * @Author: Assistant
 * @Date: 2025/7/9
 * @Description: 统一的Docker和文件服务适配器
 */
@Service
@Slf4j
public class UnifiedDockerService {

    @Autowired
    private DockerModeConfig dockerModeConfig;

    @Autowired
    private DockerService dockerService;

    @Autowired
    private LocalDockerService localDockerService;

    @Autowired
    private SftpDataService sftpDataService;

    @Autowired
    private LocalFileService localFileService;

    /**
     * 初始化环境
     */
    public void initEnv(String projectPath) {
        if (dockerModeConfig.isLocalMode()) {
            localDockerService.initEnv(projectPath);
        } else {
            dockerService.initEnv(projectPath);
        }
    }

    /**
     * 创建容器
     */
    public String createContainer(String imageName, String containerName, String volumePath, String workDir) {
        if (dockerModeConfig.isLocalMode()) {
            return localDockerService.createContainer(imageName, containerName, volumePath, workDir);
        } else {
            return dockerService.createContainer(imageName, containerName, volumePath, workDir);
        }
    }

    /**
     * 检查容器状态
     */
    public boolean checkContainerState(String containerId) {
        try {
            if (dockerModeConfig.isLocalMode()) {
                return localDockerService.checkContainerState(containerId);
            } else {
                return dockerService.checkContainerState(containerId);
            }
        } catch (Exception e) {
            log.error("Failed to check container state", e);
            return false;
        }
    }

    /**
     * 启动容器
     */
    public void startContainer(String containerId) {
        if (dockerModeConfig.isLocalMode()) {
            localDockerService.startContainer(containerId);
        } else {
            dockerService.startContainer(containerId);
        }
    }

    /**
     * 停止容器
     */
    public void stopContainer(String containerId) {
        try {
            if (dockerModeConfig.isLocalMode()) {
                localDockerService.stopContainer(containerId);
            } else {
                dockerService.stopContainer(containerId);
            }
        } catch (Exception e) {
            log.error("Failed to stop container", e);
        }
    }

    /**
     * 删除容器
     */
    public void removeContainer(String containerId) {
        try {
            if (dockerModeConfig.isLocalMode()) {
                localDockerService.removeContainer(containerId);
            } else {
                dockerService.removeContainer(containerId);
            }
        } catch (Exception e) {
            log.error("Failed to remove container", e);
        }
    }

    /**
     * 获取目录文件
     */
    public List<DFileInfo> getCurDirFiles(String projectId, String containerId, String path, List<DFileInfo> fileTree) {
        if (dockerModeConfig.isLocalMode()) {
            return localDockerService.getCurDirFiles(projectId, containerId, path, fileTree);
        } else {
            return dockerService.getCurDirFiles(projectId, containerId, path, fileTree);
        }
    }

    /**
     * 在容器中运行命令
     */
    public void runCMDInContainer(String userId, String projectId, String containerId, String command) {
        if (dockerModeConfig.isLocalMode()) {
            localDockerService.runCMDInContainer(userId, projectId, containerId, command);
        } else {
            dockerService.runCMDInContainer(userId, projectId, containerId, command);
        }
    }

    /**
     * 在容器中运行命令（无交互）
     */
    public void runCMDInContainerWithoutInteraction(String userId, String projectId, String containerId, String command) {
        if (dockerModeConfig.isLocalMode()) {
            localDockerService.runCMDInContainerWithoutInteraction(userId, projectId, containerId, command);
        } else {
            dockerService.runCMDInContainerWithoutInteraction(userId, projectId, containerId, command);
        }
    }

    /**
     * 在容器中运行命令并获取输出
     */
    public String runCMDInContainerAndGetOutput(String containerId, String command) {
        if (dockerModeConfig.isLocalMode()) {
            return localDockerService.runCMDInContainerAndGetOutput(containerId, command);
        } else {
            return dockerService.runCMDInContainerAndGetOutput(containerId, command);
        }
    }

    /**
     * 静默运行命令
     */
    public void runCMDInContainerSilent(String containerId, String command) {
        if (dockerModeConfig.isLocalMode()) {
            localDockerService.runCMDInContainerSilent(containerId, command);
        } else {
            dockerService.runCMDInContainerSilent(containerId, command);
        }
    }

    /**
     * 读取文件
     */
    public String readFile(String filePath) {
        if (dockerModeConfig.isLocalMode()) {
            return localFileService.readLocalFile(filePath);
        } else {
            return sftpDataService.readRemoteFile(filePath);
        }
    }

    /**
     * 写入文件
     */
    public void writeFile(String filePath, String content) {
        if (dockerModeConfig.isLocalMode()) {
            localFileService.writeLocalFile(filePath, content);
        } else {
            sftpDataService.writeRemoteFile(filePath, content);
        }
    }

    /**
     * 创建目录
     */
    public void createDir(String dirPath, int permissions) {
        if (dockerModeConfig.isLocalMode()) {
            localFileService.createLocalDir(dirPath, permissions);
        } else {
            sftpDataService.createRemoteDir(dirPath, permissions);
        }
    }

    /**
     * 从流上传文件
     */
    public void uploadFileFromStream(InputStream inputStream, String filePath) {
        if (dockerModeConfig.isLocalMode()) {
            localFileService.uploadFileFromStream(inputStream, filePath);
        } else {
            sftpDataService.uploadFileFromStream(inputStream, filePath);
        }
    }

    /**
     * 上传文件
     */
    public void uploadFile(String sourceFilePath, String targetFilePath) {
        if (dockerModeConfig.isLocalMode()) {
            localFileService.uploadFile(sourceFilePath, targetFilePath);
        } else {
            sftpDataService.uploadFile(sourceFilePath, targetFilePath);
        }
    }

    /**
     * 删除文件夹
     */
    public void deleteFolder(String folderPath) {
        if (dockerModeConfig.isLocalMode()) {
            localFileService.deleteFolder(folderPath);
        } else {
            sftpDataService.deleteFolder(folderPath);
        }
    }

    /**
     * 删除文件夹内容
     */
    public void deleteFolderContents(String folderPath) {
        if (dockerModeConfig.isLocalMode()) {
            localFileService.deleteFolderContents(folderPath);
        } else {
            sftpDataService.deleteFolderContents(folderPath);
        }
    }

    /**
     * 检查文件是否存在
     */
    public boolean fileExists(String filePath) {
        if (dockerModeConfig.isLocalMode()) {
            return localFileService.fileExists(filePath);
        } else {
            // 远程模式下可以尝试读取文件来检查存在性
            try {
                String content = sftpDataService.readRemoteFile(filePath);
                return content != null;
            } catch (Exception e) {
                return false;
            }
        }
    }

    /**
     * 获取服务器路径
     */
    public String getServerPath() {
        if (dockerModeConfig.isLocalMode()) {
            return dockerModeConfig.getLocal().getLocalPath();
        } else {
            return dockerModeConfig.getServerDir();
        }
    }

    /**
     * 获取工作目录
     */
    public String getWorkDir() {
        if (dockerModeConfig.isLocalMode()) {
            return dockerModeConfig.getLocal().getWorkDir();
        } else {
            return dockerModeConfig.getWorkDir();
        }
    }

    /**
     * 创建项目目录和文件
     */
    public void createProjectDirAndFile(String projectPath) {
        if (dockerModeConfig.isLocalMode()) {
            localFileService.createProjectDirAndFile(projectPath);
        } else {
            // 远程模式下，使用现有的逻辑
            SftpConn sftpConn = SftpConn.builder()
                    .host(dockerModeConfig.getDefaultServer().getHost())
                    .username(dockerModeConfig.getDefaultServer().getUsername())
                    .password(dockerModeConfig.getDefaultServer().getPassword())
                    .BuildSftpConn();
            sftpDataService.createRemoteDirAndFile(sftpConn, projectPath);
        }
    }

    /**
     * 获取当前模式
     */
    public String getCurrentMode() {
        return dockerModeConfig.getMode();
    }

    /**
     * 是否为本地模式
     */
    public boolean isLocalMode() {
        return dockerModeConfig.isLocalMode();
    }

    /**
     * 是否为远程模式
     */
    public boolean isRemoteMode() {
        return dockerModeConfig.isRemoteMode();
    }
} 