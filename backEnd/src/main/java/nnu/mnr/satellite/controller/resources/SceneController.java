package nnu.mnr.satellite.controller.resources;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.model.dto.common.GeoJsonDTO;
import nnu.mnr.satellite.model.dto.resources.SceneDesDTO;
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
    public ResponseEntity<SceneDesDTO> getDescriptionById(@PathVariable String sceneId) throws IOException, FactoryException {
        return ResponseEntity.ok(sceneDataService.getSceneById(sceneId));
    }

    @PostMapping("/sensorId/productId/time/area")
    public ResponseEntity<GeoJsonDTO> getScenesByIdsTimeAndBBox(@RequestBody JSONObject paramObj) throws IOException {
        return ResponseEntity.ok(sceneDataService.getScenesByIdsTimeAndBBox(paramObj));
    }

    @GetMapping("/png/sceneId/{sceneId}")
    public ResponseEntity<byte[]> getScenePngById(@PathVariable String sceneId) {
        byte[] imageData = sceneDataService.getPngById(sceneId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("image/png"));
        return ResponseEntity.ok()
                .headers(headers)
                .body(imageData);
    }

}
