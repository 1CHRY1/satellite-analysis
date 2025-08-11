package nnu.mnr.satellite.service.modeling;

import com.alibaba.fastjson2.JSONObject;
import javassist.expr.NewArray;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.cache.SceneDataCache;
import nnu.mnr.satellite.jobs.QuartzSchedulerManager;
import nnu.mnr.satellite.model.dto.modeling.ModelServerImageDTO;
import nnu.mnr.satellite.model.dto.modeling.ModelServerSceneDTO;
import nnu.mnr.satellite.model.dto.modeling.VisualizationLowLevelTile;
import nnu.mnr.satellite.model.dto.modeling.VisualizationTileDTO;
import nnu.mnr.satellite.model.pojo.modeling.ModelServerProperties;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.model.vo.modeling.NoCloudConfigVO;
import nnu.mnr.satellite.model.vo.resources.GridsAndGridsBoundary;
import nnu.mnr.satellite.model.vo.resources.SceneDesVO;
import nnu.mnr.satellite.service.common.BandMapperGenerator;
import nnu.mnr.satellite.service.resources.ImageDataService;
import nnu.mnr.satellite.service.resources.RegionDataService;
import nnu.mnr.satellite.service.resources.SceneDataServiceV3;
import nnu.mnr.satellite.utils.common.ProcessUtil;
import nnu.mnr.satellite.utils.dt.RedisUtil;
import nnu.mnr.satellite.utils.geom.GeometryUtil;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import nnu.mnr.satellite.utils.dt.MinioUtil;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service("ModelExampleServiceV3")
public class ModelExampleServiceV3 {

    @Autowired
    MinioUtil minioUtil;
    @Autowired
    BandMapperGenerator bandMapperGenerator;
    @Autowired
    ImageDataService imageDataService;
    @Autowired
    SceneDataServiceV3 sceneDataService;
    @Autowired
    RegionDataService regionDataService;
    @Autowired
    QuartzSchedulerManager quartzSchedulerManager;
    @Autowired
    ModelServerProperties modelServerProperties;
    @Autowired
    RedisUtil redisUtil;

    public CommonResultVO createScenesVisualizationConfig(VisualizationTileDTO visualizationTileDTO, String cacheKey) {
        SceneDataCache.printAllCacheContents();
        SceneDataCache.UserSceneCache userSceneCache = SceneDataCache.getUserSceneCacheMap(cacheKey);
        SceneDataCache.UserRegionInfoCache userRegionInfoCache = SceneDataCache.getUserRegionInfoCacheMap(cacheKey);
        // 1、先求瓦片tileBoundingBox，并判断与格网边界是否相交
        Geometry gridBoundary = userRegionInfoCache.gridsBoundary;
        List<Float> points = visualizationTileDTO.getPoints();
        Geometry tileBoundingBox = GeometryUtil.pointsConvertToPolygon(points);
        if (userSceneCache == null) {
            return CommonResultVO.builder()
                    .message("No corresponding data found, please log in again or retrieve the data")
                    .data(cacheKey.split("_")[1])
                    .status(-1)
                    .build();
        } else if (!gridBoundary.intersects(tileBoundingBox)) {
            return CommonResultVO.builder()
                    .message("Tiles out of gridsBoundary")
                    .status(-1)
                    .build();
        } else{
            // 构建JSON配置
            String sensorName = visualizationTileDTO.getSensorName();
            JSONObject configJson = buildScenesVisualizationConfig(tileBoundingBox, sensorName, userSceneCache);
            return CommonResultVO.builder().status(1).message("success").data(configJson).build();
        }
    }

