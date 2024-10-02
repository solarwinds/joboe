package com.tracelytics.test.spring.controller;

import com.appoptics.api.ext.Trace;
import com.appoptics.api.ext.TraceEvent;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ManyEventsController {
    private static final Integer DEFAULT_EVENT_COUNT = 10000;
    private static String LONG_STRING = getLongString(256);


    @RequestMapping("/many-events")
    public String hello(@RequestParam(value="name", required=false, defaultValue="World") String name,
                        @RequestParam(value="event-count", required=false) Integer inputEventCount,
                        Model model) {
        int eventCount = inputEventCount != null ? inputEventCount : DEFAULT_EVENT_COUNT;

        for (int i = 0 ; i < eventCount; i++) {
            TraceEvent infoEvent = Trace.createInfoEvent(null);
            infoEvent.addInfo("test-key", LONG_STRING);
            infoEvent.report();
        }

        //create
        model.addAttribute("name", name);
        return "helloworld";
    }

    private static String getLongString(int length) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0 ; i < length; i++) {
            stringBuilder.append("A");
        }
        return stringBuilder.toString();
    }
}
