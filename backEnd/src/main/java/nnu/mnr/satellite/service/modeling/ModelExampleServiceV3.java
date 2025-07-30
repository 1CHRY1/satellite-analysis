package nnu.mnr.satellite.service.modeling;

import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.cache.SceneDataCache;
import nnu.mnr.satellite.model.dto.modeling.ModelServerImageDTO;
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

    public CommonResultVO createVisualizationConfig(VisualizationTileDTO visualizationTileDTO, String cacheKey) {
        SceneDataCache.UserSceneCache userSceneCache = SceneDataCache.getUserSceneCacheMap(cacheKey);
        if (userSceneCache == null) {
            return CommonResultVO.builder()
                    .message("No corresponding data found, please log in again or retrieve the data")
                    .data(cacheKey.split("_")[1])
                    .status(-1)
                    .build();
        }
        List<SceneDesVO> scenesInfo = userSceneCache.scenesInfo;
        try {
            // 1. 生成配置ID
            String configId = "Visualization" + UUID.randomUUID().toString().replace("-", "");
            // 2. 将影像信息转换为JSON并上传到MinIO
            String jsonFileName = "VisualizationConfig/" + configId + ".json";
            String bucketName = "temp-files";
            // 确保bucket存在
            minioUtil.existBucket(bucketName);
            // 构建JSON配置
            JSONObject configJson = buildVisualizationConfig(visualizationTileDTO, scenesInfo);

            return CommonResultVO.builder().status(1).message("success").data(configJson).build();

        } catch (Exception e) {
            return CommonResultVO.builder().status(-1).message("Error creating visualization config: " + e.getMessage()).build();
        }
    }

    // 构建影像可视化配置JSON
    private JSONObject buildVisualizationConfig(VisualizationTileDTO visualizationTileDTO, List<SceneDesVO> scenesInfo) {
        String sensorName = visualizationTileDTO.getSensorName();
        List<Float> points = visualizationTileDTO.getPoints();
        Geometry tileBoundingBox = GeometryUtil.pointsConvertToPolygon(points);
        JSONObject bandMapper = bandMapperGenerator.getSatelliteConfigBySensorName(sensorName);
        List<SceneDesVO> scenes = new ArrayList<>();
        for (SceneDesVO sceneDesVO : scenesInfo) {
            if (sensorName.equals(sceneDesVO.getSensorName())) {
                scenes.add(sceneDesVO);
            }
        }
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
        // 降序排列
        scenesConfig.sort((a, b) -> Double.compare(b.getCoverage(), a.getCoverage()));
        JSONObject result = new JSONObject();
        result.put("bandMapper", bandMapper);
        result.put("scenesConfig", scenesConfig);
        return result;
    }

}
