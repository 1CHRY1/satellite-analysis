package nnu.mnr.satellitemodeling.jobs.modeling;

import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satelliteresource.jobs.QuartzSchedulerManager;
import nnu.mnr.satelliteresource.model.properties.ModelServerProperties;
import nnu.mnr.satelliteresource.utils.common.BeanUtil;
import nnu.mnr.satelliteresource.utils.common.ProcessUtil;
import nnu.mnr.satelliteresource.utils.data.RedisUtil;
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
        // Update Redis
        redisUtil.updateJsonField(caseId, "status", status);
        if (status.equals("COMPLETE")) {
            log.info("model case " + caseId + " has finished!");
            try {
                quartzSchedulerManager.deleteJob(jobName, jobGroup);
                String resultUrl = modelServerProperties.getAddress() + modelServerProperties.getApis().get("result");
                JSONObject resultResponse = ProcessUtil.getModelCaseResult(resultUrl, caseId);
                JSONObject resObj = resultResponse.getJSONObject("data").getJSONObject("result");
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
    }
}
