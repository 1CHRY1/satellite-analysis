package nnu.mnr.satellite.service.common;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.RemoveContainerCmd;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.github.dockerjava.okhttp.OkDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.enums.common.FileType;
import nnu.mnr.satellite.model.pojo.common.DFileInfo;
import nnu.mnr.satellite.model.pojo.common.SftpConn;
import nnu.mnr.satellite.model.pojo.modeling.DockerServerProperties;
import nnu.mnr.satellite.nettywebsocket.ModelResultCallBack;
import nnu.mnr.satellite.service.websocket.ModelSocketService;
import nnu.mnr.satellite.utils.docker.DockerFileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/16 10:20
 * @Description:
 */

@Service
@Slf4j
public class DockerService {

    @Autowired
    DockerServerProperties dockerServerProperties;

    private int startPort = 55000;

//    @Value("${ca.dir}")
//    private String localCADir;

    @Autowired
    SftpDataService sftpDataService;

    @Autowired
    ModelSocketService modelSocketService;

    private DockerClient dockerClient;

//    private String localPath;
    private String serverDir;
    private String workDir;
    private String defaultHost;
    private int defaultPort;
    private String defaultUser;
    private String defaultPassword;

    @PostConstruct
    public void init() {
//        this.localPath = dockerServerProperties.getLocalPath();
        this.serverDir = dockerServerProperties.getServerDir();
        this.workDir = dockerServerProperties.getWorkDir();
        this.defaultHost = dockerServerProperties.getDefaultServer().get("host");
        this.defaultPort = Integer.parseInt(dockerServerProperties.getDefaultServer().get("port"));
        this.defaultUser = dockerServerProperties.getDefaultServer().get("username");
        this.defaultPassword = dockerServerProperties.getDefaultServer().get("password");
        dockerClient = createDockerClient();
    }

    private DockerClient createDockerClient() {

        String dockerServerIp = defaultHost;        // Docker Server IP
        int dockerServerPort = defaultPort;         // Docker Server TLS Port

        // 构建 Docker 客户端配置
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("tcp://" + dockerServerIp + ":" + dockerServerPort)  // Docker Server Address
//                .withDockerTlsVerify(true)   // Use TLS Certification
//                .withDockerCertPath(localCADir)  // ca Dir
                .withApiVersion("1.45")
                .build();

        // 配置 OkHttp 作为 HTTP 客户端
        DockerHttpClient httpClient = new OkDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig()) // 配置 TLS
                .build();

        // 使用配置好的 HTTP 客户端构建 DockerClient
        return DockerClientBuilder.getInstance(config)
                .withDockerHttpClient(httpClient)
                .build();
    }

    public void initEnv(String serverDir) {
        SftpConn sftpInfo = SftpConn.builder()
                .host(defaultHost).username(defaultUser).password(defaultPassword)
                .BuildSftpConn();
        try {
            // create folder for project docker
            sftpDataService.createRemoteDirAndFile(sftpInfo, serverDir);
        } catch (Exception e) {
            log.error("Error Initing Environment " + e);
        }
    }

    public String createContainer(String imageName, String containerName, String volumePath, String workDir) {
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

        CreateContainerResponse container = dockerClient.createContainerCmd(imageName)
                .withName(containerName)
                .withStdinOpen(true)
                .withTty(true)
                .withWorkingDir(workDir)
                .withExposedPorts(ExposedPort.tcp(startPort))
                .withVolumes(volumeList)
                .withBinds(bindList)
                .withEnv(envList)
                .exec();
        String containerId = container.getId();
        ++ startPort;
        return containerId;
    }

    public boolean checkContainerState(String containerId) throws Exception {
        boolean ifrunning=false;
        List<Container> containers = dockerClient.listContainersCmd().exec();
        for (Container container : containers) {
            if (container.getId().equals(containerId)) {
                ifrunning=true;
            }
        }
        return ifrunning;
    }

    public void startContainer(String containerId) throws DockerException {
        dockerClient.startContainerCmd(containerId).exec();
    }

    public void stopContainer(String containerId) throws DockerException, InterruptedException {
        dockerClient.stopContainerCmd(containerId).exec();
    }

    public void removeContainer(String containerId) throws DockerException, InterruptedException {
        RemoveContainerCmd removeContainerCmd = dockerClient.removeContainerCmd(containerId);
        removeContainerCmd.withForce(true).exec();
    }

    public List<DFileInfo> getCurDirFiles(String projectId, String containerId, String path, List<DFileInfo> fileTree) {
        // TODO: 通过projectId获取运行路径
        String workSpaceDir = workDir;
        try {
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

                    // Build FileInfo
                    String filePath = path + "/" + fileName;
                    Boolean isDirectory = file.startsWith("d");
                    FileType fileType = FileType.unknown;
                    if (isDirectory){
                        fileType = FileType.folder;
                        filePath = filePath + "/";
                    } else{
                        int dotIndex = fileName.lastIndexOf('.');
                        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
                            String type = fileName.substring(dotIndex + 1).toLowerCase();
                            fileType = DockerFileUtil.setDataType(type);
                        }
                    }

                    // TODO
                    DFileInfo fileInfo = DFileInfo.builder()
                            .fileName(fileName).filePath(filePath).fileType(fileType)
                            .serverPath(serverDir + projectId + "/" + filePath)
                            .updateTime(DockerFileUtil.parseFileLastModified(fileDetails))
                            .fileSize(Long.parseLong(fileDetails[4]))
                            .DFileInfoBuilder();

                    if (isDirectory) {
                        List<DFileInfo> subFiles = new ArrayList<>();
                        subFiles = getCurDirFiles(containerId, projectId, filePath, subFiles);
                        fileInfo.setChildrenFileList(subFiles);
                    }

                    fileTree.add(fileInfo);

                }
            }
            return fileTree;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void runCMDInContainer(String userId, String projectId, String containerId, String command) {
        try{
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
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void runCMDInContainerWithoutInteraction(String userId, String projectId, String containerId, String command) {
        try{
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
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 在容器中执行命令并获取输出
     * @param containerId 容器ID
     * @param command 命令
     * @return 命令输出
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
            
            return outputStream.toString();
        } catch (InterruptedException e) {
            log.error("Command execution interrupted: {}", e.getMessage());
            throw new RuntimeException("Command execution failed", e);
        }
    }

    /**
     * 静默执行命令（不抛出异常）
     * @param containerId 容器ID
     * @param command 命令
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

}
