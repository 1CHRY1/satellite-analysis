package nnu.mnr.satellite.controller.resources;

import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.model.dto.resources.ScenesFetchDTOV2;
import nnu.mnr.satellite.model.vo.resources.SceneDesVO;
import nnu.mnr.satellite.service.resources.SceneDataServiceV2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/12 17:14
 * @Description:
 */

@RestController
@RequestMapping("api/v2/data/scene")
@Slf4j
public class SceneControllerV2 {

    private final SceneDataServiceV2 sceneDataService;

    public SceneControllerV2(SceneDataServiceV2 sceneDataService) {
        this.sceneDataService = sceneDataService;
    }

    @PostMapping("/time/cloud/region")
    public ResponseEntity<List<SceneDesVO>> getScenesByTimeAndRegion(@RequestBody ScenesFetchDTOV2 scenesFetchDTO) throws IOException {
        return ResponseEntity.ok(sceneDataService.getScenesDesByTimeRegionAndTag(scenesFetchDTO));
    }

    @GetMapping("/boundary/sceneId/{sceneId}")
    public ResponseEntity<JSONObject> getSceneBoundaryById(@PathVariable String sceneId) throws IOException {
        return ResponseEntity.ok(sceneDataService.getSceneBoundaryById(sceneId));
    }

}
