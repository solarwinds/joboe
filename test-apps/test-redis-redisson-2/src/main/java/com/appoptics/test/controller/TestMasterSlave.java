package com.appoptics.test.controller;

import org.redisson.Redisson;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class TestMasterSlave extends AbstractRedissonController {
    protected static final String REDIS_SLAVE_PORT = "6381";

    @GetMapping("/test-master-slave")
    protected ModelAndView test() {
        Config config = new Config();
        config.useMasterSlaveServers().setMasterAddress(host + ":" + port).addSlaveAddress(host + ":" + REDIS_SLAVE_PORT);
        RedissonClient client = Redisson.create(config);

        RBucket<String> bucket = client.getBucket(KEY);
        bucket.set(VALUE);

        bucket = client.getBucket(KEY);
        bucket.get();

        printToOutput("Finished testing master-slave");


        return getModelAndView("index");
    }

}
