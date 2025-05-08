package nnu.mnr.satellite.service.modeling;

import com.alibaba.fastjson2.JSONObject;
import nnu.mnr.satellite.jobs.QuartzSchedulerManager;
import nnu.mnr.satellite.model.dto.resources.TilesMergeDTO;
import nnu.mnr.satellite.model.po.resources.Region;
import nnu.mnr.satellite.model.pojo.modeling.ModelServerProperties;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.service.resources.RegionDataService;
import nnu.mnr.satellite.service.resources.SceneDataServiceV2;
import nnu.mnr.satellite.utils.common.ProcessUtil;
import nnu.mnr.satellite.utils.data.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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
    SceneDataServiceV2 sceneDataService;

//    public CommonResultVO getNoCloudByRegion(Integer regionId) {
//        Region region = sceneDataService.getScenesByTimeAndRegion(LocalDateTime.now().toString(), LocalDateTime.now().toString() ,regionId);
//
//    }

    public CommonResultVO getNDVIResult() {
        JSONObject ndviParam = JSONObject.of("param1", "p1", "param2", "p2");
        String ndviUrl = modelServerProperties.getAddress() + modelServerProperties.getApis().get("ndvi");
        try {
            JSONObject modelCaseResponse = JSONObject.parseObject(ProcessUtil.runModelCase(ndviUrl, ndviParam));
            String caseId = modelCaseResponse.getJSONObject("data").getString("taskId");
            quartzSchedulerManager.startModelRunningStatusJob(caseId);
            JSONObject modelCase = JSONObject.of("status", "running", "start", LocalDateTime.now());
            redisUtil.addJsonDataWithExpiration(caseId, modelCase, 60 * 10);
            return CommonResultVO.builder().status(1).message("success").data(caseId).build();
        } catch (Exception e) {
            return CommonResultVO.builder().status(-1).message("Wrong Because of " + e.getMessage()).build();
        }
    }

}
