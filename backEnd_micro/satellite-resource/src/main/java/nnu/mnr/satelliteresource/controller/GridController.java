package nnu.mnr.satelliteresource.controller;

import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satelliteresource.model.dto.resources.GridSceneFetchDTO;
import nnu.mnr.satelliteresource.model.vo.resources.GridSceneVO;
import nnu.mnr.satelliteresource.service.GridDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    public GridController(GridDataService gridDataService) {
        this.gridDataService = gridDataService;
    }

    @PostMapping("/scene/grids")
    public ResponseEntity<List<GridSceneVO>> getScenesFromGrids(@RequestBody GridSceneFetchDTO gridSceneFetchDTO) {
        return ResponseEntity.ok(gridDataService.getScenesFromGrids(gridSceneFetchDTO));
    }

}
