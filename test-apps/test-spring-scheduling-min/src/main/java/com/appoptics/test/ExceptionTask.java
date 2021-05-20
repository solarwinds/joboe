package com.appoptics.test;

import org.springframework.scheduling.annotation.Scheduled;

public class ExceptionTask {
    public void triggerException() {
        throw new RuntimeException("test");
    }
}