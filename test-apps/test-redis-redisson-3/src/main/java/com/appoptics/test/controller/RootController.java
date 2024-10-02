package com.appoptics.test.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpSession;

@Controller
public class RootController {
    private static final String DEFAULT_HOSTS_STRING = "redis://127.0.0.1:7000 redis://127.0.0.1:7001 redis://127.0.0.1:7002";
    @GetMapping("/")
    public ModelAndView root(HttpSession session) {
        ModelAndView result = new ModelAndView();
//        Input input = new Input();
//        input.setHostsString(DEFAULT_HOSTS_STRING);
        session.setAttribute("hostsString", DEFAULT_HOSTS_STRING);
        session.setAttribute("host", AbstractRedissonController.host);
        session.setAttribute("port", AbstractRedissonController.port);
        result.setViewName("index");
        return result;
    }
}
