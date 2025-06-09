package nnu.mnr.satellite.service.modeling;

import com.alibaba.fastjson2.JSONObject;
import nnu.mnr.satellite.jobs.QuartzSchedulerManager;
import nnu.mnr.satellite.model.dto.modeling.*;
import nnu.mnr.satellite.model.po.resources.Scene;
import nnu.mnr.satellite.model.po.resources.SceneSP;
import nnu.mnr.satellite.model.pojo.modeling.ModelServerProperties;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.service.resources.ImageDataService;
import nnu.mnr.satellite.service.resources.RegionDataService;
import nnu.mnr.satellite.service.resources.SceneDataService;
import nnu.mnr.satellite.service.resources.SceneDataServiceV2;
import nnu.mnr.satellite.utils.common.ProcessUtil;
import nnu.mnr.satellite.service.common.BandMapperGenerator;
import nnu.mnr.satellite.utils.dt.RedisUtil;
import nnu.mnr.satellite.utils.geom.GeometryUtil;
import nnu.mnr.satellite.utils.geom.TileCalculateUtil;
import org.locationtech.jts.geom.Geometry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/5/8 22:39
 * @Description:
 */

@Service
public class ModelExampleService {

    @Autowired
    ModelServerProperties modelServerProperties;

    @Autowired
    QuartzSchedulerManager quartzSchedulerManager;

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    SceneDataServiceV2 sceneDataServiceV2;

    @Autowired
    SceneDataService sceneDataServiceV1;

    @Autowired
    ImageDataService imageDataService;

    @Autowired
    RegionDataService regionDataService;

    @Autowired
    BandMapperGenerator bandMapperGenerator;

    private CommonResultVO runModelServerModel(String url, JSONObject param, long expirationTime) {
        try {
            JSONObject modelCaseResponse = JSONObject.parseObject(ProcessUtil.runModelCase(url, param));
            String caseId = modelCaseResponse.getJSONObject("data").getString("taskId");
            quartzSchedulerManager.startModelRunningStatusJob(caseId);
            JSONObject modelCase = JSONObject.of("status", "RUNNING", "start", LocalDateTime.now());
            redisUtil.addJsonDataWithExpiration(caseId, modelCase, expirationTime);
            return CommonResultVO.builder().status(1).message("success").data(caseId).build();
        } catch (Exception e) {
            return CommonResultVO.builder().status(-1).message("Wrong Because of " + e.getMessage()).build();
        }
    }

    // 无云一版图计算
    public CommonResultVO getNoCloudByRegion(NoCloudFetchDTO noCloudFetchDTO) throws IOException {
        Integer regionId = noCloudFetchDTO.getRegionId(); Integer resolution = noCloudFetchDTO.getResolution();
        List<String> sceneIds = noCloudFetchDTO.getSceneIds();

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
        Geometry region = regionDataService.getRegionById(regionId).getBoundary();
        List<Integer[]> tileIds = TileCalculateUtil.getRowColByRegionAndResolution(region, resolution);

        // 请求modelServer
        JSONObject noCloudParam = JSONObject.of("tiles", tileIds, "scenes", modelServerSceneDTOs, "cloud", noCloudFetchDTO.getCloud(), "resolution", resolution);
        String noCloudUrl = modelServerProperties.getAddress() + modelServerProperties.getApis().get("noCloud");
        long expirationTime = 60 * 100;
        return runModelServerModel(noCloudUrl, noCloudParam, expirationTime);
    }

