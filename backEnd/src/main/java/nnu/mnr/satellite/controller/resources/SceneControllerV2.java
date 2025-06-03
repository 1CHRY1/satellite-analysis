package nnu.mnr.satellite.controller.resources;

import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.model.dto.modeling.ModelServerSceneDTO;
import nnu.mnr.satellite.model.dto.resources.*;
import nnu.mnr.satellite.model.vo.resources.ViewWindowVO;
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
        return ResponseEntity.ok(sceneDataService.getScenesDesByTimeRegionAndCloud(scenesFetchDTO));
    }

    @PostMapping("/time/cloud/resolution/location")
    public ResponseEntity<List<SceneDesVO>> getScenesDesByTimeLocationAndCloud(@RequestBody ScenesLocationFetchDTO scenesFetchDTO) throws IOException {
        return ResponseEntity.ok(sceneDataService.getScenesDesByTimeLocationAndCloud(scenesFetchDTO));
    }

    @PostMapping("/raster/time/region")
    public ResponseEntity<List<SceneDesVO>> getRasterScenesDesByRegionAndDataType(@RequestBody RastersFetchDTO rastersFetchDTO) throws IOException {
        return ResponseEntity.ok(sceneDataService.getRasterScenesDesByRegionAndDataType(rastersFetchDTO));
    }

    @PostMapping("/cover/region/sceneIds")
    public ResponseEntity<List<ModelServerSceneDTO>> getCoveredSceneByRegionResolutionAndSensor(@RequestBody CoverFetchSceneDTO coverFetchSceneDTO) {
        return ResponseEntity.ok(sceneDataService.getCoveredSceneByRegionResolutionAndSensor(coverFetchSceneDTO));
    }

    @PostMapping("/cover/location/resolution/sceneIds")
    public ResponseEntity<List<ModelServerSceneDTO>> getCoveredSceneByLocationResolutionAndSensor(@RequestBody CoverLocationFetchSceneDTO coverFetchSceneDTO) {
        return ResponseEntity.ok(sceneDataService.getCoveredSceneByLocationResolutionAndSensor(coverFetchSceneDTO));
    }

    @GetMapping("/boundary/sceneId/{sceneId}")
    public ResponseEntity<JSONObject> getSceneBoundaryById(@PathVariable String sceneId) throws IOException {
        return ResponseEntity.ok(sceneDataService.getSceneBoundaryById(sceneId));
    }

    @GetMapping("/window/sceneId/{sceneId}")
    public ResponseEntity<ViewWindowVO> getSceneWindowById(@PathVariable String sceneId) throws IOException {
        return ResponseEntity.ok(sceneDataService.getSceneWindowById(sceneId));
    }

    @GetMapping("/sceneId/{sceneId}")
    public ResponseEntity<SceneImageDTO> getSceneByIdWithProductAndSensor(@PathVariable String sceneId) {
        return ResponseEntity.ok(sceneDataService.getSceneByIdWithImage(sceneId));
    }

}
