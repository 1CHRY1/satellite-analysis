package nnu.mnr.satellite.controller.modeling;

import nnu.mnr.satellite.model.dto.modeling.CreateProjectDTO;
import nnu.mnr.satellite.model.dto.modeling.ProjectFileDTO;
import nnu.mnr.satellite.model.dto.modeling.RunProjectDTO;
import nnu.mnr.satellite.model.pojo.common.DFileInfo;
import nnu.mnr.satellite.model.vo.modeling.CodingProjectVO;
import nnu.mnr.satellite.service.modeling.ModelCodingService;
import org.apache.ibatis.annotations.Delete;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    // Project Controller
    @PostMapping("/project/new")
    public ResponseEntity<CodingProjectVO> createCodingProject(@RequestBody CreateProjectDTO createProjectDTO) {
        return ResponseEntity.ok(modelCodingService.createCodingProject(createProjectDTO));
    }

    @PostMapping("/project/{projectId}/{action}")
    public ResponseEntity<CodingProjectVO> openCodingProject(@PathVariable String projectId, @PathVariable String action) {
        return switch (action) {
            case "open" -> ResponseEntity.ok(modelCodingService.openCodingProject(projectId));
            case "close" -> ResponseEntity.ok(modelCodingService.closeProjectContainer(projectId));
            default -> ResponseEntity.ok(CodingProjectVO.builder().info("No such Action").status(-1).build());
        };
    }

    @DeleteMapping("/project/{projectId}")
    public ResponseEntity<CodingProjectVO> deleteCodingProject(@PathVariable String projectId) {
        return ResponseEntity.ok(modelCodingService.deleteCodingProject(projectId));
    }

    // File Controller
    @PostMapping("/project/file")
    public ResponseEntity<List<DFileInfo>> getCurFile(@RequestBody ProjectFileDTO projectFileDTO) {
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

    // Operating Controller
    @PostMapping("/project/executing")
    public ResponseEntity<CodingProjectVO> runPythonScript(@RequestBody RunProjectDTO runProjectDTO) {
        return ResponseEntity.ok(modelCodingService.runScript(runProjectDTO));
    }

}