    // NDVI指数计算
    public CommonResultVO getNDVIByPoint(NdviFetchDTO ndviFetchDTO) {
        List<String> sceneIds = ndviFetchDTO.getSceneIds();
        Double[] point = ndviFetchDTO.getPoint();

        // 构成影像景参数信息
        List<ModelServerSceneDTO> modelServerSceneDTOs = new ArrayList<>();

        // 构成影像景参数信息
        for (String sceneId : sceneIds) {
            Geometry geomPoint = GeometryUtil.parse4326Point(point);
            SceneSP scene = sceneDataServiceV2.getSceneByIdWithProductAndSensor(sceneId);
            if (scene.getBbox().contains(geomPoint)) {
                List<ModelServerImageDTO> imageDTO = imageDataService.getModelServerImageDTOBySceneId(sceneId);

                // TODO: 临时需求删选波段45
//                boolean hasBand4 = imageDTO.stream().anyMatch(img -> "4".equals(img.getBand()));
//                boolean hasBand5 = imageDTO.stream().anyMatch(img -> "5".equals(img.getBand()));
//                if (!hasBand4 || !hasBand5) {
//                    continue;
//                }

               ModelServerSceneDTO modelServerSceneDTO = ModelServerSceneDTO.builder()
                        .sceneId(sceneId).images(imageDTO).sceneTime(scene.getSceneTime())
                       .sensorName(scene.getSensorName()).productName(scene.getProductName())
                       .bandMapper(bandMapperGenerator.getSatelliteConfigBySensorName(scene.getSensorName()))
                        .cloudPath(scene.getCloudPath()).bucket(scene.getBucket()).build();
                modelServerSceneDTOs.add(modelServerSceneDTO);
            }
        }

        // 请求modelServer
        JSONObject ndviParam = JSONObject.of("point",point, "scenes", modelServerSceneDTOs);
        String ndviUrl = modelServerProperties.getAddress() + modelServerProperties.getApis().get("ndvi");
        long expirationTime = 60 * 10;
        return runModelServerModel(ndviUrl, ndviParam, expirationTime);
    }

    // 光谱分析计算
    public CommonResultVO getSpectrumByPoint(SpectrumDTO spectrumDTO) {
        Double[] point = spectrumDTO.getPoint();
        String sceneId = spectrumDTO.getSceneId();

        // 构成影像景参数信息
        Geometry geomPoint = GeometryUtil.parse4326Point(point);
        SceneSP scene = sceneDataServiceV2.getSceneByIdWithProductAndSensor(sceneId);
        if (!scene.getBbox().contains(geomPoint)) {
            return CommonResultVO.builder().status(-1).message("Point is not contained in Scene" + sceneId).build();
        }
        List<ModelServerImageDTO> images = imageDataService.getModelServerImageDTOBySceneId(sceneId);

        // 请求modelServer
        JSONObject ndviParam = JSONObject.of("point",point, "images", images);
        String ndviUrl = modelServerProperties.getAddress() + modelServerProperties.getApis().get("spectrum");
        long expirationTime = 60 * 10;
        return runModelServerModel(ndviUrl, ndviParam, expirationTime);
    }

    // 根据点和数据计算栅格值
    public CommonResultVO getRasterResultByPoint(PointRasterFetchDTO pointRasterFetchDTO) {
        Double[] point = pointRasterFetchDTO.getPoint();
        List<String> sceneIds = pointRasterFetchDTO.getSceneIds();

        // 构成影像景参数信息
        Geometry geomPoint = GeometryUtil.parse4326Point(point);
        List<Scene> scenes = sceneDataServiceV1.getScenesByIds(sceneIds);
        for (Scene scene : scenes) {
            if (scene.getBbox().contains(geomPoint)) {
                List<ModelServerImageDTO> images = imageDataService.getModelServerImageDTOBySceneId(scene.getSceneId());
                // 请求modelServer
                JSONObject pointRasterParam = JSONObject.of("point",point, "raster", images.get(0));
                String pointRasterUrl = modelServerProperties.getAddress() + modelServerProperties.getApis().get("rasterPoint");
                long expirationTime = 60 * 10;
                return runModelServerModel(pointRasterUrl, pointRasterParam, expirationTime);
            }
        }
        return CommonResultVO.builder().status(-1).message("Point is not contained in Scenes").build();
    }

    // 根据线和一组栅格数据计算栅格list值
    public CommonResultVO getRasterResultByLine(LineRasterFetchDTO lineRasterFetchDTO) {
        List<Double[]> points = lineRasterFetchDTO.getPoints();
        List<String> sceneIds = lineRasterFetchDTO.getSceneIds();
        // 构成影像景参数信息a
        List<Scene> scenes = sceneDataServiceV1.getScenesByIds(sceneIds);
        List<ModelServerImageDTO> images = new ArrayList<>();
        for (Scene scene : scenes) {
            List<ModelServerImageDTO> sceneImages = imageDataService.getModelServerImageDTOBySceneId(scene.getSceneId());
            images.add(sceneImages.get(0));
        }
        JSONObject pointRasterParam = JSONObject.of("points",points, "rasters", images);
        String pointRasterUrl = modelServerProperties.getAddress() + modelServerProperties.getApis().get("rasterLine");
        long expirationTime = 60 * 10;
        return runModelServerModel(pointRasterUrl, pointRasterParam, expirationTime);
    }

}
