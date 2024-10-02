package com.tracelytics.test.spring.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Work with javaagent config
 * 
 * "agent.urlSampleRates": 
    [
        {
            ".*no-trace.*" : {
               "tracingMode" : "never"
            }
        }
        ,
        {
            ".*no-metrics.*" : {
               "metricsEnabled" : false
            }
        }
    ],
 * @author pluk
 *
 */
@Controller
public class HelloWorldNoMetricsController { 

    @RequestMapping("/hello-no-metrics")
    public String helloNoMetrics(@RequestParam(value="name", required=false, defaultValue="No metrics") String name, Model model) {
        model.addAttribute("name", name);
        return "helloworld";
    }

}
