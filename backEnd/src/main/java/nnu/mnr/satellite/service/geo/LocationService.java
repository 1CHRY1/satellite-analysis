package nnu.mnr.satellite.service.geo;

import nnu.mnr.satellite.model.po.geo.GeoLocation;
import nnu.mnr.satellite.repository.geo.LocationRepoImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/5/26 20:21
 * @Description:
 */

@Service
public class LocationService {

    @Autowired
    private LocationRepoImpl locationRepo;

    public List<GeoLocation> searchByNameContaining(String keyword) {
        return locationRepo.searchByName(keyword);
    }

    public GeoLocation searchById(String id) {
        return locationRepo.searchById(id);
    }
}