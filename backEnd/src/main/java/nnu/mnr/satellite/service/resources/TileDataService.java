package nnu.mnr.satellite.service.resources;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.dynamic.datasource.annotation.DS;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.model.dto.resources.TileBasicDTO;
import nnu.mnr.satellite.model.dto.resources.TilesFetchDTO;
import nnu.mnr.satellite.model.dto.resources.TilesMergeDTO;
import nnu.mnr.satellite.model.po.resources.Tile;
import nnu.mnr.satellite.model.pojo.modeling.ModelServerProperties;
import nnu.mnr.satellite.model.vo.common.GeoJsonVO;
import nnu.mnr.satellite.model.vo.resources.TileDesVO;
import nnu.mnr.satellite.repository.resources.IImageRepo;
import nnu.mnr.satellite.repository.resources.ITileRepo;
import nnu.mnr.satellite.utils.common.HttpUtil;
import nnu.mnr.satellite.utils.common.ProcessUtil;
import nnu.mnr.satellite.utils.data.MinioUtil;
import nnu.mnr.satellite.utils.data.RedisUtil;
import nnu.mnr.satellite.utils.geom.GeometryUtil;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
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

    private final ITileRepo tileRepo;

    public TileDataService(ITileRepo tileRepo) {
        this.tileRepo = tileRepo;
    }

    @Autowired
    MinioUtil minioUtil;

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    ModelMapper tileModelMapper;


    @Autowired
    ModelServerProperties modelServerProperties;

    @Autowired
    ImageDataService imageDataService;

    public GeoJsonVO getTilesBySceneAndLevel(String sceneId, String tileLevel) throws IOException {
        String band = imageDataService.getImagesBySceneId(sceneId).get(0).getBand();
        List<Tile> tiles = tileRepo.getTileByBandAndLevel(sceneId, band, tileLevel);
        return GeometryUtil.tileList2GeojsonVO(tiles);
    }

    private String getBasicTilesBySceneLevelAndBBox(TilesFetchDTO tilesFetchDTO) {
        String sceneId = tilesFetchDTO.getSceneId(), tileLevel = tilesFetchDTO.getTileLevel();
        JSONObject geometry = tilesFetchDTO.getGeometry();
        JSONArray coordinates = geometry.getJSONArray("coordinates");
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        Geometry bbox = GeometryUtil.parse4326Polygon(coordinates, geometryFactory);
        String wkt = bbox.toText();
        List<TileBasicDTO> tileBasics = tileRepo.getBasicTileByBandAndLevel(sceneId, tileLevel, wkt);
        JSONObject requestParam = JSONObject.of("sceneId", sceneId, "tiles", tileBasics);
        String mergeUrl = modelServerProperties.getApis().get("merge");
        String modelCaseId = ProcessUtil.runModelCase(mergeUrl, requestParam);
        JSONObject modelCase = JSONObject.of("status", "running", "result", null);
        redisUtil.addJsonDataWithExpiration(modelCaseId, modelCase, 60 * 10);
        return modelCaseId;
    }

    public byte[] getTileTifById(String sceneId, String tileId) {
        Tile tile = tileRepo.getTileByTileId(sceneId, tileId);
        return minioUtil.downloadByte(tile.getBucket(), tile.getPath());
    }

    public String getMergeTileTif(TilesMergeDTO tilesMergeDTO) {
        JSONObject mergeParam = JSONObject.of("sceneId", tilesMergeDTO.getSceneId(),"tiles", tilesMergeDTO.getTiles());
        String mergeUrl = modelServerProperties.getAddress() + modelServerProperties.getApis().get("merge");
        String modelCaseId = ProcessUtil.runModelCase(mergeUrl, mergeParam);
        JSONObject modelCase = JSONObject.of("status", "running", "result", null);
        redisUtil.addJsonDataWithExpiration(modelCaseId, modelCase, 60 * 10);
        return modelCaseId;
    }

    public TileDesVO getTileDescriptionById(String sceneId, String tileId) {
        Tile tile = tileRepo.getTileByTileId(sceneId, tileId);
        return tileModelMapper.map(tile, new TypeToken<TileDesVO>() {}.getType());
    }

}
