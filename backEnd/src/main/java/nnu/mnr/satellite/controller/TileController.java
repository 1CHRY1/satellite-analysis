package nnu.mnr.satellite.controller;

import com.alibaba.fastjson2.JSONArray;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.model.po.Tile;
import nnu.mnr.satellite.service.resources.TileDataService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
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

    @GetMapping("/imageId/{imageId}/tileLevel/{tileLevel}")
    public ResponseEntity<JSONArray> getTilesByImageAndLevel(@PathVariable String imageId, @PathVariable int tileLevel) throws IOException {
        return ResponseEntity.ok(tileDataService.getTilesByImageAndLevel(imageId, tileLevel));
    }

    @GetMapping("/tif/imageId/{imageId}/tileId/{tileId}")
    public ResponseEntity<byte[]> getTifByImageAndTileId(@PathVariable String imageId, @PathVariable String tileId) {
        byte[] tifData = tileDataService.getTileTifById(imageId, tileId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("image/tiff"));
        return ResponseEntity.ok()
                .headers(headers)
                .body(tifData);
    }

    @GetMapping("/description/imageId/{imageId}/tileId/{tileId}")
    public ResponseEntity<Tile> getDescriptionByImageAndTileId(@PathVariable String imageId, @PathVariable String tileId) {
        return ResponseEntity.ok(tileDataService.getTileDescriptionById(imageId, tileId));
    }
}
