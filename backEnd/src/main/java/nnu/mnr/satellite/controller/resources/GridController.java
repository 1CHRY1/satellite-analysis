package nnu.mnr.satellite.controller.resources;

import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.model.dto.resources.GridSceneFetchDTO;
import nnu.mnr.satellite.model.dto.resources.GridVectorFetchDTO;
import nnu.mnr.satellite.model.vo.resources.GridBoundaryVO;
import nnu.mnr.satellite.model.vo.resources.GridSceneVO;
import nnu.mnr.satellite.model.vo.resources.GridVectorVO;
import nnu.mnr.satellite.service.resources.GridDataService;
import nnu.mnr.satellite.service.resources.RegionDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/5/13 15:31
 * @Description:
 */

@RestController
@RequestMapping("api/v1/data/grid")
@Slf4j
public class GridController {

    private final GridDataService gridDataService;

    @Autowired
    private RegionDataService regionDataService;

    public GridController(GridDataService gridDataService) {
        this.gridDataService = gridDataService;
    }

    @GetMapping("/grids/region/{regionId}/resolution/{resolution}")
    public ResponseEntity<List<GridBoundaryVO>> getGridsByRegionAndResolution(@PathVariable Integer regionId, @PathVariable Integer resolution) throws IOException {
        return ResponseEntity.ok(regionDataService.getGridsByRegionAndResolution(regionId, resolution));
    }

    @GetMapping("/grids/location/{locationId}/resolution/{resolution}")
    public ResponseEntity<List<GridBoundaryVO>> getGridsFromLocation(@PathVariable String locationId, @PathVariable Integer resolution) throws IOException {
        return ResponseEntity.ok(gridDataService.getGridsByLocationId(locationId, resolution));
    }

    @PostMapping("/scene/grids")
    public ResponseEntity<List<GridSceneVO>> getScenesFromGrids(@RequestBody GridSceneFetchDTO gridSceneFetchDTO) {
        return ResponseEntity.ok(gridDataService.getScenesFromGrids(gridSceneFetchDTO));
    }

    @PostMapping("/vector/grids")
    public ResponseEntity<List<GridVectorVO>> getVectorsFromGrids(@RequestBody GridVectorFetchDTO gridVectorFetchDTO) {
        return ResponseEntity.ok(gridDataService.getVectorsFromGrids(gridVectorFetchDTO));
    }

}
