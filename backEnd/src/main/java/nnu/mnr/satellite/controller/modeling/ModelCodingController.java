package nnu.mnr.satellite.controller.modeling;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import nnu.mnr.satellite.model.dto.common.FileData;
import nnu.mnr.satellite.model.dto.modeling.*;
import nnu.mnr.satellite.model.pojo.common.DFileInfo;
import nnu.mnr.satellite.model.vo.modeling.CodingProjectVO;
import nnu.mnr.satellite.model.vo.modeling.ProjectResultVO;
import nnu.mnr.satellite.model.vo.modeling.TilerVO;
import nnu.mnr.satellite.service.modeling.ModelCodingService;
import nnu.mnr.satellite.service.modeling.ProjectResultDataService;
import nnu.mnr.satellite.utils.common.FileUtil;
import nnu.mnr.satellite.utils.docker.DockerFileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/24 9:39
 * @Description:
 */

@RestController
@RequestMapping("/api/v1/coding")
public class ModelCodingController {

    @Autowired
    ModelCodingService modelCodingService;

    @Autowired
    ProjectResultDataService projectResultDataService;

    // Project Controller
    @PostMapping("/project/new")
    public ResponseEntity<CodingProjectVO> createCodingProject(@RequestBody CreateProjectDTO createProjectDTO) throws JSchException, SftpException, IOException {
        return ResponseEntity.ok(modelCodingService.createCodingProject(createProjectDTO));
    }

    @PostMapping("/project/operating")
    public ResponseEntity<CodingProjectVO> openCodingProject(@RequestBody ProjectOperateDTO projectOperateDTO) throws InterruptedException {
        String action = projectOperateDTO.getAction();
        if (action == null) {
            return ResponseEntity.ok(CodingProjectVO.builder().info("Action is null").status(-1).build());
        }
        return switch (action) {
            case "open" -> ResponseEntity.ok(modelCodingService.openCodingProject(projectOperateDTO));
            case "close" -> ResponseEntity.ok(modelCodingService.closeProjectContainer(projectOperateDTO));
            case "delete" -> ResponseEntity.ok(modelCodingService.deleteCodingProject(projectOperateDTO));
            default -> ResponseEntity.ok(CodingProjectVO.builder().info("No such Action").status(-1).build());
        };
    }

    // File Controller
    @PostMapping("/project/file/script")
    public ResponseEntity<String> getPyContent(@RequestBody ProjectBasicDTO projectBasicDTO) throws JSchException, SftpException, IOException {
        return ResponseEntity.ok(modelCodingService.getMainPyOfContainer(projectBasicDTO));
    }

    @PostMapping("/project/file")
    public ResponseEntity<List<DFileInfo>> getCurFile(@RequestBody ProjectFileDTO projectFileDTO) throws Exception {
        return ResponseEntity.ok(modelCodingService.getProjectCurDirFiles(projectFileDTO));
    }

    @PutMapping("/project/file")
    public ResponseEntity<CodingProjectVO> createFolder(@RequestBody ProjectFileDTO projectFileDTO) {
        return ResponseEntity.ok(modelCodingService.newProjectFolder(projectFileDTO));
    }

    @DeleteMapping("/project/file")
    public ResponseEntity<CodingProjectVO> saveCurFile(@RequestBody ProjectFileDTO projectFileDTO) {
        return ResponseEntity.ok(modelCodingService.deleteProjectFile(projectFileDTO));
    }

    @PutMapping("/project/file/script")
    public ResponseEntity<CodingProjectVO> savePythonScript(@RequestBody ProjectFileDTO projectFileDTO) {
        return ResponseEntity.ok(modelCodingService.saveProjectCode(projectFileDTO));
    }

    @PostMapping("/project/file/geojson")
    public ResponseEntity<CodingProjectVO> uploadGeoJson(@RequestBody ProjectDataDTO projectDataDTO) throws IOException {
        return ResponseEntity.ok(modelCodingService.uploadGeoJson(projectDataDTO));
    }

    // Operating Controller
    @PostMapping("/project/executing")
    public ResponseEntity<CodingProjectVO> runPythonScript(@RequestBody ProjectBasicDTO projectBasicDTO) {
        return ResponseEntity.ok(modelCodingService.runScript(projectBasicDTO));
    }

    @PostMapping("/project/executing/watcher")
    public ResponseEntity<CodingProjectVO> runWatcherScript(@RequestBody ProjectBasicDTO projectBasicDTO) {
        return ResponseEntity.ok(modelCodingService.runWatcher(projectBasicDTO));
    }

    @PostMapping("/project/canceling")
    public ResponseEntity<CodingProjectVO> stopPythonScript(@RequestBody ProjectBasicDTO projectBasicDTO) {
        return ResponseEntity.ok(modelCodingService.stopScript(projectBasicDTO));
    }

    // Environment Controller
    @PostMapping("/project/package")
    public ResponseEntity<CodingProjectVO> projectPackageOperation(@RequestBody ProjectPackageDTO projectPackageDTO) {
        return ResponseEntity.ok(modelCodingService.packageOperation(projectPackageDTO));
    }

    @PostMapping("/project/package/list")
    public ResponseEntity<HashSet<String>> getEnvironmentPackages(@RequestBody ProjectBasicDTO projectBasicDTO) {
        return ResponseEntity.ok(modelCodingService.getEnvironmentPackages(projectBasicDTO));
    }

    @PostMapping("/project/environment")
    public ResponseEntity<CodingProjectVO> projectEnvironmentOperation(@RequestBody ProjectEnvironmentDTO projectEnvironmentDTO) {
        return ResponseEntity.ok(modelCodingService.environmentOperation(projectEnvironmentDTO));
    }

    // Result Controller
    @PostMapping("/project/results")
    public ResponseEntity<List<ProjectResultVO>> getProjectResults(@RequestBody ProjectBasicDTO projectBasicDTO) {
        return ResponseEntity.ok(projectResultDataService.getProjectResults(projectBasicDTO));
    }

    @GetMapping("/project/result/tif/{dataId}")
    public ResponseEntity<TilerVO> getProjectTifResult(@PathVariable String dataId) {
        return ResponseEntity.ok(projectResultDataService.getProjectTifResult(dataId));
    }

    @PostMapping("/project/result")
    public ResponseEntity<byte[]> getProjectResult(@RequestBody ProjectResultDTO projectResultDTO) {
        FileData resultData = projectResultDataService.getProjectResult(projectResultDTO);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(FileUtil.setMediaType(resultData.getType()));
        return ResponseEntity.ok()
                .headers(headers)
                .body(resultData.getStream());
    }
}
