package nnu.mnr.satellite.service.modeling;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.config.JSchConnectionManager;
import nnu.mnr.satellite.model.dto.modeling.CreateProjectDTO;
import nnu.mnr.satellite.model.dto.modeling.ProjectFileDTO;
import nnu.mnr.satellite.model.dto.modeling.RunProjectDTO;
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
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    private boolean Verify(String userId, String projectId) {
        // TODO: joined user verify
        QueryWrapper<Project> queryWrapper = new QueryWrapper();
        queryWrapper.eq("create_user", userId).eq("project_id", projectId);
        Project project = projectRepo.selectOne(queryWrapper);
        return project != null;
    }

    // Coding Project Services
    public CodingProjectVO createCodingProject(CreateProjectDTO createProjectDTO) {
        String userId = createProjectDTO.getUserId();
        String projectName = createProjectDTO.getProjectName();
        String env = createProjectDTO.getEnvironment();
        String imageEnv = DockerFileUtil.getImageByName(env);
        String projectId = IdUtil.generateProjectId();
        String serverDir = dockerServerProperties.getServerDir() + projectId + "/";
        String workDir = dockerServerProperties.getWorkDir();
        String pyPath = workDir + "main.py";
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
                .pyPath(pyPath).localPyPath(localPyPath).serverPyPath(serverPyPath)
                .pyContent("")
                .build();
        dockerService.initEnv(projectId, serverDir);
        String containerid = dockerService.createContainer(imageEnv, projectId, project.getServerDir(), workDir);
        project.setContainerId(containerid);
        dockerService.startContainer(containerid);
        projectRepo.insert(project);
        responseInfo = "Project " + projectId + " has been Created";
        return CodingProjectVO.builder().status(1).info(responseInfo).projectId(projectId).build();
    }

    public CodingProjectVO openCodingProject(String userId, String projectId) {
        String responseInfo = "";
        if ( !Verify(userId, projectId) ) {
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
            return CodingProjectVO.builder().status(1).info(responseInfo).projectId(projectId).build();
        } catch (Exception e) {
            responseInfo = "Wrong Openning Container " + e.getMessage();
            return CodingProjectVO.builder().status(-1).info(responseInfo).projectId(projectId).build();
        }
    }

    public CodingProjectVO deleteCodingProject(String userId, String projectId) {
        String responseInfo = "";
        if ( !Verify(userId, projectId) ) {
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
            try {
                dockerService.stopContainer(containerId);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                try {
                    dockerService.removeContainer(containerId);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        // Delete Local Data & Remote Data
        FileUtil.deleteFolder(new File(dockerServerProperties.getLocalPath() + projectId));
        jSchConnectionManager.deleteFolder(project.getServerDir());
        log.info("Successfully deleted folder and its contents: " + project.getServerDir());
        projectRepo.deleteById(projectId);
        responseInfo = "Project " + projectId + " has been Deleted";
        return CodingProjectVO.builder().status(1).info(responseInfo).projectId(projectId).build();
    }

    // Container Services
    public CodingProjectVO closeProjectContainer(String userId, String projectId) {
        String responseInfo = "";
        if ( !Verify(userId, projectId) ) {
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
            dockerService.stopContainer(containerId);
            responseInfo = "Project " + projectId + " has been closed";
            return CodingProjectVO.builder().status(1).info(responseInfo).projectId(projectId).build();
        } catch (InterruptedException e) {
            responseInfo = "Wrong Deleting Container " + e.getMessage();
            return CodingProjectVO.builder().status(-1).info(responseInfo).projectId(projectId).build();
        }

    }

    // File Services
    public List<DFileInfo> getProjectCurDirFiles(ProjectFileDTO projectFileDTO) {
        String projectId = projectFileDTO.getProjectId();
        String userId = projectFileDTO.getUserId();
        if ( !Verify(userId, projectId) ) {
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
        String userId = projectFileDTO.getUserId();
        String projectId = projectFileDTO.getProjectId();
        String responseInfo = "";
        if ( !Verify(userId, projectId) ) {
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
        String userId = projectFileDTO.getUserId();
        String projectId = projectFileDTO.getProjectId();
        String responseInfo = "";
        if ( !Verify(userId, projectId) ) {
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
        String projectId = projectFileDTO.getProjectId();
        String userId = projectFileDTO.getUserId();
        String responseInfo = "";
        if ( !Verify(userId, projectId) ) {
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
    public CodingProjectVO runScript(RunProjectDTO runProjectDTO) {
        String userId = runProjectDTO.getUserId();
        String projectId = runProjectDTO.getProjectId();
        String responseInfo = "";
        if ( !Verify(userId, projectId) ) {
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
        responseInfo = "Script Executed Successfully";
        return CodingProjectVO.builder().status(1).info(responseInfo).projectId(projectId).build();
    }

    public CodingProjectVO stopScript(RunProjectDTO runProjectDTO) {
        String userId = runProjectDTO.getUserId();
        String projectId = runProjectDTO.getProjectId();
        String responseInfo = "";
        if ( !Verify(userId, projectId) ) {
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

}
