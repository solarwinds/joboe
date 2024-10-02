package com.appoptics.test.controller;

import org.redisson.Redisson;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpSession;

@Controller
public class TestCluster extends AbstractRedissonController {
    //private static final String[] REDIS_NODE_ADDRESSES = new String[] { host + ":7000" };//, host + ":7001", host + ":7002" };
    //private static final String[] REDIS_NODE_ADDRESSES = new String[] { "redis://t2" + ":17000" , "redis://t2" + ":17001", "redis://t2" + ":17002" };

    @GetMapping("/test-cluster")
    protected ModelAndView test(@RequestParam("hostsString")String hostsString, HttpSession session) {
        Config config = new Config();
        String[] hosts = hostsString.split("\\s");
        session.setAttribute("hostsString", hostsString);

        config.useClusterServers().addNodeAddress(hosts);
        RedissonClient client = Redisson.create(config);

        for (int i = 0; i < 10; i++) {
            RBucket<Integer> bucket = client.getBucket(String.valueOf(i));
            bucket.set(i);
        }

        for (int i = 0; i < 10; i++) {
            RBucket<Integer> bucket = client.getBucket(String.valueOf(i));
            System.out.println(bucket.get());
        }

        clearExtendedOutput();
        printToOutput("Finished testing cluster");



        return getModelAndView("index");
    }

}
