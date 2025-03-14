package nnu.mnr.satellite.service.resources;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import nnu.mnr.satellite.model.po.Tile;
import nnu.mnr.satellite.repository.ITileRepo;
import nnu.mnr.satellite.utils.GeometryUtil;
import nnu.mnr.satellite.utils.MinioUtil;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/12 9:28
 * @Description:
 */

@Service("TileDataService")
public class TileDataService {

    @Autowired
    MinioUtil minioUtil;

    private final ITileRepo tileRepo;

    public TileDataService(ITileRepo tileRepo) {
        this.tileRepo = tileRepo;
    }

    @DS("mysql_tile")
    public JSONArray getTilesByImageAndLevel(String imageId, int tileLevel) throws IOException {
        List<Tile> tiles = tileRepo.getTileByImageIdAndLevel(imageId, tileLevel);
        JSONArray tileGeoJsons = new JSONArray();
        for (Tile tile : tiles) {
            JSONObject tileGeoJson = JSONObject.parse(GeometryUtil.geometry2geojson(tile.getBbox(), tile.getTileId()));
            tileGeoJsons.add(JSONObject.of("tile", tileGeoJson));
        }
        return tileGeoJsons;
    }

    @DS("mysql_tile")
    public byte[] getTileTifById(String imageId, String tileId) {
        Tile tile = tileRepo.getTileByTileId(imageId, tileId);
        return minioUtil.downloadByte(tile.getBucket(), tile.getPath());
    }

    @DS("mysql_tile")
    public Tile getTileDescriptionById(String imageId, String tileId) {
        return tileRepo.getTileByTileId(imageId, tileId);
    }

}
