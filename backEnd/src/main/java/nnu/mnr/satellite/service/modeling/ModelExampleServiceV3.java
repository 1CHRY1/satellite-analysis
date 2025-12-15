package nnu.mnr.satellite.service.modeling;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.cache.EOCubeCache;
import nnu.mnr.satellite.cache.SceneDataCache;
import nnu.mnr.satellite.jobs.QuartzSchedulerManager;
import nnu.mnr.satellite.model.dto.cache.CacheEOCubeDTO;
import nnu.mnr.satellite.model.dto.modeling.*;
import nnu.mnr.satellite.model.po.resources.SceneSP;
import nnu.mnr.satellite.model.pojo.modeling.ModelServerProperties;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.model.vo.modeling.NoCloudConfigVO;
import nnu.mnr.satellite.model.vo.resources.GridsAndGridsBoundary;
import nnu.mnr.satellite.model.vo.resources.SceneDesVO;
import nnu.mnr.satellite.service.common.BandMapperGenerator;
import nnu.mnr.satellite.service.resources.*;
import nnu.mnr.satellite.utils.common.IdUtil;
import nnu.mnr.satellite.utils.common.ProcessUtil;
import nnu.mnr.satellite.utils.dt.RedisUtil;
import nnu.mnr.satellite.utils.geom.GeometryUtil;
import nnu.mnr.satellite.utils.geom.TileCalculateUtil;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import nnu.mnr.satellite.utils.dt.MinioUtil;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static nnu.mnr.satellite.utils.geom.GeometryUtil.getGridsBoundaryByTilesAndResolution;

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
    @Autowired
    SceneDataServiceV2 sceneDataServiceV2;
    @Autowired
    CaseDataService caseDataService;

    public CommonResultVO createScenesVisualizationConfig(VisualizationTileDTO visualizationTileDTO, String cacheKey) {
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
            if (coverage > 0.0 && sceneDataService.calculateCoveragePercentage(unionCoverage, coverageGeometry).getKey() < 1) {
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
        // TODO:
        GridsAndGridsBoundary gridsAndGridsBoundary = regionDataService.getGridsByRegionAndResolution(100000, 150);
        String sensorName = visualizationLowLevelTile.getSensorName();
        String startTime = visualizationLowLevelTile.getStartTime();
        String endTime = visualizationLowLevelTile.getEndTime();
        Integer regionId = visualizationLowLevelTile.getRegionId();
        String visualizationUrl = modelServerProperties.getAddress() + modelServerProperties.getApis().get("createLowLevelMosaic");
        JSONObject visualizationParam = JSONObject.of("sensorName", sensorName, "startTime", startTime, "endTime", endTime, "gridsAndGridsBoundary", gridsAndGridsBoundary, "regionId", regionId);
        long expirationTime = 60 * 10;
        return runModelServerModel(visualizationUrl, visualizationParam, expirationTime, headers, cookies);
    }

    // 时序立方体计算
    public CommonResultVO calcEOCube(EOCubeFetchDto eoCubeFetchDto, String userId) throws IOException {
        Integer regionId = eoCubeFetchDto.getRegionId();
        Integer resolution = eoCubeFetchDto.getResolution();
        List<String> sceneIds = eoCubeFetchDto.getSceneIds();
        List<String> bandList = eoCubeFetchDto.getBandList();
        if (bandList == null || bandList.isEmpty()) {
            bandList = Arrays.asList("Red", "Green", "Blue");
        }
        // 构成影像景参数信息
        List<ModelServerSceneDTO> modelServerSceneDTOs = new ArrayList<>();

        // 构成影像景参数信息
        for (String sceneId : sceneIds) {
            SceneSP scene = sceneDataServiceV2.getSceneByIdWithProductAndSensor(sceneId);
            List<ModelServerImageDTO> imageDTO = imageDataService.getModelServerImageDTOBySceneId(sceneId);
            ModelServerSceneDTO modelServerSceneDTO = ModelServerSceneDTO.builder()
                    .sceneId(sceneId).images(imageDTO).sceneTime(scene.getSceneTime()).resolution(scene.getResolution())
                    .bbox(GeometryUtil.geometry2Geojson(scene.getBbox())).noData(scene.getNoData())
                    .sensorName(scene.getSensorName()).productName(scene.getProductName()).cloud(scene.getCloud())
                    .bandMapper(bandMapperGenerator.getSatelliteConfigBySensorName(scene.getSensorName()))
                    .cloudPath(scene.getCloudPath()).bucket(scene.getBucket()).build();
            modelServerSceneDTOs.add(modelServerSceneDTO);
        }

        // 构成网格行列号信息
        String[] parts = eoCubeFetchDto.getDataSet().getCubeId().split("-");
        int rowId = Integer.parseInt(parts[0].trim());
        int columnId = Integer.parseInt(parts[1].trim());
        Integer[] tileId = new Integer[]{columnId, rowId};
        List<Integer[]> tileIds = new ArrayList<>();
        tileIds.add(tileId);

        // 构建行政区地址信息
        String address = regionDataService.getAddressById(regionId);
        switch (eoCubeFetchDto.getDataSet().getPeriod()) {
            case "month": {
                address += "(月度)";
                break;
            }
            case "season": {
                address += "(季度)";
                break;
            }
            case "year": {
                address += "(年度)";
                break;
            }
        }

        // 构建边界信息
        Geometry boundary = getGridsBoundaryByTilesAndResolution(tileIds, resolution);

        // 请求modelServer
        JSONObject calcEOCubeParam = JSONObject.of("tiles", tileIds, "scenes", modelServerSceneDTOs, "cloud", 0, "resolution", resolution, "bandList", bandList);
        calcEOCubeParam.put("resample", eoCubeFetchDto.getDataSet().getResample());
        calcEOCubeParam.put("period", eoCubeFetchDto.getDataSet().getPeriod());
        JSONObject caseJsonObj = JSONObject.of("boundary", boundary, "address", address, "resolution", resolution, "sceneIds", sceneIds, "dataSet", JSON.toJSONString(eoCubeFetchDto.getDataSet()));
        caseJsonObj.put("regionId", regionId);
        caseJsonObj.put("bandList", bandList);
        caseJsonObj.put("userId", userId);
        caseJsonObj.put("type", "eoCube");

        String calcEOCubeUrl;
        calcEOCubeUrl = modelServerProperties.getAddress() + modelServerProperties.getApis().get("eoCube");
        System.out.println(calcEOCubeUrl);

        long expirationTime = 60 * 100;
        return runModelServerModel(calcEOCubeUrl, calcEOCubeParam, expirationTime, caseJsonObj);
    }

    public CommonResultVO cacheEOCube(CacheEOCubeDTO cacheEOCubeDTO, String userId){
        String cacheKey = IdUtil.generateEOCubeCacheKey(userId);
        String cubeId = cacheEOCubeDTO.getCubeId();
        List<String> dimensionSensors = cacheEOCubeDTO.getDimensionSensors();
        List<String> dimensionDates = cacheEOCubeDTO.getDimensionDates();
        List<String> dimensionBands = cacheEOCubeDTO.getDimensionBands();
        List<EOCubeCache.Scene> dimensionScenes = cacheEOCubeDTO.getDimensionScenes();
        EOCubeCache.cacheEOCube(cacheKey, cubeId, dimensionSensors, dimensionDates, dimensionBands, dimensionScenes);
        return CommonResultVO.builder().status(1).message("EO立方体缓存成功").data(cacheKey).build();
    }

    public CommonResultVO getEOCube(String cacheKey){
        EOCubeCache eOCubeCache = EOCubeCache.getCache(cacheKey);
        return CommonResultVO.builder().status(1).message("获取EO立方体缓存成功").data(eOCubeCache).build();
    }

    public CommonResultVO getAllEOCube(){
        Map<String, Object> allCaches = EOCubeCache.getAllCaches();
        return CommonResultVO.builder()
                .status(1)
                .message("获取所有EO立方体缓存成功")
                .data(allCaches)  // 返回整个缓存 Map
                .build();
    }

    public CommonResultVO getUserEOCubes(String userId) {
        Map<String, EOCubeCache> userCaches = EOCubeCache.getUserCaches(userId);
        return CommonResultVO.builder()
                .status(1)
                .message("获取用户EO立方体缓存成功")
                .data(userCaches)
                .build();
    }

    // 函数重载，时序立方体专用
    private CommonResultVO runModelServerModel(String url, JSONObject param, long expirationTime, JSONObject caseJsonObj) {
        try {
            String rawResponse = ProcessUtil.runModelCase(url, param);
            System.out.println("Raw Response: " + rawResponse); // 打印原始返回内容
            JSONObject modelCaseResponse = JSONObject.parseObject(rawResponse); // 确认是否抛出异常

            String caseId = modelCaseResponse.getJSONObject("data").getString("taskId");
            // 在没有相应历史记录的情况下, 持久化记录
            if (caseDataService.selectById(caseId) == null) {
                caseDataService.addCaseFromParamAndCaseId(caseId, caseJsonObj);
            }
            JSONObject modelCase = JSONObject.of("status", "RUNNING", "start", LocalDateTime.now());
            redisUtil.addJsonDataWithExpiration(caseId, modelCase, expirationTime);
            // 注意最后再启动任务！！！不然会出现updateJsonField找不到对应键值的情况
            quartzSchedulerManager.startModelRunningStatusJob(caseId, modelServerProperties);
            return CommonResultVO.builder().status(1).message("success").data(caseId).build();
        } catch (Exception e) {
            return CommonResultVO.builder().status(-1).message("Wrong Because of " + e.getMessage()).build();
        }
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
