package nnu.mnr.satellite.controller;

import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.model.po.Scene;
import nnu.mnr.satellite.service.resources.SceneDataService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/{sceneId}")
    public ResponseEntity<Scene> getDescriptionById(@PathVariable String sceneId) {
        return ResponseEntity.ok(sceneDataService.getSceneById(sceneId));
    }

    @PostMapping("/sensor/product/time/area")
    public ResponseEntity<List<Scene>> getScenesByIdsTimeAndBBox(@RequestBody JSONObject paramObj) {
        return ResponseEntity.ok(sceneDataService.getScenesByIdsTimeAndBBox(paramObj));
    }

    @GetMapping("/{sceneId}/png")
    public ResponseEntity<byte[]> getScenePngById(String sceneId) {
        byte[] imageData = sceneDataService.getPngById(sceneId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("image/png"));
        return ResponseEntity.ok()
                .headers(headers)
                .body(imageData);
    }

}