    // 构建影像可视化配置JSON
    private JSONObject buildScenesVisualizationConfig(Geometry tileBoundingBox, String sensorName, SceneDataCache.UserSceneCache userSceneCache) {

        // 2、用空间索引筛选瓦片周围的景
        List<SceneDesVO> scenesInfo = userSceneCache.queryCandidateScenes(tileBoundingBox);
        // 3、根据sensorName进一步筛选景，同时获取bandMapper（最后才用上）
        JSONObject bandMapper = bandMapperGenerator.getSatelliteConfigBySensorName(sensorName);
        List<SceneDesVO> scenes = new ArrayList<>();
        for (SceneDesVO sceneDesVO : scenesInfo) {
            if (sensorName.equals(sceneDesVO.getSensorName())) {
                scenes.add(sceneDesVO);
            }
        }
        // 按云量升序排列
        scenes.sort(Comparator.comparing(SceneDesVO::getCloud));

        // 记录开始时间
        long startTime = System.nanoTime();
        List<NoCloudConfigVO> scenesConfig = new ArrayList<>();
        GeometryFactory geometryFactory = new GeometryFactory();
        Geometry unionCoverage = geometryFactory.createMultiPolygon(new Polygon[]{});
        for (SceneDesVO scene : scenes) {
            // path
            List<ModelServerImageDTO> imageDTO = imageDataService.getModelServerImageDTOBySceneId(scene.getSceneId());
            JSONObject paths = new JSONObject();
            for (ModelServerImageDTO image : imageDTO) {
                paths.put("band_" + image.getBand(), image.getTifPath());
            }
            // coverage
            AbstractMap.SimpleEntry<Double, Geometry> coverageInfo = sceneDataService.calculateCoveragePercentage(scene.getBoundingBox(), tileBoundingBox);
            double coverage = coverageInfo.getKey();
            Geometry coverageGeometry = coverageInfo.getValue();
            // 已覆盖区域覆盖了新覆盖区域90%以上，跳过这一个新景
            if (coverage > 0.0 && sceneDataService.calculateCoveragePercentage(unionCoverage, coverageGeometry).getKey() < 0.9) {
                unionCoverage = unionCoverage.union(coverageGeometry);
                NoCloudConfigVO noCloudConfigVO = new NoCloudConfigVO();
                noCloudConfigVO.setSensorName(sensorName);
                noCloudConfigVO.setSceneId(scene.getSceneId());
                noCloudConfigVO.setSceneName(scene.getSceneName());
                noCloudConfigVO.setCloudPath(scene.getCloudPath());
                noCloudConfigVO.setBucket(scene.getBucket());
                noCloudConfigVO.setPath(paths);
                noCloudConfigVO.setResolution(scene.getResolution());
                noCloudConfigVO.setNoData(scene.getNoData().toString());
                noCloudConfigVO.setCloud(scene.getCloud());
                noCloudConfigVO.setCoverage(coverage);
                try {
                    noCloudConfigVO.setBbox(GeometryUtil.geometry2Geojson(scene.getBoundingBox()));
                }catch (IOException e) {
                    throw new RuntimeException("Failed to convert geometry to GeoJSON", e);
                }
                scenesConfig.add(noCloudConfigVO);
            }
        }
        // 记录结束时间并打印耗时
        long endTime = System.nanoTime();
        long durationMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
        System.out.println("筛选影像可视化景数据运行时间: " + durationMs + " ms，景总数为：" + scenesConfig.size() + "景");
        // 按覆盖率降序排列
        scenesConfig.sort((a, b) -> Double.compare(b.getCoverage(), a.getCoverage()));
        JSONObject result = new JSONObject();
        result.put("bandMapper", bandMapper);
        result.put("scenesConfig", scenesConfig);
        return result;
    }

    // 单个专题影像可视化
    public CommonResultVO createThemeVisualizationConfig(String themeName, String cacheKey){
        SceneDataCache.UserThemeCache userThemeCache = SceneDataCache.getUserThemeCacheMap(cacheKey);
        if (userThemeCache == null) {
            return CommonResultVO.builder()
                    .message("No corresponding data found, please log in again or retrieve the data")
                    .data(cacheKey.split("_")[1])
                    .status(-1)
                    .build();
        }
        List<SceneDesVO> scenesInfo = userThemeCache.scenesInfo;
        SceneDesVO targetScene = new SceneDesVO();
        for (SceneDesVO sceneDesVO : scenesInfo) {
            if (sceneDesVO.getSceneName().equals(themeName)) {
                targetScene = sceneDesVO;
                break;
            }
        }

        ModelServerSceneDTO modelServerSceneDTO = ModelServerSceneDTO.builder()
                .sceneId(targetScene.getSceneId())
                .noData(targetScene.getNoData())
                .bandMapper(bandMapperGenerator.getSatelliteConfigBySensorName(targetScene.getSensorName()))
                .images(imageDataService.getModelServerImageDTOBySceneId(targetScene.getSceneId())).build();

        return CommonResultVO.builder().status(1).message("success").data(modelServerSceneDTO).build();
    }

    //全国范围低zoom下的可视化
    public CommonResultVO createLowLevelScenesVisualizationConfig(VisualizationLowLevelTile visualizationLowLevelTile, Map<String, String> headers, Map<String, String> cookies) throws IOException {
        GridsAndGridsBoundary gridsAndGridsBoundary = regionDataService.getGridsByRegionAndResolution(100000, 150);
        String sensorName = visualizationLowLevelTile.getSensorName();
        String startTime = visualizationLowLevelTile.getStartTime();
        String endTime = visualizationLowLevelTile.getEndTime();
        String visualizationUrl = modelServerProperties.getAddress() + modelServerProperties.getApis().get("createLowLevelMosaic");
        JSONObject visualizationParam = JSONObject.of("sensorName", sensorName, "startTime", startTime, "endTime", endTime, "gridsAndGridsBoundary", gridsAndGridsBoundary);
        long expirationTime = 60 * 10;
        return runModelServerModel(visualizationUrl, visualizationParam, expirationTime, headers, cookies);
    }

    // 携带headers和cookies执行modelServer任务
    private CommonResultVO runModelServerModel(String url, JSONObject param, long expirationTime, Map<String, String> headers, Map<String, String> cookies) {
        try {
            JSONObject modelCaseResponse = JSONObject.parseObject(ProcessUtil.runModelCaseWithCookies(url, param, headers, cookies));
            String caseId = modelCaseResponse.getJSONObject("data").getString("taskId");
            quartzSchedulerManager.startModelRunningStatusJob(caseId, modelServerProperties);
            JSONObject modelCase = JSONObject.of("status", "RUNNING", "start", LocalDateTime.now());
            redisUtil.addJsonDataWithExpiration(caseId, modelCase, expirationTime);
            return CommonResultVO.builder().status(1).message("success").data(caseId).build();
        } catch (Exception e) {
            return CommonResultVO.builder().status(-1).message("Wrong Because of " + e.getMessage()).build();
        }
    }

}
