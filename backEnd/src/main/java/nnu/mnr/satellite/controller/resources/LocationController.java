package nnu.mnr.satellite.controller.resources;

import com.alibaba.fastjson2.JSONObject;
import nnu.mnr.satellite.model.po.geo.GeoLocation;
import nnu.mnr.satellite.model.vo.resources.ViewWindowVO;
import nnu.mnr.satellite.service.resources.LocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/5/26 21:33
 * @Description:
 */

@RestController
@RequestMapping("/api/v1/data/location")
public class LocationController {

    @Autowired
    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping("/name/{keyword}")
    public ResponseEntity<List<GeoLocation>> findByNameKeyWord(@PathVariable String keyword) {
        return ResponseEntity.ok(locationService.searchByNameContaining(keyword));
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<GeoLocation> searchById(@PathVariable String id) {
        return ResponseEntity.ok(locationService.searchById(id));
    }

    @GetMapping("/window/location/{locationId}/resolution/{resolution}")
    public ResponseEntity<ViewWindowVO> getRegionWindow(@PathVariable Integer resolution, @PathVariable String locationId) {
        return ResponseEntity.ok(locationService.getLocationWindowById(resolution, locationId));
    }

    @GetMapping("/boundary/location/{locationId}/resolution/{resolution}")
    public ResponseEntity<JSONObject> getLocationBoundary(@PathVariable Integer resolution, @PathVariable String locationId) throws IOException {
        return ResponseEntity.ok(locationService.getLocationGeojsonBoundary(resolution, locationId));
    }

}
