package nnu.mnr.satellite.controller.resources;

import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.model.dto.resources.ScenesFetchDTO;
import nnu.mnr.satellite.model.vo.common.GeoJsonVO;
import nnu.mnr.satellite.model.vo.resources.SceneDesVO;
import nnu.mnr.satellite.service.resources.SceneDataService;
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
    public ResponseEntity<SceneDesVO> getDescriptionById(@PathVariable String sceneId) throws IOException, FactoryException {
        return ResponseEntity.ok(sceneDataService.getSceneById(sceneId));
    }

    @PostMapping("/sensorId/productId/time/area")
    public ResponseEntity<GeoJsonVO> getRegionById(@RequestBody ScenesFetchDTO scenesFetchDTO) throws IOException {
        return ResponseEntity.ok(sceneDataService.getScenesByIdsTimeAndBBox(scenesFetchDTO));
    }

}
