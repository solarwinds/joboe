package com.appoptics.test.controller;

import org.redisson.Redisson;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class TestSentinel extends AbstractRedissonController {
    protected static final String REDIS_SENTINEL_PORT = "26379";
    protected static final String REDIS_MASTER_NAME = "mymaster";

    @GetMapping("/test-sentinel")
    protected ModelAndView test() {
        Config config = new Config();
        config.useSentinelServers().addSentinelAddress(host + ":" + REDIS_SENTINEL_PORT).setMasterName(REDIS_MASTER_NAME);
        RedissonClient client = Redisson.create(config);
        
        RBucket<String> bucket = client.getBucket(KEY);
        bucket.set(VALUE);
        
        bucket = client.getBucket(KEY);
        bucket.get();
        
        printToOutput("Finished testing sentinel");

        return getModelAndView("index");
    }

}
