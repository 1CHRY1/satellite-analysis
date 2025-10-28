package nnu.mnr.satellite.controller.admin;

import nnu.mnr.satellite.model.dto.admin.image.ImageDeleteDTO;
import nnu.mnr.satellite.model.dto.admin.image.ImageUpdateDTO;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.service.admin.AdminImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("admin/api/v1/image")
public class AdminImageController {

    @Autowired
    private AdminImageService adminImageService;

    @GetMapping("/sceneId/{sceneId}")
    public ResponseEntity<CommonResultVO> getImageBySceneId(@PathVariable("sceneId") String sceneId) {
        return ResponseEntity.ok(adminImageService.getImagesBySceneId(sceneId));
    }

    @PostMapping("/update")
    public ResponseEntity<CommonResultVO> updateImageInfo(@RequestBody ImageUpdateDTO imageUpdateDTO) {
        return ResponseEntity.ok(adminImageService.updateImageInfo(imageUpdateDTO));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<CommonResultVO> deleteImage(@RequestBody ImageDeleteDTO imageDeleteDTO){
        return ResponseEntity.ok(adminImageService.deleteImage(imageDeleteDTO));
    }
}
