package nnu.mnr.satellite.controller;

import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.model.po.Product;
import nnu.mnr.satellite.model.po.Tile;
import nnu.mnr.satellite.service.ProductDataService;
import nnu.mnr.satellite.service.TileDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/{imageId}/{tileLevel}")
    public ResponseEntity<List<Tile>> GetAllData(@PathVariable String imageId, @PathVariable int tileLevel) {
        return ResponseEntity.ok(tileDataService.getTileByImageAndLevel(imageId, tileLevel));
    }

}
