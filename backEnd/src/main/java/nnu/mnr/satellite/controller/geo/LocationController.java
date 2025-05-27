package nnu.mnr.satellite.controller.geo;

import nnu.mnr.satellite.model.po.geo.GeoLocation;
import nnu.mnr.satellite.service.geo.LocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/5/26 21:33
 * @Description:
 */

@RestController
@RequestMapping("/api/v1/geo/location")
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

}
