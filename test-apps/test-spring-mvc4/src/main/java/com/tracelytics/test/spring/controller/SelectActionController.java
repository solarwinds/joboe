package com.tracelytics.test.spring.controller;

import java.util.concurrent.TimeUnit;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class SelectActionController { 

    @RequestMapping("/action1")
    public ResponseEntity<String> action1(@RequestParam(value="duration", required=false) Integer duration, 
                                          @RequestParam(value="status", required=false, defaultValue="200") int status, 
                                          Model model) {
        sleep(duration);
        return new ResponseEntity<String>("hello o/", HttpStatus.valueOf(status));
    }
    
    @RequestMapping("/action2")
    public ResponseEntity<String> action2(@RequestParam(value="duration", required=false) Integer duration, 
                                          @RequestParam(value="status", required=false, defaultValue="200") int status, 
                                          Model model) {
        sleep(duration);
        return new ResponseEntity<String>("hello o/", HttpStatus.valueOf(status));
    }
    
    @RequestMapping("/action3")
    public ResponseEntity<String> action3(@RequestParam(value="duration", required=false) Integer duration, 
                                          @RequestParam(value="status", required=false, defaultValue="200") int status, 
                                          Model model) {
        sleep(duration);
        return new ResponseEntity<String>("hello o/", HttpStatus.valueOf(status));
    }
    
    @RequestMapping("/action4")
    public ResponseEntity<String> action4(@RequestParam(value="duration", required=false) Integer duration, 
                                          @RequestParam(value="status", required=false, defaultValue="200") int status, 
                                          Model model) {
        sleep(duration);
        return new ResponseEntity<String>("hello o/", HttpStatus.valueOf(status));
    }
    
    @RequestMapping("/action5")
    public ResponseEntity<String> action5(@RequestParam(value="duration", required=false) Integer duration, 
                                          @RequestParam(value="status", required=false, defaultValue="200") int status, 
                                          Model model) {
        sleep(duration);
        return new ResponseEntity<String>("hello o/", HttpStatus.valueOf(status));
    }
    
    @RequestMapping("/action6")
    public ResponseEntity<String> action6(@RequestParam(value="duration", required=false) Integer duration, 
                                          @RequestParam(value="status", required=false, defaultValue="200") int status, 
                                          Model model) {
        sleep(duration);
        return new ResponseEntity<String>("hello o/", HttpStatus.valueOf(status));
    }
    
    @RequestMapping("/action7")
    public ResponseEntity<String> action7(@RequestParam(value="duration", required=false) Integer duration, 
                                          @RequestParam(value="status", required=false, defaultValue="200") int status, 
                                          Model model) {
        sleep(duration);
        return new ResponseEntity<String>("hello o/", HttpStatus.valueOf(status));
    }
    
    @RequestMapping("/action8")
    public ResponseEntity<String> action8(@RequestParam(value="duration", required=false) Integer duration, 
                                          @RequestParam(value="status", required=false, defaultValue="200") int status, 
                                          Model model) {
        sleep(duration);
        return new ResponseEntity<String>("hello o/", HttpStatus.valueOf(status));
    }
    
    @RequestMapping("/action9")
    public ResponseEntity<String> action9(@RequestParam(value="duration", required=false) Integer duration, 
                                          @RequestParam(value="status", required=false, defaultValue="200") int status, 
                                          Model model) {
        sleep(duration);
        return new ResponseEntity<String>("hello o/", HttpStatus.valueOf(status));
    }
    
    @RequestMapping("/action10")
    public ResponseEntity<String> action10(@RequestParam(value="duration", required=false) Integer duration, 
                                          @RequestParam(value="status", required=false, defaultValue="200") int status, 
                                          Model model) {
        sleep(duration);
        return new ResponseEntity<String>("hello o/", HttpStatus.valueOf(status));
    }
    
    private void sleep(Integer duration) {
        if (duration != null) {
            try {
                TimeUnit.MILLISECONDS.sleep(duration);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

}
