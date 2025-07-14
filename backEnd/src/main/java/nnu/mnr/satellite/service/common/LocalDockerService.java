package nnu.mnr.satellite.service.common;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.RemoveContainerCmd;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ulimit;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.github.dockerjava.okhttp.OkDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.config.optimization.RayOptimizationProperties;
import nnu.mnr.satellite.enums.common.FileType;
import nnu.mnr.satellite.model.pojo.common.DFileInfo;
import nnu.mnr.satellite.nettywebsocket.ModelResultCallBack;
import nnu.mnr.satellite.service.websocket.ModelSocketService;
import nnu.mnr.satellite.utils.docker.DockerFileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 本地Docker服务
 * 用于本地Docker模式下的容器管理操作
 *
 * @Author: Assistant
 * @Date: 2025/7/9
 * @Description: 本地Docker容器管理服务
 */
@Service
@Slf4j
public class LocalDockerService {

    @Value("${docker.local.localPath}")
    private String localPath;

    @Value("${docker.local.workDir}")
    private String workDir;

    @Value("${ray.optimization.dockerRunOptions:}")
    private String dockerRunOptions;

    @Autowired
    private ModelSocketService modelSocketService;

    @Autowired
    private LocalFileService localFileService;

    @Autowired
    private RayOptimizationProperties rayOptimizationProperties;

    private DockerClient dockerClient;
    private int startPort = 55000;

    @PostConstruct
    public void init() {
        dockerClient = createDockerClient();
        log.info("LocalDockerService initialized with localPath: {}, workDir: {}", localPath, workDir);
    }

    /**
     * 创建本地Docker客户端
     */
    private DockerClient createDockerClient() {
        try {
            // 本地Docker配置
            DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                    .withDockerHost("tcp://localhost:2375")  // 本地Docker地址
                    .withApiVersion("1.45")
                    .build();

            // 配置 OkHttp 作为 HTTP 客户端
            DockerHttpClient httpClient = new OkDockerHttpClient.Builder()
                    .dockerHost(config.getDockerHost())
                    .sslConfig(config.getSSLConfig())
                    .build();

            // 构建 DockerClient
            DockerClient client = DockerClientBuilder.getInstance(config)
                    .withDockerHttpClient(httpClient)
                    .build();

            log.info("Local Docker client created successfully");
            return client;
        } catch (Exception e) {
            log.error("Failed to create local Docker client", e);
            throw new RuntimeException("Failed to create local Docker client", e);
        }
    }

    /**
     * 初始化本地环境
     */
    public void initEnv(String projectPath) {
        try {
            // 创建项目目录和文件结构
            localFileService.createProjectDirAndFile(projectPath);
            log.info("Local environment initialized: {}", projectPath);
        } catch (Exception e) {
            log.error("Error initializing local environment: {}", projectPath, e);
            throw new RuntimeException("Failed to initialize local environment", e);
        }
    }

