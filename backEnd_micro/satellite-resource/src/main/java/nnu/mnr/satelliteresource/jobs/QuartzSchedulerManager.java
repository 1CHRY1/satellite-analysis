package nnu.mnr.satelliteresource.jobs;

import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satelliteresource.jobs.modeling.ModelRunStatusJob;
import nnu.mnr.satelliteresource.model.properties.ModelServerProperties;
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

    public void startModelRunningStatusJob(String caseId) throws SchedulerException {
        log.info("start Modeltaskjob "+ caseId +" here");
        modelRunningStatusJob(scheduler, caseId);
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

}
