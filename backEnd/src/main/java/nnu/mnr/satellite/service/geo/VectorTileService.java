package nnu.mnr.satellite.service.geo;

import nnu.mnr.satellite.mapper.geo.IVectorTileMapper;
import nnu.mnr.satellite.service.resources.RegionDataService;
import org.locationtech.jts.geom.Geometry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/5/27 11:32
 * @Description:
 */

@Service
public class VectorTileService {

    @Autowired
    RegionDataService regionDataService;

    private final IVectorTileMapper geoDataMapper;

    public VectorTileService(IVectorTileMapper geoDataMapper) {
        this.geoDataMapper = geoDataMapper;
    }

    public byte[] getGeoVecterTiles(String layerName, int z, int x, int y) {
        return (byte[]) geoDataMapper.getVectorTile(layerName, x, y, z);
    }

    public byte[] getGeoVecterTilesByBetweenParamAndWithinRegion(String layerName, int z, int x, int y, String param, Integer valueStart, Integer valueEnd, String wkt) {
        return (byte[]) geoDataMapper.getVectorTileByBetweenParam(layerName, x, y, z, param, valueStart, valueEnd, wkt);
    }

    public byte[] getPatchGeoVecterTilesByParam(String layerName, int z, int x, int y, String type, Integer regionId) {
        String wkt = regionDataService.getRegionById(regionId).getBoundary().toText();
        int valueStart = Integer.MIN_VALUE, valueEnd = Integer.MAX_VALUE;
        switch (type) {
            case "farm" -> {
                valueStart = 0;
                valueEnd = 20;
            }
            case "forest" -> {
                valueStart = 20;
                valueEnd = 30;
            }
            case "grass" -> {
                valueStart = 30;
                valueEnd = 40;
            }
            case "water" -> {
                valueStart = 40;
                valueEnd = 50;
            }
            case "city" -> {
                valueStart = 50;
                valueEnd = 60;
            }
            case "inuse" -> {
                valueStart = 60;
                valueEnd = 70;
            }
            case "ocean" -> {
                valueStart = 70;
                valueEnd = 100;
            }
            default -> valueStart = 100;
        }
        return getGeoVecterTilesByBetweenParamAndWithinRegion(layerName, z, x, y, "shandong_i", valueStart, valueEnd, wkt);
    }

}