    /**
     * 创建容器
     */
    public String createContainer(String imageName, String containerName, String volumePath, String workDir) {
        try {
            Volume workVolume = new Volume(workDir);
            Bind workBind = new Bind(volumePath, workVolume);
            List<Bind> bindList = new ArrayList<>();
            bindList.add(workBind);
            List<Volume> volumeList = new ArrayList<>();
            volumeList.add(workVolume);

            List<String> envList = new ArrayList<>();
            envList.add("TZ=Asia/Shanghai");
            envList.add("LANG=C.UTF-8");
            envList.add("LC_ALL=C.UTF-8");

            // 创建端口绑定列表
            List<ExposedPort> exposedPorts = new ArrayList<>();
            List<PortBinding> portBindings = new ArrayList<>();
            
            // 基础端口
            exposedPorts.add(ExposedPort.tcp(startPort));
            
            // 如果启用了Ray Dashboard，添加Dashboard端口映射
            if (rayOptimizationProperties.getDashboard().isEnabled()) {
                int dashboardPort = rayOptimizationProperties.getDashboard().getPort();
                int hostPort = rayOptimizationProperties.getDashboard().getHostPort();
                
                exposedPorts.add(ExposedPort.tcp(dashboardPort));
                portBindings.add(PortBinding.parse(hostPort + ":" + dashboardPort));
                
                log.info("Ray Dashboard port mapping enabled: {}:{}", hostPort, dashboardPort);
            }

            // 创建HostConfig来设置共享内存大小、端口绑定和资源限制
            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withBinds(bindList)
                    // 添加严格的资源限制
                    .withMemory(2L * 1024 * 1024 * 1024) // 2GB内存限制
                    .withMemorySwap(2L * 1024 * 1024 * 1024) // 禁用swap，设置相同值
                    .withCpuCount(2L) // 限制CPU核数
                    .withCpuPercent(50L) // 限制CPU使用率50%
                    .withKernelMemory(512L * 1024 * 1024) // 512MB内核内存限制
                    .withPidsLimit(100L) // 限制进程数量
                    .withUlimits(Arrays.asList(
                        new Ulimit("nofile", 1024L, 1024L), // 文件描述符限制
                        new Ulimit("nproc", 100L, 100L),    // 进程数量限制
                        new Ulimit("fsize", 100L * 1024 * 1024, 100L * 1024 * 1024) // 文件大小限制100MB
                    ))
                    .withReadonlyRootfs(false) // 允许写入，但会监控
                    .withNetworkMode("none") // 禁用网络访问以增强安全性
                    .withCapDrop(com.github.dockerjava.api.model.Capability.ALL) // 移除所有capabilities
                    .withCapAdd(com.github.dockerjava.api.model.Capability.CHOWN,
                               com.github.dockerjava.api.model.Capability.SETGID,
                               com.github.dockerjava.api.model.Capability.SETUID) // 只添加必要的capabilities
                    .withSecurityOpts(Arrays.asList("no-new-privileges")); // 禁止获取新权限

            // 如果有端口绑定，添加到HostConfig
            if (!portBindings.isEmpty()) {
                hostConfig.withPortBindings(portBindings);
                // 如果需要端口绑定，允许网络访问但限制为bridge模式
                hostConfig.withNetworkMode("bridge");
            }

            // 如果有额外的Docker运行选项，添加共享内存大小
            if (dockerRunOptions != null && dockerRunOptions.contains("--shm-size")) {
                String shmSize = extractShmSize(dockerRunOptions);
                if (shmSize != null) {
                    hostConfig.withShmSize(parseShmSize(shmSize));
                }
            } else {
                // 默认设置较小的共享内存大小
                hostConfig.withShmSize(128L * 1024 * 1024); // 128MB
            }

            // 构建容器创建命令
            CreateContainerResponse container = dockerClient.createContainerCmd(imageName)
                    .withName(containerName)
                    .withStdinOpen(true)
                    .withTty(true)
                    .withWorkingDir(workDir)
                    .withExposedPorts(exposedPorts.toArray(new ExposedPort[0]))
                    .withVolumes(volumeList)
                    .withEnv(envList)
                    .withHostConfig(hostConfig)
                    .exec();
            String containerId = container.getId();
            startPort++;
            
            log.info("Container created successfully: {} ({})", containerName, containerId);
            return containerId;
        } catch (Exception e) {
            log.error("Failed to create container: {}", containerName, e);
            throw new RuntimeException("Failed to create container", e);
        }
    }

    /**
     * 从Docker运行选项中提取shm-size参数
     */
    private String extractShmSize(String dockerRunOptions) {
        String[] options = dockerRunOptions.split("\\s+");
        for (int i = 0; i < options.length; i++) {
            if ("--shm-size".equals(options[i]) && i + 1 < options.length) {
                return options[i + 1];
            } else if (options[i].startsWith("--shm-size=")) {
                return options[i].substring("--shm-size=".length());
            }
        }
        return null;
    }

    /**
     * 解析shm-size参数为字节数
     */
    private long parseShmSize(String shmSize) {
        try {
            String size = shmSize.toLowerCase();
            if (size.endsWith("gb")) {
                return (long) (Double.parseDouble(size.substring(0, size.length() - 2)) * 1024 * 1024 * 1024);
            } else if (size.endsWith("mb")) {
                return (long) (Double.parseDouble(size.substring(0, size.length() - 2)) * 1024 * 1024);
            } else if (size.endsWith("kb")) {
                return (long) (Double.parseDouble(size.substring(0, size.length() - 2)) * 1024);
            } else {
                return Long.parseLong(size);
            }
        } catch (Exception e) {
            log.warn("Failed to parse shm-size: {}, using default", shmSize);
            return 1024 * 1024 * 1024; // 1GB默认值
        }
    }

