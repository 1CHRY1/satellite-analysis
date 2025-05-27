package nnu.mnr.satellite.service.geo;

import nnu.mnr.satellite.mapper.geo.IVectorTileMapper;
import nnu.mnr.satellite.model.pojo.common.TileBox;
import nnu.mnr.satellite.utils.geom.TileUtil;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/5/27 11:32
 * @Description:
 */

@Service
public class VectorTileService {

    private final IVectorTileMapper geoDataMapper;

    public VectorTileService(IVectorTileMapper geoDataMapper) {
        this.geoDataMapper = geoDataMapper;
    }

    public byte[] getGeoVecterTiles(String layerName, int z, int x, int y) {
        TileBox tileBox = TileUtil.tile2boundingBox(
                x, y, z, layerName
        );
        return (byte[]) geoDataMapper.getVectorTile(tileBox);
    }

    public byte[] getGeoVecterTilesByParam(String param, String value, String layerName, int z, int x, int y) {
        TileBox tileBox = TileUtil.tile2boundingBox(
                x, y, z, layerName
        );
        return (byte[]) geoDataMapper.getVectorTileByParam(tileBox, param, value);
    }

    public byte[] getPatchGeoVecterTilesByParam(String value, String layerName, int z, int x, int y) {
        TileBox tileBox = TileUtil.tile2boundingBox(
                x, y, z, layerName
        );
        int valueMin = Integer.MIN_VALUE, valueMax = Integer.MAX_VALUE;
        switch (value) {
            case "farm" -> {
                valueMin = 0;
                valueMax = 20;
            }
            case "forest" -> {
                valueMin = 20;
                valueMax = 30;
            }
            case "grass" -> {
                valueMin = 30;
                valueMax = 40;
            }
            case "water" -> {
                valueMin = 40;
                valueMax = 50;
            }
            case "city" -> {
                valueMin = 50;
                valueMax = 60;
            }
            case "inuse" -> {
                valueMin = 60;
                valueMax = 70;
            }
            case "ocean" -> {
                valueMin = 70;
                valueMax = 100;
            }
            default -> valueMin = 100;
        }
        return (byte[]) geoDataMapper.getVectorTileByParam(tileBox, "shandong_i", value);
    }

}
