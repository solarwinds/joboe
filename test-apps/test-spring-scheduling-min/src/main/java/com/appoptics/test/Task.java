package com.appoptics.test;

import org.springframework.scheduling.annotation.Scheduled;

public class Task {
    @Scheduled(fixedRate=10000)
    public void sayHello() {
        System.out.println("Hello !!! ");
    }
}