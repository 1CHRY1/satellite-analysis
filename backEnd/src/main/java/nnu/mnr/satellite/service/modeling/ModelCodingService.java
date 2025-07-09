package nnu.mnr.satellite.service.modeling;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.dynamic.datasource.creator.DataSourceProperty;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceProperties;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.config.web.JSchConnectionManager;
import nnu.mnr.satellite.constants.DockerContants;
import nnu.mnr.satellite.constants.UserConstants;
import nnu.mnr.satellite.model.po.user.User;
import nnu.mnr.satellite.model.pojo.modeling.MinioProperties;
import nnu.mnr.satellite.model.dto.modeling.*;
import nnu.mnr.satellite.model.po.modeling.Project;
import nnu.mnr.satellite.model.pojo.common.DFileInfo;
import nnu.mnr.satellite.model.pojo.modeling.DockerServerProperties;
import nnu.mnr.satellite.model.vo.modeling.CodingProjectVO;
import nnu.mnr.satellite.mapper.modeling.IProjectRepo;
import nnu.mnr.satellite.mapper.resources.ITileRepo;
import nnu.mnr.satellite.service.common.DockerService;
import nnu.mnr.satellite.service.common.SftpDataService;
import nnu.mnr.satellite.service.common.UnifiedDockerService;
import nnu.mnr.satellite.service.user.UserService;
import nnu.mnr.satellite.service.optimization.CodeOptimizationFacade;
import nnu.mnr.satellite.service.optimization.OptimizationRequest;
import nnu.mnr.satellite.service.optimization.CodeOptimizationOutcome;
import nnu.mnr.satellite.service.optimization.ExecutionPlan;
import nnu.mnr.satellite.utils.common.FileUtil;
import nnu.mnr.satellite.utils.common.IdUtil;
import nnu.mnr.satellite.utils.dt.MinioUtil;
import nnu.mnr.satellite.utils.docker.DockerFileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
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
    UnifiedDockerService unifiedDockerService;

    @Autowired
    UserService userService;

    @Autowired
    ProjectDataService projectDataService;

    @Autowired
    IProjectRepo projectRepo;

    @Autowired
    ITileRepo tileRepo;

    @Autowired
    MinioUtil minioUtil;

    @Autowired
    CodeOptimizationFacade codeOptimizationFacade;

    // config info
    @Autowired
    private DynamicDataSourceProperties dynamicDataSourceProperties;

    @Autowired
    private MinioProperties minioProperties;

    private final String projectDataBucket = "project-data-bucket";

    public String getRemoteConfig(Project project) {
        JSONObject projectConfig = JSONObject.of(
                "project_id", project.getProjectId(),
                "user_id", project.getCreateUser(),
                "bucket", project.getDataBucket(),
                "watch_dir", project.getOutputPath());
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
//                "satellite_database", satelliteDs.getUrl().split("://")[1].split("/", 2)[1],
                "satellite_database", "satellite",
                "tile_database", tileDs.getUrl().split("://")[1].split("/", 2)[1]);
        JSONObject remoteConfig = JSONObject.of("minio", minioConfig, "database", databaseConfig, "project_info", projectConfig);
        return  remoteConfig.toString();
    }

    // Coding Project Services
    public CodingProjectVO createCodingProject(CreateProjectDTO createProjectDTO) throws JSchException, SftpException, IOException {
        String userId = createProjectDTO.getUserId();
        String responseInfo = "";
        if ( !userService.ifUserExist(userId)) {
            responseInfo = String.format(UserConstants.USER_NOT_FOUND, userId);
            return CodingProjectVO.builder().status(-1).info(responseInfo).build();
        }
        User user = userService.getUserById(userId);
        String projectName = createProjectDTO.getProjectName();
        String env = createProjectDTO.getEnvironment();
        String imageEnv = DockerFileUtil.getImageByName(env);
        if (imageEnv.equals(DockerContants.NO_IMAGE)) {
            responseInfo = "No Such Image";
            return CodingProjectVO.builder().status(-1).info(responseInfo).build();
        }
        String projectId = IdUtil.generateProjectId();
        // 根据Docker模式设置路径
        String serverDir = unifiedDockerService.getServerPath() + "/" + projectId + "/";
        String workDir = unifiedDockerService.getWorkDir();
        String pyPath = workDir + "main.py";
        String watchPath = workDir + "watcher.py";
        String outputPath = workDir + "output";
        String dataPath = workDir + "data";
        String serverPyPath = serverDir + "main.py";
        Project formerProject = projectDataService.getProjectByUserAndName(userId, projectName);
        if ( formerProject != null){
            responseInfo = "Project " + formerProject.getProjectName() + " has been Registered";
            return CodingProjectVO.builder().status(-1).info(responseInfo).build();
        }
        Project project = Project.builder()
                .projectId(projectId).projectName(projectName)
                .environment(env).createTime(LocalDateTime.now()).dataBucket(projectDataBucket)
                .workDir(workDir).serverDir(serverDir).description(createProjectDTO.getDescription())
                .createUser(userId).createUserEmail(user.getEmail()).createUserName(user.getUserName())
                .pyPath(pyPath).serverPyPath(serverPyPath)
                .watchPath(watchPath).outputPath(outputPath).dataPath(dataPath)
                .build();
        unifiedDockerService.initEnv(serverDir);

        // Load Config
        String configPath = serverDir + "config.json";
        unifiedDockerService.writeFile(configPath, getRemoteConfig(project));

        String containerId = unifiedDockerService.createContainer(imageEnv, projectId, project.getServerDir(), workDir);
        String pyContent = unifiedDockerService.readFile(project.getServerPyPath());
        project.setPyContent(pyContent);
        project.setContainerId(containerId);
        unifiedDockerService.startContainer(containerId);

        // Start Watching Process
        String watchCommand = "nohup python -u " + project.getWatchPath() + " > watcher.out 2>&1";
        CompletableFuture.runAsync(() -> unifiedDockerService.runCMDInContainerWithoutInteraction(userId, projectId, containerId, watchCommand));
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
            String containerId = project.getContainerId();
            // Container Running ?
            if (!unifiedDockerService.checkContainerState(containerId)){
                unifiedDockerService.startContainer(containerId);
                responseInfo = "Project " + projectId + " is Opened";
            } else { responseInfo = "Project " + projectId + " has been Opened"; }
            // Start Watching Process
            String watchCommand = "nohup python -u " + project.getWatchPath() + " > watcher.out 2>&1";
            CompletableFuture.runAsync( () -> unifiedDockerService.runCMDInContainer(userId, projectId, containerId, watchCommand));

            return CodingProjectVO.builder().status(1).info(responseInfo).projectId(projectId).build();
        } catch (Exception e) {
            responseInfo = "Wrong Openning Container " + e.getMessage();
            return CodingProjectVO.builder().status(-1).info(responseInfo).projectId(projectId).build();
        }
    }

    public CodingProjectVO deleteCodingProject(ProjectOperateDTO projectOperateDTO) throws InterruptedException {
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
        // Delete Local Data & Remote Data
        FileUtil.deleteFolder(new File(dockerServerProperties.getLocalPath() + projectId));
        // Delete Container if Exists
        if (project.getContainerId() != null){
            String containerId = project.getContainerId();
            CompletableFuture.runAsync( () -> {
                try {
                    unifiedDockerService.stopContainer(containerId);
                } catch (Exception e) {
                    log.error("Failed to stop container", e);
                } finally {
                    try {
                        unifiedDockerService.removeContainer(containerId);
                    } catch (Exception e) {
                        log.error("Failed to remove container", e);
                    }
                }
            });
        }
        // Wait for Watch Dog Deleting Data
        CompletableFuture<Void> deleteContentsFuture = CompletableFuture.runAsync(() -> {
            try {
                unifiedDockerService.deleteFolderContents(project.getServerDir() + "output/");
            } catch (Exception e) {
                log.error("Failed to delete folder contents", e);
            }
        });
        deleteContentsFuture.thenRunAsync(() -> {
            try {
                Thread.sleep(5000);
                unifiedDockerService.deleteFolder(project.getServerDir());
            } catch (Exception e) {
                log.error("Failed to delete folder", e);
            }
        }).exceptionally(throwable -> {
            log.error("Error in async deletion", throwable);
            return null;
        });
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
                unifiedDockerService.stopContainer(containerId);
            } catch (Exception e) {
                log.error("Failed to stop container", e);
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
        return unifiedDockerService.readFile(project.getServerPyPath());
    }

    public List<DFileInfo> getProjectCurDirFiles(ProjectFileDTO projectFileDTO) throws Exception {
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
        if (!unifiedDockerService.checkContainerState(containerId)) {
            return null;
        }
        List<DFileInfo> fileTree = new ArrayList<>();
        return unifiedDockerService.getCurDirFiles(projectId, containerId, path, fileTree);
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
        unifiedDockerService.runCMDInContainer(userId, projectId, containerId, command);
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
        unifiedDockerService.runCMDInContainer(userId, projectId, containerId, cmdBuilder.toString());
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
            String content = projectFileDTO.getContent();
            project.setPyContent(projectFileDTO.getContent());
            projectRepo.updateById(project);
            try (InputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
                unifiedDockerService.uploadFileFromStream(inputStream, project.getServerPyPath());
            }
            responseInfo = "Python Script " + projectId + " has been Saved";
            return CodingProjectVO.builder().status(1).info(responseInfo).projectId(projectId).build();
        } catch (Exception e) {
            responseInfo = "Python Script " + projectId + " hasn't been Saved Because of: " + e.getMessage();
            return CodingProjectVO.builder().status(-1).info(responseInfo).projectId(projectId).build();
        }
    }

    public CodingProjectVO uploadGeoJson(ProjectDataDTO projectDataDTO) throws IOException {
        String userId = projectDataDTO.getUserId(); String projectId = projectDataDTO.getProjectId();
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
        JSONObject geometry = projectDataDTO.getUploadData();
        String fileName = projectDataDTO.getUploadDataName() + ".geojson";
        String remotePath = project.getServerDir() + "data/" + fileName;
        try (InputStream inputStream = new ByteArrayInputStream(geometry.toString().getBytes(StandardCharsets.UTF_8))) {
            unifiedDockerService.uploadFileFromStream(inputStream, remotePath);
        }
        responseInfo = "Project Data has been Uploaded";
        return CodingProjectVO.builder().status(1).info(responseInfo).projectId(projectId).build();
    }

    public CodingProjectVO uploadTilesToProject(ProjectTileDataDTO projectTileDataDTO) {
        String userId = projectTileDataDTO.getUserId(); String projectId = projectTileDataDTO.getProjectId();
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
        // TODO: add another database
        String object = projectTileDataDTO.getObject();
        int index = object.indexOf("/");
        String bucket = object.substring(0, index);
        String path = object.substring(index + 1);
//        String sceneId = projectTileDataDTO.getSceneId();
//        List<String> tileIds = projectTileDataDTO.getTileIds();
//        CompletableFuture.runAsync(() -> {
//            String remoteDataPath = project.getServerDir() + "data/";
//            sftpDataService.createRemoteDir( remoteDataPath + sceneId + "/", 0777);
//            for (String tileId : tileIds) {
//                Tile tile = tileRepo.getTileByTileId(sceneId, tileId);
//                InputStream tileStream = minioUtil.getObjectStream(tile.getBucket(), tile.getPath());
//                int lastIndex = tile.getPath().lastIndexOf('/');
//                String tileName = tile.getPath().substring(lastIndex + 1);
//                sftpDataService.uploadFileFromStream(tileStream, remoteDataPath + sceneId + "/" + tileName);
//            }
//        });
        CompletableFuture.runAsync(() -> {
            String remoteDataPath = project.getServerDir() + "data/";
            InputStream tileStream = minioUtil.getObjectStream(bucket, path);
            unifiedDockerService.uploadFileFromStream(tileStream, remoteDataPath + projectTileDataDTO.getName() + ".tif");
        });
        responseInfo = "Data Uploading Successfully";
        return CodingProjectVO.builder().status(1).info(responseInfo).projectId(projectId).build();
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
        
        // ========= Ray优化集成 =========
        try {
            log.info("Starting Ray optimization for project: {}", projectId);
            
            // 1. 创建优化请求
            OptimizationRequest optimizationRequest = OptimizationRequest.create(project, containerId, userId);
            
            // 2. 执行优化并获取执行计划
            CodeOptimizationOutcome outcome = codeOptimizationFacade.optimizeAndPrepareExecution(optimizationRequest);
            
            // 3. 获取执行计划
            ExecutionPlan executionPlan = outcome.getExecutionPlan();
            
            // 4. 执行优化后的代码（或回退到原始代码）
            if (executionPlan != null) {
                String finalCommand = executionPlan.getCommand();
                log.info("Executing {} code for project {}: {}", 
                        executionPlan.isOptimized() ? "Ray optimized" : "original", 
                        projectId, executionPlan.getDescription());
                
                CompletableFuture.runAsync(() -> dockerService.runCMDInContainer(userId, projectId, containerId, finalCommand));
                
                responseInfo = outcome.isUsingOptimizedCode() ? 
                    "Ray优化代码执行中 (预期加速" + String.format("%.1f", outcome.getOptimizationResult().getExpectedSpeedup()) + "x)" :
                    "原始代码执行中 (" + outcome.getExecutionDescription() + ")";
            } else {
                // 回退到原始执行方式
                String command = "python " + project.getPyPath();
                CompletableFuture.runAsync(() -> dockerService.runCMDInContainer(userId, projectId, containerId, command));
                responseInfo = "Script Executing Successfully (fallback)";
            }
            
        } catch (Exception e) {
            log.error("Ray optimization failed for project {}, falling back to original execution: {}", 
                     projectId, e.getMessage());
            
            // 回退到原始执行方式
        String command = "python " + project.getPyPath();
            CompletableFuture.runAsync(() -> dockerService.runCMDInContainer(userId, projectId, containerId, command));
            responseInfo = "Script Executing Successfully (optimization failed, using original code)";
        }
        
        return CodingProjectVO.builder().status(1).info(responseInfo).projectId(projectId).build();
    }

    public CodingProjectVO runWatcher(ProjectBasicDTO projectBasicDTO) {
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
        String watchCommand = "nohup python -u " + project.getWatchPath() + " > watcher.out 2>&1";
        CompletableFuture.runAsync( () -> dockerService.runCMDInContainer(userId, projectId, containerId, watchCommand));
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
        runWatcher(projectBasicDTO);
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

    public HashSet<String> getEnvironmentPackages(ProjectBasicDTO projectBasicDTO) {
        String projectId = projectBasicDTO.getProjectId(); String userId = projectBasicDTO.getUserId();
        if ( !projectDataService.VerifyUserProject(userId, projectId) ) {
            return null;
        }
        Project project = projectDataService.getProjectById(projectId);
        if ( project == null){
            return null;
        }
        return project.getPackages();
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
        String pyPackage = version == null ? name : name + " " + version;
        HashSet<String> installedPackages = project.getPackages();
        String conditionStr = "";
        switch (action) {
            case "add" -> {
                if (installedPackages.contains(pyPackage) ) {
                    responseInfo = "Package " + name + version + " has been Installed";
                    return CodingProjectVO.builder().status(-1).info(responseInfo).projectId(projectId).build();
                }
                if (version == null) {
                    command = "pip install " + name;
                } else {
                    command = "pip install " + name + "==" + version;
                }
                project.getPackages().add(pyPackage);
                conditionStr = "Installing";
            }
            case "remove" -> {
                if (version == null) {
                    command = "pip uninstall " + name + " -y";
                } else {
                    command = "pip uninstall " + name + "==" + version + " -y";
                }
                project.getPackages().remove(pyPackage);
                conditionStr = "Removing";
            }
        }
        String finalCommand = command;
        CompletableFuture.runAsync( () -> {
            dockerService.runCMDInContainer(userId, projectId, containerId, finalCommand);
        });

        projectRepo.updateById(project);
        responseInfo = "Package " + name + version + " has been " + conditionStr;
        return CodingProjectVO.builder().status(1).info(responseInfo).projectId(projectId).build();
    }

}
