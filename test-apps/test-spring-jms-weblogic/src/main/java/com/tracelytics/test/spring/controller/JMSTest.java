package com.tracelytics.test.spring.controller;


import com.tracelytics.test.spring.queue.QueueSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class JMSTest {
    @Autowired
    QueueSender sender;

    @RequestMapping(value="/send", method = RequestMethod.GET)
    public String testMethod(@RequestParam(defaultValue = "works!") String message) {
        sender.sendToQueue(message); // TODO
        return "index";
    }
}