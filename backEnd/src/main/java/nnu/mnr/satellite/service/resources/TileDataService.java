package nnu.mnr.satellite.service.resources;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.dynamic.datasource.annotation.DS;
import nnu.mnr.satellite.model.dto.common.GeoJsonDTO;
import nnu.mnr.satellite.model.dto.resources.ImageInfoDTO;
import nnu.mnr.satellite.model.dto.resources.TileDesDTO;
import nnu.mnr.satellite.model.po.resources.Tile;
import nnu.mnr.satellite.repository.resources.ITileRepo;
import nnu.mnr.satellite.utils.GeometryUtil;
import nnu.mnr.satellite.utils.MinioUtil;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
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
    ModelMapper tileModelMapper;

    @Autowired
    MinioUtil minioUtil;

    private final ITileRepo tileRepo;

    public TileDataService(ITileRepo tileRepo) {
        this.tileRepo = tileRepo;
    }

    @DS("mysql_tile")
    public GeoJsonDTO getTilesByImageAndLevel(String imageId, int tileLevel) throws IOException {
        List<Tile> tiles = tileRepo.getTileByImageIdAndLevel(imageId, tileLevel);
        return GeometryUtil.tileList2GeojsonDTO(tiles);
    }

    @DS("mysql_tile")
    public byte[] getTileTifById(String imageId, String tileId) {
        Tile tile = tileRepo.getTileByTileId(imageId, tileId);
        return minioUtil.downloadByte(tile.getBucket(), tile.getPath());
    }

    @DS("mysql_tile")
    public TileDesDTO getTileDescriptionById(String imageId, String tileId) {
        Tile tile = tileRepo.getTileByTileId(imageId, tileId);
        return tileModelMapper.map(tile, new TypeToken<TileDesDTO>() {}.getType());
    }

}
