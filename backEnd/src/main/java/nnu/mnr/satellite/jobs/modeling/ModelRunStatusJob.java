package nnu.mnr.satellite.jobs.modeling;

import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.jobs.QuartzSchedulerManager;
import nnu.mnr.satellite.model.pojo.modeling.ModelServerProperties;
import nnu.mnr.satellite.utils.common.BeanUtil;
import nnu.mnr.satellite.utils.common.ProcessUtil;
import nnu.mnr.satellite.utils.data.RedisUtil;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;

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
        String status = ProcessUtil.getModelCaseStatus(statusUrl, caseId);
        // Update Redis
        redisUtil.updateJsonField(caseId, "status", status);
        if (status.equals("COMPLETE")) {
            log.info("model case " + caseId + " has finished!");
            try {
                quartzSchedulerManager.deleteJob(jobName, jobGroup);
                String resultUrl = modelServerProperties.getAddress() + modelServerProperties.getApis().get("result");
                JSONObject resObj = ProcessUtil.getModelCaseResult(resultUrl, caseId);
                redisUtil.updateJsonField(caseId, "result", resObj);
                redisUtil.updateJsonField(caseId, "end", LocalDateTime.now());
            } catch (SchedulerException e) {
                log.info(e.toString());
            }
        }
        // TODO: Add Other Conditions
    }
}
