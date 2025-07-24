package nnu.mnr.satellite.jobs.modeling;

import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.jobs.QuartzSchedulerManager;
import nnu.mnr.satellite.model.pojo.modeling.BaseModelServerProperties;
import nnu.mnr.satellite.model.pojo.modeling.ModelServerProperties;
import nnu.mnr.satellite.service.resources.CaseDataService;
import nnu.mnr.satellite.utils.common.BeanUtil;
import nnu.mnr.satellite.utils.common.ProcessUtil;
import nnu.mnr.satellite.utils.dt.RedisUtil;
import org.quartz.*;

import java.time.LocalDateTime;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/21 9:48
 * @Description:
 */

@Slf4j
public class ModelRunStatusJob implements Job {

    private final RedisUtil redisUtil;

    private final QuartzSchedulerManager quartzSchedulerManager;

    @Resource
    private CaseDataService caseDataService;

    public ModelRunStatusJob() {
        redisUtil = BeanUtil.getBean(RedisUtil.class);
        quartzSchedulerManager = BeanUtil.getBean(QuartzSchedulerManager.class);
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String jobName = jobExecutionContext.getJobDetail().getKey().getName();
        String jobGroup = jobExecutionContext.getJobDetail().getKey().getGroup();
        JobDataMap dataMap = jobExecutionContext.getJobDetail().getJobDataMap();
        BaseModelServerProperties modelServerProperties = (BaseModelServerProperties) dataMap.get("serverProperties");
        String caseId = dataMap.getString("caseId");
        String statusUrl = modelServerProperties.getAddress() + modelServerProperties.getApis().get("status");
        JSONObject statusResponse = JSONObject.parseObject(ProcessUtil.getModelCaseStatus(statusUrl, caseId));
//        String status = statusResponse.getJSONObject("data").getString("status");
        // 堆一点屎
        JSONObject statusData = statusResponse.getJSONObject("data");
        String status;
        if(statusData == null) {
            status = statusResponse.getString("status");
        }else {
            status = statusResponse.getJSONObject("data").getString("status");
        }
//        String status = statusResponse.getJSONObject("data").getString("status");
//        if(status == null){
//            status = statusResponse.getString("status");
//        }
        if (status.equals("COMPLETE")) {
            redisUtil.updateJsonField(caseId, "status", status);
            caseDataService.updateCaseStatusById(caseId, status);
            log.info("model case " + caseId + " has finished!");
            try {
                quartzSchedulerManager.deleteJob(jobName, jobGroup);
                String resultUrl = modelServerProperties.getAddress() + modelServerProperties.getApis().get("result");
                JSONObject resultResponse = ProcessUtil.getModelCaseResult(resultUrl, caseId);
                // 再堆一点屎
                JSONObject resultData = resultResponse.getJSONObject("data");
                JSONObject resObj;
                try {
                    if(resultData == null) {
                        resObj = resultResponse.getJSONObject("result");
                    }else {
                        resObj = resultResponse.getJSONObject("data").getJSONObject("result");
                    }
//                    resObj = resultResponse.getJSONObject("data").getJSONObject("result");
//                    if (resObj == null) {
//                        resObj = resultResponse.getJSONObject("result");
//                    }
                } catch (Exception e) {
                    resObj = new JSONObject();
                    resObj.put("ERROR", "Failed to parse result: " + e.getMessage());
                }
                redisUtil.updateJsonField(caseId, "result", resObj);
                redisUtil.updateJsonField(caseId, "end", LocalDateTime.now());
                caseDataService.updateCaseResultById(caseId, resObj);
            } catch (SchedulerException e) {
                log.info(e.toString());
            }
        } else if (status.equals("ERROR")) {

            try {
                quartzSchedulerManager.deleteJob(jobName, jobGroup);
            } catch (SchedulerException e) {
                log.info(e.toString());
            }
            redisUtil.updateJsonField(caseId, "end", LocalDateTime.now());
            // caseDataService.removeCaseById(caseId);
            caseDataService.updateCaseStatusById(caseId, status);
        }
        // TODO: Add Other Conditions

        redisUtil.updateJsonField(caseId, "status", status);
    }
}
