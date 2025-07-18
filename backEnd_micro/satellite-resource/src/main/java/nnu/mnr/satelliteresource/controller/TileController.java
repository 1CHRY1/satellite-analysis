package nnu.mnr.satelliteresource.controller;

import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satelliteresource.model.dto.resources.TilesFetchDTO;
import nnu.mnr.satelliteresource.model.dto.resources.TilesMergeDTO;
import nnu.mnr.satelliteresource.model.vo.common.CommonResultVO;
import nnu.mnr.satelliteresource.model.vo.common.GeoJsonVO;
import nnu.mnr.satelliteresource.model.vo.resources.TileDesVO;
import nnu.mnr.satelliteresource.model.vo.resources.TilesFetchResultVO;
import nnu.mnr.satelliteresource.service.TileDataService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/12 9:30
 * @Description:
 */

@RestController
@RequestMapping("api/v1/data/tile")
@Slf4j
public class TileController {

    private final TileDataService tileDataService;

    public TileController(TileDataService tileDataService) {
        this.tileDataService = tileDataService;
    }

    @GetMapping("/sceneId/{sceneId}/tileLevel/{tileLevel}")
    public ResponseEntity<GeoJsonVO> getTilesByImageAndLevel(@PathVariable String sceneId, @PathVariable String tileLevel) throws IOException {
        GeoJsonVO geoJsonVO = tileDataService.getTilesBySceneAndLevel(sceneId, tileLevel);
        return ResponseEntity.ok(geoJsonVO);
    }

    @PostMapping("/tiler/tiles")
    public ResponseEntity<List<TilesFetchResultVO>> getTilesByLevelBandAndIds(@RequestBody TilesFetchDTO tilesFetchDTO) throws IOException {
        return ResponseEntity.ok(tileDataService.getTilesByBandLevelAndIds(tilesFetchDTO));
    }

    @GetMapping("/tif/scene/{sceneId}/tileId/{tileId}")
    public ResponseEntity<byte[]> getTifBySceneAndTileId(@PathVariable String sceneId, @PathVariable String tileId) {
        byte[] tifData = tileDataService.getTileTifById(sceneId, tileId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("image/tiff"));
        return ResponseEntity.ok()
                .headers(headers)
                .body(tifData);
    }

    @PostMapping("/tif/tiles")
    public ResponseEntity<CommonResultVO> getMergedTifBySceneBandsAndTiles(@RequestBody TilesMergeDTO tilesMergeDTO) {
        return ResponseEntity.ok(tileDataService.getMergeTileTif(tilesMergeDTO));
    }

    @GetMapping("/description/sceneId/{sceneId}/tileId/{tileId}")
    public ResponseEntity<TileDesVO> getDescriptionByImageAndTileId(@PathVariable String sceneId, @PathVariable String tileId) {
        return ResponseEntity.ok(tileDataService.getTileDescriptionById(sceneId, tileId));
    }
}