    /**
     * 检查容器状态
     */
    public boolean checkContainerState(String containerId) {
        try {
            List<Container> containers = dockerClient.listContainersCmd().exec();
            boolean isRunning = containers.stream()
                    .anyMatch(container -> container.getId().equals(containerId));
            log.debug("Container {} state: {}", containerId, isRunning ? "running" : "stopped");
            return isRunning;
        } catch (Exception e) {
            log.error("Failed to check container state: {}", containerId, e);
            return false;
        }
    }

    /**
     * 启动容器
     */
    public void startContainer(String containerId) {
        try {
            dockerClient.startContainerCmd(containerId).exec();
            log.info("Container started successfully: {}", containerId);
        } catch (DockerException e) {
            log.error("Failed to start container: {}", containerId, e);
            throw new RuntimeException("Failed to start container", e);
        }
    }

    /**
     * 停止容器
     */
    public void stopContainer(String containerId) {
        try {
            dockerClient.stopContainerCmd(containerId).exec();
            log.info("Container stopped successfully: {}", containerId);
        } catch (DockerException e) {
            log.error("Failed to stop container: {}", containerId, e);
            throw new RuntimeException("Failed to stop container", e);
        }
    }

    /**
     * 删除容器
     */
    public void removeContainer(String containerId) {
        try {
            RemoveContainerCmd removeContainerCmd = dockerClient.removeContainerCmd(containerId);
            removeContainerCmd.withForce(true).exec();
            log.info("Container removed successfully: {}", containerId);
        } catch (DockerException e) {
            log.error("Failed to remove container: {}", containerId, e);
            throw new RuntimeException("Failed to remove container", e);
        }
    }

