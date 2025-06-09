package nnu.mnr.satellite.jobs.modeling;

import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.jobs.QuartzSchedulerManager;
import nnu.mnr.satellite.model.pojo.modeling.ModelServerProperties;
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

    public ModelRunStatusJob() {
        redisUtil = BeanUtil.getBean(RedisUtil.class);
        quartzSchedulerManager = BeanUtil.getBean(QuartzSchedulerManager.class);
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String jobName = jobExecutionContext.getJobDetail().getKey().getName();
        String jobGroup = jobExecutionContext.getJobDetail().getKey().getGroup();
        JobDataMap dataMap = jobExecutionContext.getJobDetail().getJobDataMap();
        ModelServerProperties modelServerProperties = (ModelServerProperties) dataMap.get("modelServerProperties");
        String caseId = dataMap.getString("caseId");
        String statusUrl = modelServerProperties.getAddress() + modelServerProperties.getApis().get("status");
        JSONObject statusResponse = JSONObject.parseObject(ProcessUtil.getModelCaseStatus(statusUrl, caseId));
        String status = statusResponse.getJSONObject("data").getString("status");
        if (status.equals("COMPLETE")) {
            redisUtil.updateJsonField(caseId, "status", status);
            log.info("model case " + caseId + " has finished!");
            try {
                quartzSchedulerManager.deleteJob(jobName, jobGroup);
                String resultUrl = modelServerProperties.getAddress() + modelServerProperties.getApis().get("result");
                JSONObject resultResponse = ProcessUtil.getModelCaseResult(resultUrl, caseId);
                JSONObject resObj = resultResponse.getJSONObject("data").getJSONObject("result");
                try {
                    resObj = resultResponse.getJSONObject("data").getJSONObject("result");
                } catch (Exception e) {
                    resObj.put("ERROR", resultResponse.getJSONObject("data"));
                }
                redisUtil.updateJsonField(caseId, "result", resObj);
                redisUtil.updateJsonField(caseId, "end", LocalDateTime.now());
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
        }
        // TODO: Add Other Conditions

        redisUtil.updateJsonField(caseId, "status", status);
    }
}
