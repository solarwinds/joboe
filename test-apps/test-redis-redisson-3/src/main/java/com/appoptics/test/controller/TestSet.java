package com.appoptics.test.controller;

import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.UUID;

@Controller
public class TestSet extends AbstractRedissonController {
    @GetMapping("/test-set")
    public ModelAndView test(Model model) {
        RedissonClient client = getClient();
        RBucket bucket = client.getBucket(KEY);
        bucket.set(VALUE);

        bucket = client.getBucket(KEY);
        bucket.set(1);

        bucket = client.getBucket(KEY);
        bucket.set(1.15);

        bucket = client.getBucket(KEY);
        bucket.set(true);

        bucket = client.getBucket(KEY);
        bucket.set(UUID.randomUUID());

        bucket = client.getBucket(KEY);
        bucket.set(VALUE);

        printToOutput("Finished testing SET");
        return getModelAndView("index");
    }
}