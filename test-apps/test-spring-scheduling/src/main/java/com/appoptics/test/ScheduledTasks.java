package com.appoptics.test;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.appoptics.api.ext.Trace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledTasks {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTasks.class);

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    @Scheduled(fixedRate = 10000)
    public void reportCurrentTime() {
        Trace.createEntryEvent("test-context").report();
        log.info("The time is now {}", dateFormat.format(new Date()));
        Trace.createExitEvent("test-context").report();
    }

    @Scheduled(cron = "0 * * * * *")
    public void throwException() {
        throw new RuntimeException("Testing");
    }

}