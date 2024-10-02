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
public class SetHost extends AbstractRedissonController {
    @GetMapping("/set-host")
    protected ModelAndView test(@RequestParam("host")String host, @RequestParam("port")String port, HttpSession session) {
        session.setAttribute("host", host);
        session.setAttribute("port", port);

        AbstractRedissonController.host = host;
        AbstractRedissonController.port = Integer.parseInt(port);

        clearExtendedOutput();
        printToOutput("Set host and port to " + host + ":" + port);

        return getModelAndView("index");
    }

}
