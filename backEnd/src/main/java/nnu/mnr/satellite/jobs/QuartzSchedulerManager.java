package nnu.mnr.satellite.jobs;

import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.jobs.modeling.ModelRunStatusJob;
import nnu.mnr.satellite.model.pojo.modeling.BaseModelServerProperties;
import nnu.mnr.satellite.model.pojo.modeling.ModelServerProperties;
import nnu.mnr.satellite.model.pojo.modeling.SRModelServerProperties;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/21 9:21
 * @Description:
 */

@Component("QuartzManager")
@Slf4j
public class QuartzSchedulerManager {

    @Autowired
    ModelServerProperties modelServerProperties;

    @Autowired
    @Lazy
    private Scheduler scheduler;

    @Autowired
    SRModelServerProperties srModelServerProperties;

    public void startModelRunningStatusJob(String caseId, BaseModelServerProperties serverProperties) throws SchedulerException {
        log.info("start Modeltaskjob "+ caseId +" here");
        modelRunningStatusJob(scheduler, caseId, serverProperties);
        scheduler.start();
    }

    public void deleteJob(String name, String group) throws SchedulerException {
        JobKey jobKey = new JobKey(name, group);
        JobDetail jobDetail = scheduler.getJobDetail(jobKey);
        if (jobDetail == null)
            return;
        scheduler.deleteJob(jobKey);
    }

    private void modelRunningStatusJob(Scheduler scheduler, String caseId) throws SchedulerException {
        JobDetail jobDetail = JobBuilder.newJob(ModelRunStatusJob.class)
                .withIdentity(caseId, "modelStatusGroup")
                .build();
        jobDetail.getJobDataMap().put("modelServerProperties", modelServerProperties);
        jobDetail.getJobDataMap().put("caseId", caseId);
        int interval = modelServerProperties.getInterval().get("status");
        SimpleTrigger simpleTrigger = TriggerBuilder.newTrigger().withIdentity(caseId+"_trigger","modelTriggerGroup")
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(interval).repeatForever()).build();
        scheduler.scheduleJob(jobDetail, simpleTrigger);
    }

    private void modelRunningStatusJob(Scheduler scheduler, String caseId, BaseModelServerProperties serverProperties) throws SchedulerException {
        JobDetail jobDetail = JobBuilder.newJob(ModelRunStatusJob.class)
                .withIdentity(caseId, "modelStatusGroup")
                .build();
        jobDetail.getJobDataMap().put("serverProperties", serverProperties);
        jobDetail.getJobDataMap().put("caseId", caseId);
        int interval = serverProperties.getInterval().get("status");
        SimpleTrigger simpleTrigger = TriggerBuilder.newTrigger().withIdentity(caseId+"_trigger","modelTriggerGroup")
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(interval).repeatForever()).build();
        scheduler.scheduleJob(jobDetail, simpleTrigger);
    }

}
