package com.tracelytics.test.spring.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.async.DeferredResult;

@Controller
public class DeferredController { 
    private static final int MAX_DURATION = 10;
    
    @RequestMapping("/deferred")
    @ResponseBody
    public DeferredResult<String> callDeferred(@RequestParam(value="duration", required=false, defaultValue="1") String duration, Model model) {
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
        
        final DeferredResult<String> result = new DeferredResult<String>();
        
        new DeferredThread(durationValue, result).start();
        
        return result;
     }
    
    private class DeferredThread extends Thread {
        private int durationInt;
        private DeferredResult<String> result;
        
        public DeferredThread(int durationInt, DeferredResult<String> result) {
            this.durationInt = durationInt;
            this.result = result;
        }
        
        @Override
        public void run() {
            try {
                Thread.sleep(durationInt * 1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            result.setResult("Deferred " + durationInt + " sec(s)");
        }
    }
}
