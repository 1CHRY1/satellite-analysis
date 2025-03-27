package nnu.mnr.satellite.controller.resources;

import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.model.dto.resources.TilesMergeDTO;
import nnu.mnr.satellite.model.vo.common.GeoJsonVO;
import nnu.mnr.satellite.model.vo.resources.TileDesVO;
import nnu.mnr.satellite.service.resources.TileDataService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/12 9:30
 * @Description:
 */

@RestController
@RequestMapping("api/v2/data/tile")
@Slf4j
public class TileControllerV2 {

    private final TileDataService tileDataService;

    public TileControllerV2(TileDataService tileDataService) {
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

    @GetMapping("/tif/scene/{sceneId}/tileId/{tileId}")
    public ResponseEntity<byte[]> getTifBySceneAndTileId(@PathVariable String sceneId, @PathVariable String tileId) {
        byte[] tifData = tileDataService.getTileTifById(sceneId, tileId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("image/tiff"));
        return ResponseEntity.ok()
                .headers(headers)
                .body(tifData);
    }

    @PostMapping("/tif/tileIds")
    public ResponseEntity<byte[]> getMergedTifBySceneAndTileId(@RequestBody TilesMergeDTO tilesMergeDTO) {
        byte[] tifData = tileDataService.getMergeTileTif(tilesMergeDTO);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("image/tiff"));
        return ResponseEntity.ok()
                .headers(headers)
                .body(tifData);
    }

    @GetMapping("/description/sceneId/{sceneId}/tileId/{tileId}")
    public ResponseEntity<TileDesVO> getDescriptionByImageAndTileId(@PathVariable String sceneId, @PathVariable String tileId) {
        return ResponseEntity.ok(tileDataService.getTileDescriptionById(sceneId, tileId));
    }
}
