package com.appoptics.test.controller;

import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisClient;
import org.redisson.client.RedisClientConfig;
import org.redisson.client.RedisConnection;
import org.redisson.client.RedisException;
import org.redisson.client.codec.Codec;
import org.redisson.client.codec.LongCodec;
import org.redisson.client.codec.StringCodec;
import org.redisson.client.protocol.RedisCommand;
import org.redisson.client.protocol.RedisCommands;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.concurrent.ExecutionException;

@Controller
public class TestGet extends AbstractRedissonController {
    private static final String LONG_STRING_KEY;
    static {
        StringBuffer longStringBuffer = new StringBuffer("long-string-key");
        for (int i = 0; i < 1000; i ++) {
            longStringBuffer.append(i);
        }
        LONG_STRING_KEY = longStringBuffer.toString();
    }

    @GetMapping("/test-get")
    public ModelAndView test(Model model) {
        RedissonClient client = getClient();
        RBucket<String> bucket = client.getBucket(KEY);
        bucket.get();

        RBucket<String> notFoundBucket = client.getBucket("not-exist");
        notFoundBucket.get();

        notFoundBucket = client.getBucket(LONG_STRING_KEY);
        notFoundBucket.get();

        printToOutput("Finished testing GET");
        return getModelAndView("index");
    }
}