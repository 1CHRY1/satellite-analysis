package nnu.mnr.satelliteresource.controller;

import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satelliteresource.model.dto.resources.ScenesFetchDTO;
import nnu.mnr.satelliteresource.model.vo.common.GeoJsonVO;
import nnu.mnr.satelliteresource.model.vo.resources.SceneDesVO;
import nnu.mnr.satelliteresource.service.SceneDataService;
import org.opengis.referencing.FactoryException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/12 17:14
 * @Description:
 */

@RestController
@RequestMapping("api/v1/data/scene")
@Slf4j
public class SceneController {

    private final SceneDataService sceneDataService;

    public SceneController(SceneDataService sceneDataService) {
        this.sceneDataService = sceneDataService;
    }

    @GetMapping("/description/sceneId/{sceneId}")
    public ResponseEntity<SceneDesVO> getSceneDescriptionById(@PathVariable("sceneId") String sceneId) {
        return ResponseEntity.ok(sceneDataService.getSceneById(sceneId));
    }

    @PostMapping("/sensorId/productId/time/area")
    public ResponseEntity<GeoJsonVO> getScenesByIdsTimeAndBBox(@RequestBody ScenesFetchDTO scenesFetchDTO) throws IOException {
        return ResponseEntity.ok(sceneDataService.getScenesByIdsTimeAndBBox(scenesFetchDTO));
    }

}
