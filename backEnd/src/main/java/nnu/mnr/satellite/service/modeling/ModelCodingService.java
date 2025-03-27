package nnu.mnr.satellite.service.modeling;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.dynamic.datasource.creator.DataSourceProperty;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceProperties;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.config.JSchConnectionManager;
import nnu.mnr.satellite.config.MinioConfig;
import nnu.mnr.satellite.model.dto.modeling.*;
import nnu.mnr.satellite.model.po.modeling.Project;
import nnu.mnr.satellite.model.pojo.common.DFileInfo;
import nnu.mnr.satellite.model.pojo.modeling.DockerServerProperties;
import nnu.mnr.satellite.model.vo.modeling.CodingProjectVO;
import nnu.mnr.satellite.repository.modeling.IProjectRepo;
import nnu.mnr.satellite.service.common.DockerService;
import nnu.mnr.satellite.service.common.SftpDataService;
import nnu.mnr.satellite.utils.common.FileUtil;
import nnu.mnr.satellite.utils.common.IdUtil;
import nnu.mnr.satellite.utils.docker.DockerFileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/16 10:22
 * @Description:
 */

@Service
@Slf4j
public class ModelCodingService {

    @Autowired
    SftpDataService sftpDataService;

    @Autowired
    JSchConnectionManager jSchConnectionManager;

    @Autowired
    DockerServerProperties dockerServerProperties;

    @Autowired
    DockerService dockerService;

    @Autowired
    ProjectDataService projectDataService;
    
    @Autowired
    IProjectRepo projectRepo;

    // config info
    @Autowired
    private DynamicDataSourceProperties dynamicDataSourceProperties;

    @Autowired
    private MinioConfig minioProperties;

    public String getRemoteConfig(Project project) {
        JSONObject projectConfig = JSONObject.of(
                "project_id", project.getProjectId(),
                "user_id", project.getCreateUser(),
                "bucket", project.getProjectId());
        JSONObject minioConfig = JSONObject.of(
                "endpoint", minioProperties.getUrl().split("://")[1],
                "access_key", minioProperties.getAccessKey(),
                "secret_key", minioProperties.getSecretKey(),
                "secure", false);
        Map<String, DataSourceProperty> datasources = dynamicDataSourceProperties.getDatasource();
        DataSourceProperty satelliteDs = datasources.get("mysql_satellite");
        DataSourceProperty tileDs = datasources.get("mysql_tile");
        JSONObject databaseConfig = JSONObject.of(
                "endpoint", satelliteDs.getUrl().split("://")[1].split("/")[0],
                "user", satelliteDs.getUsername(),
                "password", satelliteDs.getPassword(),
                "satellite_database", satelliteDs.getUrl().split("://")[1].split("/", 2)[1],
                "tile_database", tileDs.getUrl().split("://")[1].split("/", 2)[1]);
        JSONObject remoteConfig = JSONObject.of("minio", minioConfig, "database", databaseConfig, "project_info", projectConfig);
        return  remoteConfig.toString();
    }

