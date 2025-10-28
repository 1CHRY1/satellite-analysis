package nnu.mnr.satellite.controller.admin;

import nnu.mnr.satellite.model.dto.admin.scene.SceneDeleteDTO;
import nnu.mnr.satellite.model.dto.admin.scene.ScenePageDTO;
import nnu.mnr.satellite.model.dto.admin.scene.SceneUpdateDTO;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.service.admin.AdminSceneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("admin/api/v1/scene")
public class AdminSceneController {

    @Autowired
    private AdminSceneService adminSceneService;

    @PostMapping("/page")
    public ResponseEntity<CommonResultVO> getSceneInfoPage(@RequestBody ScenePageDTO scenePageDTO){
        return ResponseEntity.ok(adminSceneService.getSceneInfoPage(scenePageDTO));
    }

    @PostMapping("/update")
    public ResponseEntity<CommonResultVO> updateSceneInfo(@RequestBody SceneUpdateDTO sceneUpdateDTO){
        return ResponseEntity.ok(adminSceneService.updateSceneInfo(sceneUpdateDTO));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<CommonResultVO> deleteScene(@RequestBody SceneDeleteDTO sceneDeleteDTO){
        return ResponseEntity.ok(adminSceneService.deleteScene(sceneDeleteDTO));
    }



}
