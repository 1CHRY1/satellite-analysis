package nnu.mnr.satellite.service.modeling;

import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.cache.SceneDataCache;
import nnu.mnr.satellite.model.dto.modeling.ModelServerImageDTO;
import nnu.mnr.satellite.model.dto.modeling.ModelServerSceneDTO;
import nnu.mnr.satellite.model.dto.modeling.VisualizationTileDTO;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.model.vo.modeling.NoCloudConfigVO;
import nnu.mnr.satellite.model.vo.resources.SceneDesVO;
import nnu.mnr.satellite.service.common.BandMapperGenerator;
import nnu.mnr.satellite.service.resources.ImageDataService;
import nnu.mnr.satellite.service.resources.SceneDataServiceV3;
import nnu.mnr.satellite.utils.geom.GeometryUtil;
import org.locationtech.jts.geom.Geometry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import nnu.mnr.satellite.utils.dt.MinioUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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

    public CommonResultVO createScenesVisualizationConfig(VisualizationTileDTO visualizationTileDTO, String cacheKey) {
        SceneDataCache.UserSceneCache userSceneCache = SceneDataCache.getUserSceneCacheMap(cacheKey);
        if (userSceneCache == null) {
            return CommonResultVO.builder()
                    .message("No corresponding data found, please log in again or retrieve the data")
                    .data(cacheKey.split("_")[1])
                    .status(-1)
                    .build();
        }
        try {
            // 构建JSON配置
            JSONObject configJson = buildScenesVisualizationConfig(visualizationTileDTO, userSceneCache);

            return CommonResultVO.builder().status(1).message("success").data(configJson).build();

        } catch (Exception e) {
            return CommonResultVO.builder().status(-1).message("Error creating visualization config: " + e.getMessage()).build();
        }
    }

    // 构建影像可视化配置JSON
    private JSONObject buildScenesVisualizationConfig(VisualizationTileDTO visualizationTileDTO, SceneDataCache.UserSceneCache userSceneCache) {
        // 1、先将瓦片的points转geometry
        List<Float> points = visualizationTileDTO.getPoints();
        Geometry tileBoundingBox = GeometryUtil.pointsConvertToPolygon(points);
        // 2、用空间索引筛选瓦片周围的景
        List<SceneDesVO> scenesInfo = userSceneCache.queryCandidateScenes(tileBoundingBox);
        // 3、根据sensorName进一步筛选景，同时获取bandMapper（最后才用上）
        String sensorName = visualizationTileDTO.getSensorName();
        JSONObject bandMapper = bandMapperGenerator.getSatelliteConfigBySensorName(sensorName);
        List<SceneDesVO> scenes = new ArrayList<>();
        for (SceneDesVO sceneDesVO : scenesInfo) {
            if (sensorName.equals(sceneDesVO.getSensorName())) {
                scenes.add(sceneDesVO);
            }
        }

        // 记录开始时间
        long startTime = System.nanoTime();
        List<NoCloudConfigVO> scenesConfig = new ArrayList<>();
        for (SceneDesVO scene : scenes) {
            // path
            List<ModelServerImageDTO> imageDTO = imageDataService.getModelServerImageDTOBySceneId(scene.getSceneId());
            JSONObject paths = new JSONObject();
            for (ModelServerImageDTO image : imageDTO) {
                paths.put("band_" + image.getBand(), image.getTifPath());
            }
            // coverage
            double coverage = sceneDataService.calculateCoveragePercentage(scene.getBoundingBox(), tileBoundingBox);
            if (coverage > 0.0) {
                NoCloudConfigVO noCloudConfigVO = new NoCloudConfigVO();
                noCloudConfigVO.setSensorName(sensorName);
                noCloudConfigVO.setSceneId(scene.getSceneId());
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
        System.out.println("筛选影响可视化景数据运行时间: " + durationMs + " ms，景总数为：" + scenesConfig.size() + "景");
        // 降序排列
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

}
