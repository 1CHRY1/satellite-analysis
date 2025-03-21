package nnu.mnr.satellite.service.modeling;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import nnu.mnr.satellite.jobs.QuartzSchedulerManager;
import nnu.mnr.satellite.model.dto.modeling.NdviDTO;
import nnu.mnr.satellite.model.pojo.modeling.ModelServerProperties;
import nnu.mnr.satellite.utils.common.HttpUtil;
import nnu.mnr.satellite.utils.common.ProcessUtil;
import nnu.mnr.satellite.utils.data.MinioUtil;
import nnu.mnr.satellite.utils.data.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/20 14:18
 * @Description:
 */

@Service
public class ModelServerService {

    @Autowired
    MinioUtil minioUtil;

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    QuartzSchedulerManager quartzSchedulerManager;

    @Autowired
    ModelServerProperties modelServerProperties;

    // Common Services ******************************

    public String getModelCaseStatusById(String caseId) {
        return redisUtil.getJsonData(caseId).getString("status");
    }

    public byte[] getModelDataById(String dataId) {
        JSONObject dataInfo = redisUtil.getJsonData(dataId);
        String bucket = dataInfo.getString("bucket");
        String path = dataInfo.getString("path");
        return minioUtil.downloadByte(bucket, path);
    }


    // Business Services ******************************

    public String getNDVIByPoint(NdviDTO ndviDTO) {
        JSONObject ndviPointParam = JSONObject.of("sensorName", ndviDTO.getSensorName(), "scenes", ndviDTO.getScenes(), "point", ndviDTO.getGeometry());
        try {
            String ndviPointApi = modelServerProperties.getAddress() + modelServerProperties.getApis().get("ndviPoint");
            String caseId = ProcessUtil.runModelCase(ndviPointApi, ndviPointParam);
            quartzSchedulerManager.startModelRunningStatusJob(caseId);
            return caseId;
        } catch (Exception e) {
            return null;
        }
    }

    public String getNDVIByPolygon(NdviDTO ndviDTO) {
        JSONObject ndviAreaParam = JSONObject.of("sensorName", ndviDTO.getSensorName(), "scenes", ndviDTO.getScenes(), "polygon", ndviDTO.getGeometry());
//        JSONArray ndviIds = new JSONArray();
        try {
            String ndviAreaApi = modelServerProperties.getAddress() + modelServerProperties.getApis().get("ndviArea");
            String caseId = ProcessUtil.runModelCase(ndviAreaApi, ndviAreaParam);
            quartzSchedulerManager.startModelRunningStatusJob(caseId);
            return caseId;
//            JSONArray ndviAreaList = JSONArray.parseArray(HttpUtil.doPost(ndviAreaApi, ndviAreaParam));
//            for (Object ndviArea : ndviAreaList) {
//                JSONObject ndviAreaJson = (JSONObject) ndviArea;
//                String areaId = ndviAreaJson.getString("id");
//                JSONObject ndviAreaJsonVO = JSONObject.of("areaId", areaId, "areaTime", ndviAreaJson.getString("time"));
//                redisUtil.addJsonDataWithExpiration(areaId, ndviAreaJson, 60*10);
//                ndviIds.add(ndviAreaJsonVO);
//            }
//            return  ndviIds;
        } catch (Exception e) {
            return null;
        }
    }

}
