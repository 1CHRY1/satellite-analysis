package nnu.mnr.satellite.service.modeling;

import com.alibaba.fastjson2.JSONObject;
import nnu.mnr.satellite.jobs.QuartzSchedulerManager;
import nnu.mnr.satellite.model.dto.modeling.*;
import nnu.mnr.satellite.model.po.resources.Scene;
import nnu.mnr.satellite.model.po.resources.SceneSP;
import nnu.mnr.satellite.model.pojo.modeling.ModelServerProperties;
import nnu.mnr.satellite.model.pojo.modeling.SRModelServerProperties;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.service.resources.*;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static nnu.mnr.satellite.utils.geom.GeometryUtil.getGridsBoundaryByTilesAndResolution;
import static nnu.mnr.satellite.utils.geom.TileCalculateUtil.getTileGeomByIdsAndResolution;

import com.alibaba.fastjson2.JSON;
import nnu.mnr.satellite.utils.dt.MinioUtil;
import nnu.mnr.satellite.model.pojo.modeling.MinioProperties;
import nnu.mnr.satellite.model.dto.modeling.NoCloudTileDTO;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import io.minio.PutObjectArgs;

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
    CaseDataService caseDataService;

    @Autowired
    BandMapperGenerator bandMapperGenerator;

    @Autowired
    MinioUtil minioUtil;

    @Autowired
    MinioProperties minioProperties;

    // 用于缓存配置ID到JSON URL的映射
    private final Map<String, String> configCache = new ConcurrentHashMap<>();

    @Autowired
    SRModelServerProperties SRModelServerProperties;

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

    // 函数重载，无云一版图专用
    private CommonResultVO runModelServerModel(String url, JSONObject param, long expirationTime, JSONObject caseJsonObj) {
        try {
            JSONObject modelCaseResponse = JSONObject.parseObject(ProcessUtil.runModelCase(url, param));
            String caseId = modelCaseResponse.getJSONObject("data").getString("taskId");
            // 在没有相应历史记录的情况下, 持久化记录
            if (caseDataService.selectById(caseId) == null) {
                caseDataService.addCaseFromParamAndCaseId(caseId, caseJsonObj);
            }
            JSONObject modelCase = JSONObject.of("status", "RUNNING", "start", LocalDateTime.now());
            redisUtil.addJsonDataWithExpiration(caseId, modelCase, expirationTime);
            // 注意最后再启动任务！！！不然会出现updateJsonField找不到对应键值的情况
            quartzSchedulerManager.startModelRunningStatusJob(caseId);
            return CommonResultVO.builder().status(1).message("success").data(caseId).build();
        } catch (Exception e) {
            return CommonResultVO.builder().status(-1).message("Wrong Because of " + e.getMessage()).build();
        }
    }

    public CommonResultVO getNoCloudByRegionZZW(NoCloudFetchDTO noCloudFetchDTO) throws IOException {
        Integer regionId = noCloudFetchDTO.getRegionId();
        Integer resolution = noCloudFetchDTO.getResolution();
        List<String> sceneIds = noCloudFetchDTO.getSceneIds();
        List<String> bandList = noCloudFetchDTO.getBandList();

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

        // 构建行政区地址信息
        String address = regionDataService.getAddressById(regionId);

        // 构建边界信息
        Geometry boundary = getGridsBoundaryByTilesAndResolution(tileIds, resolution);

        // 请求modelServer
        JSONObject noCloudParam = JSONObject.of("tiles", tileIds, "scenes", modelServerSceneDTOs, "cloud", noCloudFetchDTO.getCloud(), "resolution", resolution, "bandList", bandList);
        JSONObject caseJsonObj = JSONObject.of("boundary", boundary, "address", address, "resolution", resolution, "sceneIds", sceneIds, "dataSet", noCloudFetchDTO.getDataSet());
        caseJsonObj.put("regionId", regionId);
        String noCloudUrl = modelServerProperties.getAddress() + modelServerProperties.getApis().get("noCloud");
        long expirationTime = 60 * 100;
        return runModelServerModel(noCloudUrl, noCloudParam, expirationTime, caseJsonObj);
    }

    // 无云一版图计算
    public CommonResultVO getNoCloudByRegion(NoCloudFetchDTO noCloudFetchDTO) throws IOException {
        Integer regionId = noCloudFetchDTO.getRegionId();
        Integer resolution = noCloudFetchDTO.getResolution();
        List<String> sceneIds = noCloudFetchDTO.getSceneIds();
        List<String> bandList = noCloudFetchDTO.getBandList();
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
        Geometry region = regionDataService.getRegionById(regionId).getBoundary();
        List<Integer[]> tileIds = TileCalculateUtil.getRowColByRegionAndResolution(region, resolution);

        // 构建行政区地址信息
        String address = regionDataService.getAddressById(regionId);

        // 构建边界信息
        Geometry boundary = getGridsBoundaryByTilesAndResolution(tileIds, resolution);

        // 请求modelServer
        JSONObject noCloudParam = JSONObject.of("tiles", tileIds, "scenes", modelServerSceneDTOs, "cloud", noCloudFetchDTO.getCloud(), "resolution", resolution, "bandList", bandList);
        JSONObject caseJsonObj = JSONObject.of("boundary", boundary, "address", address, "resolution", resolution, "sceneIds", sceneIds, "dataSet", noCloudFetchDTO.getDataSet());
        caseJsonObj.put("regionId", regionId);
        caseJsonObj.put("bandList", bandList);
        String noCloudUrl = modelServerProperties.getAddress() + modelServerProperties.getApis().get("noCloud");
        long expirationTime = 60 * 100;
        return runModelServerModel(noCloudUrl, noCloudParam, expirationTime, caseJsonObj);
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
        JSONObject ndviParam = JSONObject.of("point", point, "scenes", modelServerSceneDTOs);
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
        JSONObject ndviParam = JSONObject.of("point", point, "images", images);
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
                JSONObject pointRasterParam = JSONObject.of("point", point, "raster", images.get(0));
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
        JSONObject pointRasterParam = JSONObject.of("points", points, "rasters", images);
        String pointRasterUrl = modelServerProperties.getAddress() + modelServerProperties.getApis().get("rasterLine");
        long expirationTime = 60 * 10;
        return runModelServerModel(pointRasterUrl, pointRasterParam, expirationTime);
    }

    public CommonResultVO getSRResultByBand(SRBandDTO SRBandDTO){
        Geometry wkt = getTileGeomByIdsAndResolution(SRBandDTO.getRowId(), SRBandDTO.getColumnId(), SRBandDTO.getResolution());
        JSONObject band = SRBandDTO.getBand();
        JSONObject SRJson = new JSONObject();
        SRJson.put("wkt", wkt.toString());  // Geometry 转 WKT 字符串
        SRJson.put("band", band);          // 直接放入 JSONObject
        String SRUrl = SRModelServerProperties.getAddress() + SRModelServerProperties.getApis().get("SR");
        long expirationTime = 60 * 10;
        return runModelServerModel(SRUrl, SRJson, expirationTime);
    }
    public CommonResultVO createNoCloudConfig(NoCloudTileDTO noCloudTileDTO) {
        try {
            // 1. 生成配置ID
            String configId = "nocloud_" + UUID.randomUUID().toString().replace("-", "");

            // 2. 将影像信息转换为JSON并上传到MinIO
            String jsonFileName = "zzw/" + configId + ".json";
            String bucketName = "temp-files";

            // 确保bucket存在
            minioUtil.existBucket(bucketName);

            // 构建JSON配置
            JSONObject configJson = buildNoCloudConfig(noCloudTileDTO);
            String jsonContent = configJson.toJSONString();

            // 上传JSON到MinIO
            uploadJsonToMinio(bucketName, jsonFileName, jsonContent);

            // 3. 构建JSON的访问URL并缓存
            String jsonUrl = minioProperties.getUrl() + "/" + bucketName + "/" + jsonFileName;
            // configCache.put(configId, jsonUrl);

            return CommonResultVO.builder().status(1).message("success").data(jsonUrl).build();

        } catch (Exception e) {
            return CommonResultVO.builder().status(-1).message("Error creating no-cloud config: " + e.getMessage()).build();
        }
    }

    // 无云一版图瓦片服务（备用方法，当前不使用）
    // public byte[] getNoCloudTile(int z, int x, int y, NoCloudTileDTO noCloudTileDTO) {
    //     try {
    //         // 直接创建临时配置并调用瓦片服务
    //         CommonResultVO configResult = createNoCloudConfig(noCloudTileDTO);
    //         String configId = (String) configResult.getData();
    //         return getNoCloudTileByConfig(configId, z, x, y);
    //
    //     } catch (Exception e) {
    //         throw new RuntimeException("Error generating no-cloud tile: " + e.getMessage(), e);
    //     }
    // }

    // 构建无云一版图配置JSON
    private JSONObject buildNoCloudConfig(NoCloudTileDTO noCloudTileDTO) {
        List<JSONObject> scenesConfig = new ArrayList<>();

        // 处理所有场景ID
        for (String sceneId : noCloudTileDTO.getSceneIds()) {
            SceneSP scene = sceneDataServiceV2.getSceneByIdWithProductAndSensor(sceneId);
            List<ModelServerImageDTO> imageDTO = imageDataService.getModelServerImageDTOBySceneId(sceneId);

            JSONObject sceneConfig = new JSONObject();
            sceneConfig.put("sceneId", sceneId);
            sceneConfig.put("sensorName", scene.getSensorName());
            sceneConfig.put("resolution", scene.getResolution());
            sceneConfig.put("noData", scene.getNoData());
            sceneConfig.put("bucket", scene.getBucket());
            sceneConfig.put("cloudPath", scene.getCloudPath());
            try {
                sceneConfig.put("bbox", GeometryUtil.geometry2Geojson(scene.getBbox()));
            } catch (IOException e) {
                throw new RuntimeException("Failed to convert geometry to GeoJSON", e);
            }

            // 添加波段路径信息
            JSONObject paths = new JSONObject();
            for (ModelServerImageDTO image : imageDTO) {
                paths.put("band_" + image.getBand(), image.getTifPath());
            }
            sceneConfig.put("paths", paths);

            // 添加波段映射配置信息
            JSONObject bandMapper = bandMapperGenerator.getSatelliteConfigBySensorName(scene.getSensorName());
            sceneConfig.put("bandMapper", bandMapper);

            scenesConfig.add(sceneConfig);
        }

        return new JSONObject().fluentPut("scenes", scenesConfig);
    }

    // 上传JSON到MinIO
    private void uploadJsonToMinio(String bucketName, String fileName, String jsonContent) {
        try {
            // 使用MinioUtil的现有方法，需要先创建一个临时的MultipartFile
            ByteArrayInputStream inputStream = new ByteArrayInputStream(jsonContent.getBytes(StandardCharsets.UTF_8));

            // 创建一个简单的MultipartFile包装
            MultipartFile jsonFile = new MultipartFile() {
                @Override
                public String getName() { return "json"; }
                @Override
                public String getOriginalFilename() { return fileName; }
                @Override
                public String getContentType() { return "application/json"; }
                @Override
                public boolean isEmpty() { return false; }
                @Override
                public long getSize() { return jsonContent.length(); }
                @Override
                public byte[] getBytes() throws IOException { return jsonContent.getBytes(StandardCharsets.UTF_8); }
                @Override
                public java.io.InputStream getInputStream() throws IOException { return inputStream; }
                @Override
                public void transferTo(java.io.File dest) throws IOException, IllegalStateException {}
            };

            minioUtil.upload(jsonFile, fileName, bucketName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload JSON to MinIO: " + e.getMessage(), e);
        }
    }

}
