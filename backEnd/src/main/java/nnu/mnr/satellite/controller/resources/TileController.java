package nnu.mnr.satellite.controller.resources;

import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.model.dto.resources.TilesFetchDTO;
import nnu.mnr.satellite.model.dto.resources.TilesMergeDTO;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.model.vo.common.GeoJsonVO;
import nnu.mnr.satellite.model.vo.resources.TileDesVO;
import nnu.mnr.satellite.model.vo.resources.TilesFetchVO;
import nnu.mnr.satellite.service.resources.TileDataService;
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
//        return ResponseEntity.ok(tileDataService.getTilesBySceneAndLevel(sceneId, tileLevel));
        LocalDateTime beforetime = LocalDateTime.now();
        GeoJsonVO geoJsonVO = tileDataService.getTilesBySceneAndLevel(sceneId, tileLevel);
        LocalDateTime afterTime = LocalDateTime.now();
        System.out.println(Duration.between(beforetime, afterTime));
        return ResponseEntity.ok(geoJsonVO);
    }

    @PostMapping("/tiler/tiles")
    public ResponseEntity<List<TilesFetchVO>> getTilesByLevelBandAndIds(@RequestBody TilesFetchDTO tilesFetchDTO) throws IOException {
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
