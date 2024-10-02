package com.appoptics.test.testspringboot;

import org.redisson.Redisson;
import org.redisson.api.RBucketReactive;
import org.redisson.api.RedissonReactiveClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.redisson.config.Config;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.scheduler.Schedulers;


@RestController
public class GreetingController {
    private Logger logger = LoggerFactory.getLogger(TestSpringBootApplication.class);

    @GetMapping("/hello")
    public String hello() {
        Config config = new Config();
        config.useSingleServer()
                // use "rediss://" for SSL connection
                .setAddress("redis://127.0.0.1:6379");
        RedissonReactiveClient redissonReactive = Redisson.createReactive(config);

        RBucketReactive<String> bucket = redissonReactive.getBucket("hello");
        bucket.set("world");

        bucket.get().subscribeOn(Schedulers.newElastic("redis-scheduler")).subscribe(System.out::println);
        return "hello";
    }
}