    /**
     * 获取当前目录文件
     */
    public List<DFileInfo> getCurDirFiles(String projectId, String containerId, String path, List<DFileInfo> fileTree) {
        try {
            String workSpaceDir = workDir;
            String[] cmd = {"/bin/bash", "-c", "ls -la --time-style='+%Y-%m-%d %H:%M:%S.%N' " + workSpaceDir + path};
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .withTty(true)
                    .withAttachStdin(true)
                    .withCmd(cmd)
                    .exec();

            String execId = execCreateCmdResponse.getId();
            OutputStream output = new ByteArrayOutputStream();
            dockerClient.execStartCmd(execId)
                    .exec(new ExecStartResultCallback(output, System.err))
                    .awaitCompletion();

            String[] files = output.toString().split("\n");
            for (String file : files) {
                String[] fileDetails = file.split("\\s+");
                if (fileDetails.length >= 8) {
                    String fileName = fileDetails[7];
                    if (fileName.equals(".") || fileName.equals("..")) {
                        continue;
                    }

                    // 构建文件信息
                    String filePath = path + "/" + fileName;
                    Boolean isDirectory = file.startsWith("d");
                    FileType fileType = FileType.unknown;
                    if (isDirectory) {
                        fileType = FileType.folder;
                        filePath = filePath + "/";
                    } else {
                        int dotIndex = fileName.lastIndexOf('.');
                        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
                            String type = fileName.substring(dotIndex + 1).toLowerCase();
                            fileType = DockerFileUtil.setDataType(type);
                        }
                    }

                    DFileInfo fileInfo = DFileInfo.builder()
                            .fileName(fileName)
                            .filePath(filePath)
                            .fileType(fileType)
                            .serverPath(localPath + "/" + projectId + "/" + filePath)
                            .updateTime(DockerFileUtil.parseFileLastModified(fileDetails))
                            .fileSize(Long.parseLong(fileDetails[4]))
                            .DFileInfoBuilder();

                    if (isDirectory) {
                        List<DFileInfo> subFiles = new ArrayList<>();
                        subFiles = getCurDirFiles(projectId, containerId, filePath, subFiles);
                        fileInfo.setChildrenFileList(subFiles);
                    }

                    fileTree.add(fileInfo);
                }
            }
            return fileTree;
        } catch (InterruptedException e) {
            log.error("Failed to get directory files: {}", path, e);
            throw new RuntimeException("Failed to get directory files", e);
        }
    }

    /**
     * 在容器中运行命令（带交互）
     */
    public void runCMDInContainer(String userId, String projectId, String containerId, String command) {
        try {
            String[] cmd = {"/bin/bash", "-c", command};
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .withTty(true)
                    .withAttachStdin(true)
                    .withCmd(cmd)
                    .exec();

            ModelResultCallBack callback = new ModelResultCallBack(userId, projectId, modelSocketService);
            String exeId = execCreateCmdResponse.getId();
            dockerClient.execStartCmd(exeId).exec(callback).awaitCompletion(300, TimeUnit.SECONDS);
            log.debug("Command executed in container: {}", command);
        } catch (InterruptedException e) {
            log.error("Command execution interrupted: {}", command, e);
            throw new RuntimeException("Command execution failed", e);
        }
    }

    /**
     * 在容器中运行命令（无交互）
     */
    public void runCMDInContainerWithoutInteraction(String userId, String projectId, String containerId, String command) {
        try {
            String[] cmd = {"/bin/bash", "-c", command};
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .withTty(false)
                    .withAttachStdin(false)
                    .withCmd(cmd)
                    .exec();

            ModelResultCallBack callback = new ModelResultCallBack(userId, projectId, modelSocketService);
            String exeId = execCreateCmdResponse.getId();
            dockerClient.execStartCmd(exeId).withDetach(true).exec(callback).awaitCompletion(300, TimeUnit.SECONDS);
            log.debug("Command executed in container without interaction: {}", command);
        } catch (InterruptedException e) {
            log.error("Command execution interrupted: {}", command, e);
            throw new RuntimeException("Command execution failed", e);
        }
    }

    /**
     * 在容器中执行命令并获取输出
     */
    public String runCMDInContainerAndGetOutput(String containerId, String command) {
        try {
            String[] cmd = {"/bin/bash", "-c", command};
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .withTty(false)
                    .withAttachStdin(false)
                    .withCmd(cmd)
                    .exec();

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            String exeId = execCreateCmdResponse.getId();
            dockerClient.execStartCmd(exeId)
                    .exec(new ExecStartResultCallback(outputStream, outputStream))
                    .awaitCompletion(60, TimeUnit.SECONDS);

            String output = outputStream.toString();
            log.debug("Command output: {}", output);
            return output;
        } catch (InterruptedException e) {
            log.error("Command execution interrupted: {}", command, e);
            throw new RuntimeException("Command execution failed", e);
        }
    }

    /**
     * 静默执行命令（不抛出异常）
     */
    public void runCMDInContainerSilent(String containerId, String command) {
        try {
            String[] cmd = {"/bin/bash", "-c", command};
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                    .withAttachStdout(false)
                    .withAttachStderr(false)
                    .withTty(false)
                    .withAttachStdin(false)
                    .withCmd(cmd)
                    .exec();

            String exeId = execCreateCmdResponse.getId();
            dockerClient.execStartCmd(exeId)
                    .exec(new ExecStartResultCallback())
                    .awaitCompletion(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.debug("Silent command execution failed (ignored): {}", e.getMessage());
        }
    }

    /**
     * 创建并运行临时容器执行命令
     */
    public String createAndRunTempContainer(String imageName, String projectPath, String workDir, String command) {
        String containerName = "temp_" + System.currentTimeMillis();
        String containerId = null;

        try {
            // 创建临时容器
            containerId = createContainer(imageName, containerName, projectPath, workDir);
            
            // 启动容器
            startContainer(containerId);
            
            // 执行命令并获取输出
            String output = runCMDInContainerAndGetOutput(containerId, command);
            
            return output;
        } catch (Exception e) {
            log.error("Failed to create and run temporary container", e);
            throw new RuntimeException("Failed to create and run temporary container", e);
        } finally {
            // 清理临时容器
            if (containerId != null) {
                try {
                    stopContainer(containerId);
                    removeContainer(containerId);
                } catch (Exception e) {
                    log.warn("Failed to cleanup temporary container: {}", containerId, e);
                }
            }
        }
    }

    /**
     * 获取Docker客户端（用于扩展功能）
     */
    public DockerClient getDockerClient() {
        return dockerClient;
    }

    /**
     * 获取本地路径
     */
    public String getLocalPath() {
        return localPath;
    }

    /**
     * 获取工作目录
     */
    public String getWorkDir() {
        return workDir;
    }
} 