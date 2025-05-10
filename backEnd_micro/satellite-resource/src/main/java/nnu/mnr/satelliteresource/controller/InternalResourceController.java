package nnu.mnr.satelliteresource.controller;
import nnu.mnr.satelliteresource.model.dto.modeling.ModelServerImageDTO;
import nnu.mnr.satelliteresource.model.po.Region;
import nnu.mnr.satelliteresource.model.po.Scene;
import nnu.mnr.satelliteresource.service.ImageDataService;
import nnu.mnr.satelliteresource.service.RegionDataService;
import nnu.mnr.satelliteresource.service.SceneDataServiceV2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/4/22 17:07
 * @Description:
 */

@RestController
@RequestMapping("/api/v1/resource/internal")
public class InternalResourceController {

    @Autowired
    ImageDataService imageDataService;

    @Autowired
    SceneDataServiceV2 sceneDataServiceV2;

    @Autowired
    RegionDataService regionDataService;

    @GetMapping("/region/id/{regionId}")
    public ResponseEntity<Region> getRegionById(@PathVariable Integer regionId) {
        return ResponseEntity.ok(regionDataService.getRegionById(regionId));
    }

    @GetMapping("/image/dto/scene/{sceneId}")
    public ResponseEntity<List<ModelServerImageDTO>> getModelServerImageDTO(@PathVariable String sceneId) {
        return ResponseEntity.ok(imageDataService.getModelServerImageDTOBySceneId(sceneId));
    }

    @GetMapping("/scene/id/{sceneId}")
    public ResponseEntity<Scene> getSceneById(@PathVariable String sceneId) {
        return ResponseEntity.ok(sceneDataServiceV2.getSceneById(sceneId));
    }

}
