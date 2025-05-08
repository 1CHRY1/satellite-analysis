package nnu.mnr.satellite.controller.modeling;

import nnu.mnr.satellite.model.vo.modeling.ProjectVO;
import nnu.mnr.satellite.service.modeling.ProjectDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/29 15:03
 * @Description:
 */

@RestController
@RequestMapping("/api/v1/modeling/project")
public class ProjectController {

    private final ProjectDataService projectDataService;

    public ProjectController(ProjectDataService projectDataService) {
        this.projectDataService = projectDataService;
    }

    @GetMapping("/all")
    public ResponseEntity<List<ProjectVO>> getAllProjects() {
        return ResponseEntity.ok(projectDataService.getAllProjects());
    }

    @GetMapping("/userId/{userId}")
    public ResponseEntity<List<ProjectVO>> getAllProjects(@PathVariable String userId) {
        return ResponseEntity.ok(projectDataService.getProjectsByUserId(userId));
    }
}
