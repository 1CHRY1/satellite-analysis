package nnu.mnr.satelliteresource.controller;

import com.alibaba.fastjson2.JSONObject;
import nnu.mnr.satelliteresource.model.vo.resources.GridBoundaryVO;
import nnu.mnr.satelliteresource.model.vo.resources.RegionInfoVO;
import nnu.mnr.satelliteresource.model.vo.resources.RegionWindowVO;
import nnu.mnr.satelliteresource.service.RegionDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/5/8 20:56
 * @Description:
 */

@RestController
@RequestMapping("api/v1/data/region")
public class RegionController {

    private final RegionDataService regionDataService;

    public RegionController(RegionDataService regionDataService) {
        this.regionDataService = regionDataService;
    }

    @GetMapping("/level/{level}")
    public ResponseEntity<List<RegionInfoVO>> getRegionList(@PathVariable String level) {
        return ResponseEntity.ok(regionDataService.getRegionsByLevel(level));
    }

    @GetMapping("/parent/{parent}")
    public ResponseEntity<List<RegionInfoVO>> getRegionListByLevelAndParent(@PathVariable Integer parent) {
        return ResponseEntity.ok(regionDataService.getRegionsByParentAndType(parent));
    }

    @GetMapping("/boundary/{regionId}")
    public ResponseEntity<JSONObject> getRegionBoundaryById(@PathVariable Integer regionId) throws IOException {
        return ResponseEntity.ok(regionDataService.getRegionBoundaryById(regionId));
    }

    @GetMapping("/window/region/{regionId}")
    public ResponseEntity<RegionWindowVO> getRegionWindow(@PathVariable Integer regionId) {
        return ResponseEntity.ok(regionDataService.getRegionWindowById(regionId));
    }

    @GetMapping("/grids/region/{regionId}/resolution/{resolution}")
    public ResponseEntity<List<GridBoundaryVO>> getGridsByRegionAndResolution(@PathVariable Integer regionId, @PathVariable Integer resolution) throws IOException {
        return ResponseEntity.ok(regionDataService.getGridsByRegionAndResolution(regionId, resolution));
    }


}
