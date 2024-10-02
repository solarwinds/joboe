package com.appoptics.test.controller;


import org.redisson.client.RedisClient;
import org.redisson.client.RedisClientConfig;
import org.redisson.client.RedisConnection;
import org.redisson.client.protocol.RedisCommands;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class Reset extends AbstractRedissonController {

    @GetMapping("/reset")
    public ModelAndView reset(Model model) {
        RedisClientConfig config = new RedisClientConfig();
        config.setAddress(host, port);
        RedisClient client = RedisClient.create(config);

        RedisConnection connection = client.connect();
        connection.sync(RedisCommands.FLUSHALL);
        try {
            connection.closeAsync().await();
            printToOutput("Reset redis");
        } catch (InterruptedException e) {
            e.printStackTrace();
            printToOutput("Failed to initialize Redis: ", e);
        }

        return getModelAndView("index");
    }

}
