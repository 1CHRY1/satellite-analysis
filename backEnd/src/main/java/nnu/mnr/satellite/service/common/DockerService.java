package nnu.mnr.satellite.service.common;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.RemoveContainerCmd;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.okhttp.OkDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.model.po.common.SftpConn;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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

    @Autowired
    SftpDataService ftpDataService;

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


}
