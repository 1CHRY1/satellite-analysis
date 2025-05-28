package nnu.mnr.satellite.service.resources;

import com.alibaba.fastjson2.JSONObject;
import nnu.mnr.satellite.model.po.geo.GeoLocation;
import nnu.mnr.satellite.model.vo.resources.RegionWindowVO;
import nnu.mnr.satellite.repository.resources.LocationRepoImpl;
import nnu.mnr.satellite.utils.geom.GeometryUtil;
import nnu.mnr.satellite.utils.geom.TileCalculateUtil;
import org.locationtech.jts.geom.Geometry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
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

    public JSONObject getLocationGeojsonBoundary(Integer resolution, String id) throws IOException {
        return GeometryUtil.geometry2Geojson(getLocationBoundary(resolution, id));
    }

    public Geometry getLocationBoundary(Integer resolution, String id) {
        GeoLocation location = locationRepo.searchById(id);
        double lon = Double.parseDouble(location.getWgs84Lon());
        double lat = Double.parseDouble(location.getGcj02Lat());
        int[] grid = TileCalculateUtil.getGridXYByLngLatAndResolution(lon, lat, resolution);
        Geometry leftTop = TileCalculateUtil.getTileGeomByIdsAndResolution(grid[1]-1, grid[0]-1, resolution);
        Geometry rightBottom = TileCalculateUtil.getTileGeomByIdsAndResolution(grid[1]+1, grid[0]+1, resolution);
        Geometry rightTop = TileCalculateUtil.getTileGeomByIdsAndResolution(grid[1]-1, grid[0]+1, resolution);
        Geometry leftBottom = TileCalculateUtil.getTileGeomByIdsAndResolution(grid[1]+1, grid[0]-1, resolution);
        return leftBottom.union(rightBottom).union(leftTop).union(rightTop).getEnvelope();
    }

    public RegionWindowVO getLocationWindowById(Integer resolution, String id) {
        GeoLocation location = locationRepo.searchById(id);
        double lon = Double.parseDouble(location.getWgs84Lon());
        double lat = Double.parseDouble(location.getGcj02Lat());
        int[] grid = TileCalculateUtil.getGridXYByLngLatAndResolution(lon, lat, resolution);
        Geometry leftTop = TileCalculateUtil.getTileGeomByIdsAndResolution(grid[1]-1, grid[0]-1, resolution);
        Geometry rightBottom = TileCalculateUtil.getTileGeomByIdsAndResolution(grid[1]+1, grid[0]+1, resolution);
        List<Double> boundary = GeometryUtil.getGeometryBounds(leftTop.union(rightBottom).getBoundary());
        return RegionWindowVO.builder()
                .center(List.of(lon, lat)).bounds(boundary).build();
    }
}