    // Coding Project Services
    public CodingProjectVO createCodingProject(CreateProjectDTO createProjectDTO) throws JSchException, SftpException, IOException {
        String userId = createProjectDTO.getUserId();
        String projectName = createProjectDTO.getProjectName();
        String env = createProjectDTO.getEnvironment();
        String imageEnv = DockerFileUtil.getImageByName(env);
        String projectId = IdUtil.generateProjectId();
        String serverDir = dockerServerProperties.getServerDir() + projectId + "/";
        String workDir = dockerServerProperties.getWorkDir();
        String pyPath = workDir + "main.py";
        String watchPath = workDir + "output/";
        String localPyPath = dockerServerProperties.getLocalPath() + projectId + "/" + "main.py";
        String serverPyPath = serverDir + "main.py";
        Project formerProject = projectDataService.getProjectByUserAndName(userId, projectName);
        String responseInfo = "";
        if ( formerProject != null){
            responseInfo = "Project " + projectId + " has been Registered";
            return CodingProjectVO.builder().status(-1).info(responseInfo).projectId(projectId).build();
        }
        Project project = Project.builder()
                .projectId(projectId).projectName(projectName)
                .environment(env).createTime(LocalDateTime.now())
                .workDir(workDir).serverDir(serverDir).createUser(userId)
                .pyPath(pyPath).watchPath(watchPath).localPyPath(localPyPath).serverPyPath(serverPyPath)
                .build();
        dockerService.initEnv(projectId, serverDir);

        // Load Config
        String configPath = serverDir + "config.json";
        sftpDataService.writeRemoteFile(configPath, getRemoteConfig(project));

        String containerid = dockerService.createContainer(imageEnv, projectId, project.getServerDir(), workDir);
        String pyContent = sftpDataService.readRemoteFile(project.getServerPyPath());
        project.setPyContent(pyContent);
        project.setContainerId(containerid);
        dockerService.startContainer(containerid);

        // Start Watching Process
//        dockerService.runCMDInContainer();
        projectRepo.insert(project);
        responseInfo = "Project " + projectId + " has been Created";
        return CodingProjectVO.builder().status(1).info(responseInfo).projectId(projectId).build();
    }

    public CodingProjectVO openCodingProject(ProjectOperateDTO projectOperateDTO) {
        String userId = projectOperateDTO.getUserId(); String projectId = projectOperateDTO.getProjectId();
        String responseInfo = "";
        if ( !projectDataService.VerifyUserProject(userId, projectId) ) {
            responseInfo = "User " + userId + " Can't Operate Project " + projectId;
            return CodingProjectVO.builder().status(-1).info(responseInfo).projectId(projectId).build();
        }
        Project project = projectDataService.getProjectById(projectId);
        if ( project == null){
            responseInfo = "Project " + projectId + " hasn't been Registered";
            return CodingProjectVO.builder().status(-1).info(responseInfo).projectId(projectId).build();
        }
        // Start Container
        try {
            String containerid;
            if (project.getContainerId() == null){
                String imageEnv = DockerFileUtil.getImageByName(project.getEnvironment());
                containerid = dockerService.createContainer(imageEnv, project.getProjectName(), project.getServerDir(), project.getWorkDir());
                project.setContainerId(containerid);
                projectRepo.insert(project);
                responseInfo = "Project " + projectId + " has been Created and Opened";
                return CodingProjectVO.builder().status(1).info(responseInfo).projectId(projectId).build();
            }
            else {
                containerid=project.getContainerId();
            }
            // Container Running ?
            boolean checkstate = dockerService.checkContainerState(containerid);
            if (!checkstate){
                dockerService.startContainer(containerid);
                responseInfo = "Project " + projectId + " is Opened";
            } else { responseInfo = "Project " + projectId + " has been Opened"; }
            // Start Watching Process
//        dockerService.runCMDInContainer();
            return CodingProjectVO.builder().status(1).info(responseInfo).projectId(projectId).build();
        } catch (Exception e) {
            responseInfo = "Wrong Openning Container " + e.getMessage();
            return CodingProjectVO.builder().status(-1).info(responseInfo).projectId(projectId).build();
        }
    }

