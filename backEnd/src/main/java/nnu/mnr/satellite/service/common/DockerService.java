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
import nnu.mnr.satellite.model.po.common.DFileInfo;
import nnu.mnr.satellite.model.po.common.SftpConn;
import nnu.mnr.satellite.service.websocket.ModelSocketService;
import nnu.mnr.satellite.utils.docker.DockerFileUtil;
import nnu.mnr.satellite.websocket.ModelResultCallBack;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
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

    private int startPort = 55000;

    @Value("${docker.localPath}")
    private String localVolumePath;

    @Value("${docker.defaultServer.host}")
    private String defaultHost;

    @Value("${docker.defaultServer.port}")
    private int defaultPort;

    @Value("${ca.dir}")
    private String localCADir;

    @Value("${docker.remotePath}")
    private String containerDir; //容器工作空间

    @Autowired
    SftpDataService ftpDataService;

    @Autowired
    ModelSocketService modelSocketService;

    private DockerClient dockerClient;

    @PostConstruct
    public void init() {
        dockerClient = createDockerClient();
    }

    private DockerClient createDockerClient() {

        String dockerServerIp = defaultHost;        // Docker Server IP
        int dockerServerPort = defaultPort;         // Docker Server TLS Port

        // 构建 Docker 客户端配置
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("tcp://" + dockerServerIp + ":" + dockerServerPort)  // Docker Server Address
                .withDockerTlsVerify(true)   // Use TLS Certification
                .withDockerCertPath(localCADir)  // ca Dir
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

    public void initEnv(String projectId) {
        // TODO: create env info from dbitem
        String volumePath = "/home/vge/satellite/" + projectId;
        SftpConn sftpInfo = SftpConn.builder().host("223.2.35.208").username("vge").password("3J44.njnu.edu.cn").BuildSftpConn();

        try {
            String localPath = localVolumePath + projectId;
            File folder = new File(localPath);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            Path mainPath = Paths.get(localPath + "/main.py");
            Files.newOutputStream(mainPath).close();

            // creat folder for project docker
            ftpDataService.createRemoteDirAndFile(sftpInfo, localPath, volumePath);
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

        CreateContainerResponse container = dockerClient.createContainerCmd(imageName)
                .withName(containerName)
                .withStdinOpen(true)
                .withTty(true)
                .withWorkingDir(workDir)
                .withExposedPorts(ExposedPort.tcp(startPort))
                .withVolumes(volumeList)
                .withBinds(bindList)
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

    public void startContainer(String containerId) throws DockerException, InterruptedException {
        dockerClient.startContainerCmd(containerId).exec();
    }

    public void stopContainer(String containerId) throws DockerException, InterruptedException {
        dockerClient.stopContainerCmd(containerId).exec();
    }

    public void removeContainer(String containerId) throws DockerException, InterruptedException {
        RemoveContainerCmd removeContainerCmd = dockerClient.removeContainerCmd(containerId);
        removeContainerCmd.withForce(true).exec();
    }

    public List<DFileInfo> getCurDirFiles(String containerId, String projectId, String path, List<DFileInfo> fileTree) {
        // TODO: 通过projectId获取运行路径
        String workSpaceDir = "/usr/local/coding/";
        try {
            String[] cmd = {"/bin/bash", "-c", "ls -la " + workSpaceDir + "/" + path};
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
                if (fileDetails.length >= 9) {
                    String fileName = fileDetails[8];
                    if (fileName.equals(".") || fileName.equals("..")) {
                        continue;
                    }

                    // Build FileInfo
                    String filePath = path + fileName;
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

                    DFileInfo fileInfo = DFileInfo.builder()
                            .fileName(fileName).filePath(filePath).fileType(fileType)
                            .serverPath(containerDir + projectId + "/" + filePath)
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

    public void runCMDInContainer(String projectId, String containerId, String command) {
        try{
            String[] cmd = {"/bin/bash", "-c", command};
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .withTty(true)
                    .withAttachStdin(true)
                    .withCmd(cmd)
                    .exec();

            ModelResultCallBack callback = new ModelResultCallBack(projectId, modelSocketService);
            String exeId = execCreateCmdResponse.getId();
            dockerClient.execStartCmd(exeId).exec(callback).awaitCompletion(300, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
