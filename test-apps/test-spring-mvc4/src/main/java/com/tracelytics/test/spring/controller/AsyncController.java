package com.tracelytics.test.spring.controller;

import java.util.concurrent.Callable;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AsyncController { 
    private static final int MAX_DURATION = 10;
    
    @RequestMapping("/async")
    public Callable<String> callAsync(@RequestParam(value="duration", required=false, defaultValue="1") String duration, Model model) {
        int durationValue;
        try {
            durationValue = Integer.parseInt(duration);
            
            if (durationValue > MAX_DURATION) {
                durationValue = MAX_DURATION;
            } else if (durationValue <= 1) {
                durationValue = 1;
            }
        } catch (NumberFormatException e) {
            durationValue = 1;
        }
        
        model.addAttribute("durationWaited", durationValue);
        
        return new DelayedCallable(durationValue);
     }
    
    private class DelayedCallable implements Callable<String> {
        private int durationInt;
        
        public DelayedCallable(int durationInt) {
            this.durationInt = durationInt;

        }
        public String call() throws Exception {
            Thread.sleep(durationInt * 1000);
            return "async";
        } 
    }
}
