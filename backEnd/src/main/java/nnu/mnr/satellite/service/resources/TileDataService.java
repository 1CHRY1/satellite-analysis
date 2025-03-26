package nnu.mnr.satellite.service.resources;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.dynamic.datasource.annotation.DS;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.model.dto.resources.TilesMergeDTO;
import nnu.mnr.satellite.model.pojo.modeling.ModelServerProperties;
import nnu.mnr.satellite.model.vo.common.GeoJsonVO;
import nnu.mnr.satellite.model.vo.resources.TileDesVO;
import nnu.mnr.satellite.model.po.resources.Tile;
import nnu.mnr.satellite.repository.resources.ITileRepo;
import nnu.mnr.satellite.utils.common.HttpUtil;
import nnu.mnr.satellite.utils.geom.GeometryUtil;
import nnu.mnr.satellite.utils.data.MinioUtil;
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

@Slf4j
@Service("TileDataService")
public class TileDataService {

    @Autowired
    ModelMapper tileModelMapper;

    @Autowired
    MinioUtil minioUtil;

    @Autowired
    ModelServerProperties modelServerProperties;

    private final ITileRepo tileRepo;

    public TileDataService(ITileRepo tileRepo) {
        this.tileRepo = tileRepo;
    }

    @DS("mysql_tile")
    public GeoJsonVO getTilesByImageAndLevel(String imageId, int tileLevel) throws IOException {
        List<Tile> tiles = tileRepo.getTileByImageIdAndLevel(imageId, tileLevel);
        return GeometryUtil.tileList2GeojsonVO(tiles);
    }

    @DS("mysql_tile")
    public byte[] getTileTifById(String imageId, String tileId) {
        Tile tile = tileRepo.getTileByTileId(imageId, tileId);
        return minioUtil.downloadByte(tile.getBucket(), tile.getPath());
    }

    public byte[] getMergeTileTif(TilesMergeDTO tilesMergeDTO) {
        JSONObject mergeParam = JSONObject.of("imageId", tilesMergeDTO.getImageId(),"tiles", tilesMergeDTO.getTiles());
        try {
            String mergeApi = modelServerProperties.getAddress() + modelServerProperties.getApis().get("merge");
            // TODO: Turn to Model Task
            JSONObject fileLocation = JSONObject.parseObject(HttpUtil.doPost(mergeApi, mergeParam));
            String bucket = fileLocation.getString("bucket");
            String path = fileLocation.getString("path");
            return minioUtil.downloadByte(bucket, path);
        } catch (Exception e) {
            return null;
        }
    }

    @DS("mysql_tile")
    public TileDesVO getTileDescriptionById(String imageId, String tileId) {
        Tile tile = tileRepo.getTileByTileId(imageId, tileId);
        return tileModelMapper.map(tile, new TypeToken<TileDesVO>() {}.getType());
    }

}
