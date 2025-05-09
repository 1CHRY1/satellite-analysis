package nnu.mnr.satellite.controller.resources;

import nnu.mnr.satellite.model.po.resources.Region;
import nnu.mnr.satellite.model.vo.resources.RegionInfoVO;
import nnu.mnr.satellite.service.resources.RegionDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @Autowired
    RegionDataService regionDataService;

    @GetMapping("/level/{level}")
    public ResponseEntity<List<RegionInfoVO>> getRegionList(@PathVariable String level) {
        return ResponseEntity.ok(regionDataService.getRegionsByLevel(level));
    }

    @GetMapping("/level/{level}/parent/{parent}")
    public ResponseEntity<List<RegionInfoVO>> getRegionListByLevelAndParent(@PathVariable String level, @PathVariable Integer parent) {
        return ResponseEntity.ok(regionDataService.getRegionsByParentAndType(level, parent));
    }

}
