package com.tracelytics.test.spring.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.appoptics.api.ext.Trace;

@Controller
public class HelloWorldValidTransactionNameController { 

    @RequestMapping("/hello-valid-transaction-name")
    public String hello(@RequestParam(value="name", required=false, defaultValue="World") String name, Model model) {
        model.addAttribute("name", name);
        
        Trace.setTransactionName("my-transaction-name");
        return "helloworld";
    }

}
