package nnu.mnr.satellite.service.modeling;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import nnu.mnr.satellite.jobs.QuartzSchedulerManager;
import nnu.mnr.satellite.model.pojo.modeling.ModelServerProperties;
import nnu.mnr.satellite.model.pojo.modeling.TilerProperties;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.model.vo.modeling.TilerResultVO;
import nnu.mnr.satellite.utils.dt.MinioUtil;
import nnu.mnr.satellite.utils.dt.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

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

    @Autowired
    TilerProperties tilerProperties;

    // Common Services ******************************

    public CommonResultVO getModelCaseStatusById(String caseId) {
        Optional<String> oStatus = Optional.ofNullable(redisUtil.getJsonData(caseId).getString("status"));
        if (oStatus.isPresent()) {
            String status = oStatus.get();
            return CommonResultVO.builder().status(1).message("success").data(status).build();
        } else {
            return CommonResultVO.builder().status(-1).message("Wrong getting status").build();
        }
    }

    public CommonResultVO getModelCaseResultById(String caseId) {
        String[] removeFields = {};
//        String[] removeFields = {"bucket", "path"};
        Optional<String> oModelResult = Optional.ofNullable(redisUtil.getJsonData(caseId).getString("result"));
        if (oModelResult.isEmpty()) {
            return CommonResultVO.builder().status(-1).message("Wrong getting result by id").build();
        }
        String modelResult = oModelResult.get();
        try {
            JSONObject modelResultJson = JSONObject.parseObject(modelResult);
            for (String field : removeFields) {
                modelResultJson.remove(field);
            }
            return CommonResultVO.builder().status(1).message("success").data(modelResultJson).build();
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
            return CommonResultVO.builder().status(1).message("success").data(modelResultArrayVO).build();
        }
    }

    public CommonResultVO getModelCaseTifResultById(String caseId) {
        Optional<String> oModelResult = Optional.ofNullable(redisUtil.getJsonData(caseId).getString("result"));
        if (oModelResult.isEmpty()) {
            return CommonResultVO.builder().status(-1).message("Wrong getting tif result").build();
        }
        String modelResult = oModelResult.get();
        JSONObject modelResultJson = JSONObject.parseObject(modelResult);
        String bucket = modelResultJson.getString("bucket"); String path = modelResultJson.getString("path");
        TilerResultVO tilerInfo = TilerResultVO.tilerBuilder()
                .tilerUrl(tilerProperties.getEndPoint())
                .object(bucket + path)
                .build();
        return CommonResultVO.builder().status(1).message("success").data(tilerInfo).build();
    }

    public byte[] getTifDataById(String caseId) {
        try {
            Optional<JSONObject> oDataInfo = Optional.ofNullable(redisUtil.getJsonData(caseId).getJSONObject("result"));
            if (oDataInfo.isEmpty()) {
                return null;
            }
            JSONObject dataInfo = oDataInfo.get();
            String bucket = dataInfo.getString("bucket");
            String path = dataInfo.getString("path");
            return minioUtil.downloadByte(bucket, path);
        } catch (Exception e) {
            return null;
        }

    }

}
