package com.tracelytics.test.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class AnnotatedController {

    //@RequestMapping(method = RequestMethod.GET)
    @RequestMapping("/annotated1")
    public String printWelcome(ModelMap model) {
        model.addAttribute("message", "Spring 3 MVC Welcome");
        return "hello";
    }

    @RequestMapping("/annotated2")
    public String printHello(ModelMap model) {
        model.addAttribute("message", "test2");
        return "hello";
    }
}
