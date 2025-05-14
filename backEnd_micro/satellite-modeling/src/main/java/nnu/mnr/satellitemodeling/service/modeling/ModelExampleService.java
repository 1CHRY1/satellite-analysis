package nnu.mnr.satellitemodeling.service.modeling;

import com.alibaba.fastjson2.JSONObject;
import nnu.mnr.satellitemodeling.client.ResourceClient;
import nnu.mnr.satellitemodeling.jobs.QuartzSchedulerManager;
import nnu.mnr.satellitemodeling.model.dto.modeling.ModelServerImageDTO;
import nnu.mnr.satellitemodeling.model.dto.modeling.ModelServerSceneDTO;
import nnu.mnr.satellitemodeling.model.dto.modeling.NdviFetchDTO;
import nnu.mnr.satellitemodeling.model.dto.modeling.NoCloudFetchDTO;
import nnu.mnr.satellitemodeling.model.po.resources.Scene;
import nnu.mnr.satellitemodeling.model.properties.ModelServerProperties;
import nnu.mnr.satellitemodeling.model.vo.common.CommonResultVO;
import nnu.mnr.satellitemodeling.utils.common.ProcessUtil;
import nnu.mnr.satellitemodeling.utils.data.RedisUtil;
import nnu.mnr.satellitemodeling.utils.geom.GeometryUtil;
import nnu.mnr.satellitemodeling.utils.geom.TileCalculateUtil;
import org.locationtech.jts.geom.Geometry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    ResourceClient resourceClient;

    private CommonResultVO runModelServerModel(String url, JSONObject param, long expirationTime) {
        try {
            JSONObject modelCaseResponse = JSONObject.parseObject(ProcessUtil.runModelCase(url, param));
            String caseId = modelCaseResponse.getJSONObject("data").getString("taskId");
            quartzSchedulerManager.startModelRunningStatusJob(caseId);
            JSONObject modelCase = JSONObject.of("status", "running", "start", LocalDateTime.now());
            redisUtil.addJsonDataWithExpiration(caseId, modelCase, expirationTime);
            return CommonResultVO.builder().status(1).message("success").data(caseId).build();
        } catch (Exception e) {
            return CommonResultVO.builder().status(-1).message("Wrong Because of " + e.getMessage()).build();
        }
    }

    // 无云一版图计算
    public CommonResultVO getNoCloudByRegion(NoCloudFetchDTO noCloudFetchDTO) {
        Integer regionId = noCloudFetchDTO.getRegionId();
        Integer resolution = noCloudFetchDTO.getResolution();
        List<String> sceneIds = noCloudFetchDTO.getSceneIds();

        // 构成影像景参数信息
        List<ModelServerSceneDTO> modelServerSceneDTOs = new ArrayList<>();

        // 构成影像景参数信息
        for (String sceneId : sceneIds) {
            Scene scene = resourceClient.getSceneById(sceneId);
            List<ModelServerImageDTO> imageDTO = resourceClient.getModelServerImageDTO(sceneId);
            ModelServerSceneDTO modelServerSceneDTO = ModelServerSceneDTO.builder()
                    .sceneId(sceneId).images(imageDTO).cloudPath(scene.getCloudPath()).build();
            modelServerSceneDTOs.add(modelServerSceneDTO);
        }

        // 构成网格行列号信息
        Geometry region = resourceClient.getRegionById(regionId).getBoundary();
        List<Integer[]> tileIds = TileCalculateUtil.getRowColByRegionAndResolution(region, resolution);

        // 请求modelServer
        JSONObject noCloudParam = JSONObject.of("tiles", tileIds, "scenes", modelServerSceneDTOs, "cloud", noCloudFetchDTO.getCloud(), "resolution", resolution);
        String noCloudUrl = modelServerProperties.getAddress() + modelServerProperties.getApis().get("noCloud");
        long expirationTime = 60 * 10;
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
            Scene scene = resourceClient.getSceneById(sceneId);
            if (scene.getBbox().contains(geomPoint)) {
                List<ModelServerImageDTO> imageDTO = resourceClient.getModelServerImageDTO(sceneId);
                ModelServerSceneDTO modelServerSceneDTO = ModelServerSceneDTO.builder()
                        .sceneId(sceneId).images(imageDTO).cloudPath(scene.getCloudPath()).build();
                modelServerSceneDTOs.add(modelServerSceneDTO);
            }
        }

        // 请求modelServer
        JSONObject ndviParam = JSONObject.of("point",point, "scenes", modelServerSceneDTOs);
        String ndviUrl = modelServerProperties.getAddress() + modelServerProperties.getApis().get("ndvi");
        long expirationTime = 60 * 10;
        return runModelServerModel(ndviUrl, ndviParam, expirationTime);
    }

}
