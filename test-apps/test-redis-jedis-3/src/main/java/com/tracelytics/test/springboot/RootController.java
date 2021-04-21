package com.tracelytics.test.springboot;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpSession;

@Controller
public class RootController extends AbstractJedisController {
    private static final String DEFAULT_HOSTS_STRING = "52.7.124.5:17000 52.7.124.5:17001 52.7.124.5:17002";
    private static final String DEFAULT_SHARD_STRING = "52.7.124.5:1250 52.7.124.5:1252";
    private static final String DEFAULT_SENTINEL = "52.7.124.5:1253";

    @GetMapping("/")
    public ModelAndView root(HttpSession session) {
        ModelAndView result = new ModelAndView();
//        Input input = new Input();
//        input.setHostsString(DEFAULT_HOSTS_STRING);
        session.setAttribute("hostsString", DEFAULT_HOSTS_STRING);
        session.setAttribute("shardString", DEFAULT_SHARD_STRING);
        session.setAttribute("sentinel", DEFAULT_SENTINEL);
        session.setAttribute("host", host);
        session.setAttribute("port", port);
        result.setViewName("index");
        return result;
    }
}
