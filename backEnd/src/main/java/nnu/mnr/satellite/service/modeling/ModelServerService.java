package nnu.mnr.satellite.service.modeling;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import nnu.mnr.satellite.jobs.QuartzSchedulerManager;
import nnu.mnr.satellite.model.dto.modeling.NdviDTO;
import nnu.mnr.satellite.model.pojo.modeling.ModelServerProperties;
import nnu.mnr.satellite.utils.common.ProcessUtil;
import nnu.mnr.satellite.utils.data.MinioUtil;
import nnu.mnr.satellite.utils.data.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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

    public Object getModelCaseResultById(String caseId) {
        String[] removeFields = {"bucket", "path"};
        String modelResult = redisUtil.getJsonData(caseId).getString("result");
        try {
            JSONObject modelResultJson = JSONObject.parseObject(modelResult);
            for (String field : removeFields) {
                modelResultJson.remove(field);
            }
            return modelResultJson;
        } catch (Exception e) {
            JSONArray modelResultArray = JSONArray.parseArray(modelResult);
            JSONArray modelResultArrayVO = new JSONArray();
            for ( Object obj : modelResultArray ) {
                JSONObject obJson = (JSONObject) obj;
                for (String field : removeFields) {
                    obJson.remove(field);
                }
                modelResultArrayVO.add(obJson);
            }
            return modelResultArrayVO;
        }
    }

    public byte[] getModelDataById(String dataId) {
        JSONObject dataInfo = redisUtil.getJsonData(dataId);
        String bucket = dataInfo.getString("bucket");
        String path = dataInfo.getString("path");
        return minioUtil.downloadByte(bucket, path);
    }

    public void putModelCaseToRedis(String caseId, JSONObject param) {
        JSONObject caseJson = new JSONObject();
        caseJson.put("status", "RUNNING");
        caseJson.put("param", param);
        caseJson.put("start", LocalDateTime.now());
        caseJson.put("end", null);
        caseJson.put("result", "");
        redisUtil.addJsonDataWithExpiration(caseId, caseJson, 30*60);
    }
}
