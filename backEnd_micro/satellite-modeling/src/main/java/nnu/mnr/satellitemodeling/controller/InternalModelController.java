package nnu.mnr.satellitemodeling.controller;
import nnu.mnr.satellitemodeling.service.modeling.ProjectDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/4/22 17:07
 * @Description:
 */

@RestController
@RequestMapping("/api/v1/modeling/internal")
public class InternalModelController {

    @Autowired
    ProjectDataService projectDataService;

    @GetMapping("/validation")
    public ResponseEntity<Boolean> validateProject(@RequestHeader("X-User-Id") String userId, @RequestHeader("X-Project-Id") String projectId) {
        return ResponseEntity.ok(projectDataService.VerifyUserProject(userId, projectId));
    }

}