    public CodingProjectVO deleteCodingProject(ProjectOperateDTO projectOperateDTO) {
        String userId = projectOperateDTO.getUserId(); String projectId = projectOperateDTO.getProjectId();
        String responseInfo = "";
        if ( !projectDataService.VerifyUserProject(userId, projectId) ) {
            responseInfo = "User " + userId + " Can't Operate Project " + projectId;
            return CodingProjectVO.builder().status(-1).info(responseInfo).projectId(projectId).build();
        }
        Project project = projectDataService.getProjectById(projectId);
        if ( project == null){
            responseInfo = "Project " + projectId + " hasn't been Registered";
            return CodingProjectVO.builder().status(-1).info(responseInfo).projectId(projectId).build();
        }
//        String projectWorkDir = project.getWorkDir();
        if (project.getContainerId() != null){
            String containerId = project.getContainerId();
            CompletableFuture.runAsync( () -> {
                try {
                    dockerService.stopContainer(containerId);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    try {
                        dockerService.removeContainer(containerId);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        // Delete Local Data & Remote Data
        FileUtil.deleteFolder(new File(dockerServerProperties.getLocalPath() + projectId));
        sftpDataService.deleteFolder(project.getServerDir());
        log.info("Successfully deleted folder and its contents: " + project.getServerDir());
        projectRepo.deleteById(projectId);
        responseInfo = "Project " + projectId + " has been Deleted";
        return CodingProjectVO.builder().status(1).info(responseInfo).projectId(projectId).build();
    }

    // Container Services
    public CodingProjectVO closeProjectContainer(ProjectOperateDTO projectOperateDTO) {
        String userId = projectOperateDTO.getUserId(); String projectId = projectOperateDTO.getProjectId();
        String responseInfo = "";
        if ( !projectDataService.VerifyUserProject(userId, projectId) ) {
            responseInfo = "User " + userId + " Can't Operate Project " + projectId;
            return CodingProjectVO.builder().status(-1).info(responseInfo).projectId(projectId).build();
        }
        Project project = projectDataService.getProjectById(projectId);
        if ( project == null){
            responseInfo = "Project " + projectId + " hasn't been Registered";
            return CodingProjectVO.builder().status(-1).info(responseInfo).projectId(projectId).build();
        }
        String containerId = project.getContainerId();
        CompletableFuture.runAsync( () -> {
            try {
                dockerService.stopContainer(containerId);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        responseInfo = "Project " + projectId + " is closing";
        return CodingProjectVO.builder().status(1).info(responseInfo).projectId(projectId).build();

    }

    // File Services
    public String getMainPyOfContainer(ProjectBasicDTO projectBasicDTO) throws JSchException, SftpException, IOException {
        String userId = projectBasicDTO.getUserId(); String projectId = projectBasicDTO.getProjectId();
        String responseInfo = "";
        if ( !projectDataService.VerifyUserProject(userId, projectId) ) {
            responseInfo = "User " + userId + " Can't Operate Project " + projectId;
            return responseInfo;
        }
        Project project = projectDataService.getProjectById(projectId);
        if ( project == null){
            responseInfo = "Project " + projectId + " hasn't been Registered";
            return responseInfo;
        }
        return sftpDataService.readRemoteFile(project.getServerPyPath());
    }

    public List<DFileInfo> getProjectCurDirFiles(ProjectFileDTO projectFileDTO) {
        String projectId = projectFileDTO.getProjectId(); String userId = projectFileDTO.getUserId();
        if ( !projectDataService.VerifyUserProject(userId, projectId) ) {
            return null;
        }
        String path = projectFileDTO.getPath();
        Project project = projectDataService.getProjectById(projectId);
        if ( project == null){
            return null;
        }
        String containerId = project.getContainerId();
        List<DFileInfo> fileTree = new ArrayList<>();
        return dockerService.getCurDirFiles(projectId, containerId, path, fileTree);
    }

    public CodingProjectVO newProjectFolder(ProjectFileDTO projectFileDTO) {
        String userId = projectFileDTO.getUserId(); String projectId = projectFileDTO.getProjectId();
        String responseInfo = "";
        if ( !projectDataService.VerifyUserProject(userId, projectId) ) {
            responseInfo = "User " + userId + " Can't Operate Project " + projectId;
            return CodingProjectVO.builder().status(-1).info(responseInfo).projectId(projectId).build();
        }
        String path = projectFileDTO.getPath();
        if ( !path.endsWith("/") ) {
            responseInfo = "Path must end with '/' ";
            return CodingProjectVO.builder().status(-1).info(responseInfo).projectId(projectId).build();

        }
        String folderName = projectFileDTO.getName();
        Project project = projectDataService.getProjectById(projectId);
        if ( project == null){
            responseInfo = "Project " + projectId + " hasn't been Registered";
            return CodingProjectVO.builder().status(-1).info(responseInfo).projectId(projectId).build();
        }
        String containerId = project.getContainerId();
        String storagePath = path.equals("") ? folderName : path + folderName;
        storagePath = project.getWorkDir() + storagePath;
        String command = "mkdir -p " + storagePath + " && chmod 777 " + storagePath;
        dockerService.runCMDInContainer(userId, projectId, containerId, command);
        responseInfo = "New Folder " + folderName + " has been Created";
        return CodingProjectVO.builder().status(1).info(responseInfo).projectId(projectId).build();
    }

    public CodingProjectVO deleteProjectFile(ProjectFileDTO projectFileDTO) {
        String userId = projectFileDTO.getUserId(); String projectId = projectFileDTO.getProjectId();
        String responseInfo = "";
        if ( !projectDataService.VerifyUserProject(userId, projectId) ) {
            responseInfo = "User " + userId + " Can't Operate Project " + projectId;
            return CodingProjectVO.builder().status(-1).info(responseInfo).projectId(projectId).build();
        }
        String path = projectFileDTO.getPath();
        String fileName = projectFileDTO.getName();
        Project project = projectDataService.getProjectById(projectId);
        if ( project == null){
            responseInfo = "Project " + projectId + " hasn't been Registered";
            return CodingProjectVO.builder().status(-1).info(responseInfo).projectId(projectId).build();
        }
        String containerId = project.getContainerId();
        StringBuilder cmdBuilder = new StringBuilder("rm -rf");
        cmdBuilder.append(" ").append(project.getWorkDir() + path + fileName);
        dockerService.runCMDInContainer(userId, projectId, containerId, cmdBuilder.toString());
        responseInfo = "File " + fileName + " has been Deleted";
        return CodingProjectVO.builder().status(1).info(responseInfo).projectId(projectId).build();
    }

    public CodingProjectVO saveProjectCode(ProjectFileDTO projectFileDTO) {
        String projectId = projectFileDTO.getProjectId(); String userId = projectFileDTO.getUserId();
        String responseInfo = "";
        if ( !projectDataService.VerifyUserProject(userId, projectId) ) {
            responseInfo = "User " + userId + " Can't Operate Project " + projectId;
            return CodingProjectVO.builder().status(-1).info(responseInfo).projectId(projectId).build();
        }
        Project project = projectDataService.getProjectById(projectId);
        if ( project == null){
            responseInfo = "Project " + projectId + " hasn't been Registered";
            return CodingProjectVO.builder().status(-1).info(responseInfo).projectId(projectId).build();
        }
        try {
            Files.writeString(Paths.get(project.getLocalPyPath()), projectFileDTO.getContent());
            project.setPyContent(projectFileDTO.getContent());
            projectRepo.updateById(project);
            sftpDataService.uploadFile(
                    project.getLocalPyPath(), project.getServerPyPath()
            );
            responseInfo = "Python Script " + projectId + " has been Saved";
            return CodingProjectVO.builder().status(1).info(responseInfo).projectId(projectId).build();
        } catch (Exception e) {
            responseInfo = "Python Script " + projectId + " hasn't been Saved Because of: " + e.getMessage();
            return CodingProjectVO.builder().status(-1).info(responseInfo).projectId(projectId).build();
        }
    }

    // Operating Services
    public CodingProjectVO runScript(ProjectBasicDTO projectBasicDTO) {
        String userId = projectBasicDTO.getUserId(); String projectId = projectBasicDTO.getProjectId();
        String responseInfo = "";
        if ( !projectDataService.VerifyUserProject(userId, projectId) ) {
            responseInfo = "User " + userId + " Can't Operate Project " + projectId;
            return CodingProjectVO.builder().status(-1).info(responseInfo).projectId(projectId).build();
        }
        Project project = projectDataService.getProjectById(projectId);
        if ( project == null){
            responseInfo = "Project " + projectId + " hasn't been Registered";
            return CodingProjectVO.builder().status(-1).info(responseInfo).projectId(projectId).build();
        }
        String containerId = project.getContainerId();
        try {
            if (!dockerService.checkContainerState(containerId)){
                responseInfo = "Container Not Activated";
                return CodingProjectVO.builder().status(-1).info(responseInfo).projectId(projectId).build();
            }
        } catch (Exception e) {
            responseInfo = "Container Not Connected";
            return CodingProjectVO.builder().status(-1).info(responseInfo).projectId(projectId).build();
        }
        String command = "python " + project.getPyPath();
        CompletableFuture.runAsync( () -> dockerService.runCMDInContainer(userId, projectId, containerId, command));
        responseInfo = "Script Executing Successfully";
        return CodingProjectVO.builder().status(1).info(responseInfo).projectId(projectId).build();
    }

    public CodingProjectVO stopScript(ProjectBasicDTO projectBasicDTO) {
        String userId = projectBasicDTO.getUserId(); String projectId = projectBasicDTO.getProjectId();
        String responseInfo = "";
        if ( !projectDataService.VerifyUserProject(userId, projectId) ) {
            responseInfo = "User " + userId + " Can't Operate Project " + projectId;
            return CodingProjectVO.builder().status(-1).info(responseInfo).projectId(projectId).build();
        }
        Project project = projectDataService.getProjectById(projectId);
        if ( project == null){
            responseInfo = "Project " + projectId + " hasn't been Registered";
            return CodingProjectVO.builder().status(-1).info(responseInfo).projectId(projectId).build();
        }
        String containerId = project.getContainerId();
        String common = "kill -INT `pidof python`";
        dockerService.runCMDInContainer(userId, projectId, containerId, common);
        responseInfo = "Script Stopped Successfully";
        return CodingProjectVO.builder().status(1).info(responseInfo).projectId(projectId).build();
    }

    // Environment Services
    public CodingProjectVO environmentOperation(ProjectEnvironmentDTO projectEnvironmentDTO) {
        String projectId = projectEnvironmentDTO.getProjectId(); String userId = projectEnvironmentDTO.getUserId();
        String responseInfo = "";
        if ( !projectDataService.VerifyUserProject(userId, projectId) ) {
            responseInfo = "User " + userId + " Can't Operate Project " + projectId;
            return CodingProjectVO.builder().status(-1).info(responseInfo).projectId(projectId).build();
        }
        Project project = projectDataService.getProjectById(projectId);
        if ( project == null){
            responseInfo = "Project " + projectId + " hasn't been Registered";
            return CodingProjectVO.builder().status(-1).info(responseInfo).projectId(projectId).build();
        }
        String containerId = project.getContainerId();
        String command = projectEnvironmentDTO.getCommand();
        CompletableFuture.runAsync( () -> dockerService.runCMDInContainer(userId, projectId, containerId, command));
        responseInfo = "Command " + command + " has been Executing";
        return CodingProjectVO.builder().status(1).info(responseInfo).projectId(projectId).build();
    }

    public CodingProjectVO packageOperation(ProjectPackageDTO projectPackageDTO) {
        String userId = projectPackageDTO.getUserId(); String projectId = projectPackageDTO.getProjectId();
        String responseInfo = "";
        if ( !projectDataService.VerifyUserProject(userId, projectId) ) {
            responseInfo = "User " + userId + " Can't Operate Project " + projectId;
            return CodingProjectVO.builder().status(-1).info(responseInfo).projectId(projectId).build();
        }
        Project project = projectDataService.getProjectById(projectId);
        if ( project == null){
            responseInfo = "Project " + projectId + " hasn't been Registered";
            return CodingProjectVO.builder().status(-1).info(responseInfo).projectId(projectId).build();
        }
        String containerId = project.getContainerId();
        String action = projectPackageDTO.getAction();
        String name = projectPackageDTO.getName();
        String version = projectPackageDTO.getVersion();
        String command = "";
        switch (action) {
            case "add" -> {
                if (version == null) {
                    command = "pip install " + name;
                } else {
                    command = "pip install " + name + "==" + version;
                }
            }
            case "remove" -> {
                if (version == null) {
                    command = "pip uninstall " + name + " -y";
                } else {
                    command = "pip uninstall " + name + "==" + version + " -y";
                }
            }
        }
        dockerService.runCMDInContainer(userId, projectId, containerId, command);
        responseInfo = "Package " + name + version + " has been Installing";
        return CodingProjectVO.builder().status(1).info(responseInfo).projectId(projectId).build();
    }

}
