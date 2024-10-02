package com.appoptics.test;

import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SimpleTrigger;
import org.quartz.impl.StdSchedulerFactory;

import java.util.Date;

public class TestErrorJob {
    public static void main(String[] args) throws Exception {
        JobDetail job = new JobDetail();
        job.setName("test-error-job");
        job.setJobClass(ErrorJob.class);
        job.setGroup("my-group");

        //configure the scheduler time
        SimpleTrigger trigger = new SimpleTrigger("my-trigger", "my-group");
        trigger.setStartTime(new Date(System.currentTimeMillis() + 1000));
        trigger.setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY);
        trigger.setRepeatInterval(30000);

        //schedule it
        Scheduler scheduler = new StdSchedulerFactory().getScheduler();
        scheduler.start();
        scheduler.scheduleJob(job, trigger);

    }
}
