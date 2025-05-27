package nnu.mnr.satellite.service.resources;

import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.jobs.QuartzSchedulerManager;
import nnu.mnr.satellite.model.dto.resources.TilesFetchDTO;
import nnu.mnr.satellite.model.dto.resources.TilesMergeDTO;
import nnu.mnr.satellite.model.po.resources.Scene;
import nnu.mnr.satellite.model.po.resources.Tile;
import nnu.mnr.satellite.model.pojo.modeling.ModelServerProperties;
import nnu.mnr.satellite.model.pojo.modeling.TilerProperties;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.model.vo.common.GeoJsonVO;
import nnu.mnr.satellite.model.vo.resources.TileDesVO;
import nnu.mnr.satellite.model.vo.resources.TilesFetchResultVO;
import nnu.mnr.satellite.mapper.resources.ITileRepo;
import nnu.mnr.satellite.utils.common.ProcessUtil;
import nnu.mnr.satellite.utils.data.MinioUtil;
import nnu.mnr.satellite.utils.data.RedisUtil;
import nnu.mnr.satellite.utils.geom.GeometryUtil;
import nnu.mnr.satellite.utils.geom.TileCalculateUtil;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

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
    QuartzSchedulerManager quartzSchedulerManager;

    @Autowired
    ModelServerProperties modelServerProperties;

    @Autowired
    TilerProperties tilerProperties;

    @Autowired
    ImageDataService imageDataService;

    @Autowired
    SceneDataService sceneDataService;


    public GeoJsonVO getTilesBySceneAndLevel(String sceneId, String tileLevel) throws IOException {
        String band = imageDataService.getImagesBySceneId(sceneId).get(0).getBand();
        List<Tile> tiles = tileRepo.getTileByBandAndLevel(sceneId, band, tileLevel);
        return GeometryUtil.tileList2GeojsonVO(tiles);
    }

    // For Getting Different Tiles from Different Scenes by TileArea
//    public List<TilesFetchVO> getTilesByBandLevelAndIds(TilesFetchDTO tilesFetchDTO) throws IOException {
//        String sensorId = tilesFetchDTO.getSensorId(); String productId = tilesFetchDTO.getProductId();
//        String rowId = tilesFetchDTO.getRowId(); String columnId = tilesFetchDTO.getColumnId();
//        String band = tilesFetchDTO.getBand(); String level = tilesFetchDTO.getTileLevel();
//        List<Scene> scenes = sceneDataService.getScenesByBBox(sensorId, productId, tilesFetchDTO.getGeometry());
//        Function<Scene, TilesFetchVO> mapper = scene -> {
//            Tile tile = tileRepo.getTileDataByBandLevelAndIds(scene.getSceneId(), band, level, rowId, columnId);
//            return TilesFetchVO.tilesFetcherBuilder()
//                    .tileId(tile.getTileId())
//                    .cloud(tile.getCloud())
//                    .tilerUrl(tilerProperties.getEndPoint())
//                    .object(tile.getBucket() + tile.getPath())
//                    .build();
//        };
//        return ConcurrentUtil.processConcurrently(scenes, mapper);
//    }

    public List<TilesFetchResultVO> getTilesByBandLevelAndIds(TilesFetchDTO tilesFetchDTO) throws IOException {
        String sensorId = tilesFetchDTO.getSensorId(); String productId = tilesFetchDTO.getProductId();
        Integer rowId = tilesFetchDTO.getRowId(); Integer columnId = tilesFetchDTO.getColumnId();
        String band = tilesFetchDTO.getBand(); String level = tilesFetchDTO.getTileLevel();
        int[] gridNums = TileCalculateUtil.getGridNumFromTileLevel(level);
        JSONObject tileGeometry = TileCalculateUtil.getTileGeoJsonByIds(rowId, columnId, gridNums[0], gridNums[1]);
        List<Scene> scenes = sceneDataService.getScenesByBBox(sensorId, productId, tileGeometry);
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(scenes.size(), 10));
        List<CompletableFuture<TilesFetchResultVO>> futures = scenes.stream()
                .map(scene -> fetchTileAsync(scene, band, level, rowId, columnId, executor))
                .toList();
        List<TilesFetchResultVO> tilesFetchRes = futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        executor.shutdown();
        return tilesFetchRes;
    }

    private CompletableFuture<TilesFetchResultVO> fetchTileAsync(Scene scene, String band, String level, Integer rowId, Integer columnId, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            String sceneTableName = scene.getSceneId(); String sceneTableNameLow = sceneTableName.toLowerCase();
            Tile tile;
            try {
                tile = tileRepo.getTileDataByBandLevelAndIds(sceneTableName, band, level, rowId, columnId);
            } catch (Exception e ) {
                tile = tileRepo.getTileDataByBandLevelAndIds(sceneTableNameLow, band, level, rowId, columnId);
            }
            return TilesFetchResultVO.tilesFetcherBuilder()
                    .tileId(tile.getTileId())
                    .cloud(tile.getCloud())
                    .tilerUrl(tilerProperties.getEndPoint())
                    .sceneId(scene.getSceneId())
                    .object(tile.getBucket() + "/" + tile.getPath())
                    .build();
        }, executor);
    }

    public byte[] getTileTifById(String sceneId, String tileId) {
        Tile tile = tileRepo.getTileByTileId(sceneId, tileId);
        return minioUtil.downloadByte(tile.getBucket(), tile.getPath());
    }

    public CommonResultVO getMergeTileTif(TilesMergeDTO tilesMergeDTO) {
        JSONObject mergeParam = JSONObject.of("tiles", tilesMergeDTO.getTiles(), "bands", tilesMergeDTO.getBands());
        String mergeUrl = modelServerProperties.getAddress() + modelServerProperties.getApis().get("merge");
        try {
            JSONObject modelCaseResponse = JSONObject.parseObject(ProcessUtil.runModelCase(mergeUrl, mergeParam));
            String caseId = modelCaseResponse.getJSONObject("data").getString("taskId");
            quartzSchedulerManager.startModelRunningStatusJob(caseId);
            JSONObject modelCase = JSONObject.of("status", "RUNNING", "start", LocalDateTime.now());
            redisUtil.addJsonDataWithExpiration(caseId, modelCase, 60 * 10);
            return CommonResultVO.builder().status(1).message("success").data(caseId).build();
        } catch (Exception e) {
            return CommonResultVO.builder().status(-1).message("Wrong Because of " + e.getMessage()).build();
        }
    }

    public TileDesVO getTileDescriptionById(String sceneId, String tileId) {
        Tile tile = tileRepo.getTileByTileId(sceneId, tileId);
        return tileModelMapper.map(tile, new TypeToken<TileDesVO>() {}.getType());
    }

}
