package com.tracelytics.test.controller;

import org.springframework.web.servlet.mvc.*;
import org.springframework.web.servlet.*;
import javax.servlet.http.*;

public class SimpleController extends AbstractController {

    public ModelAndView handleRequestInternal(
        HttpServletRequest request,
        HttpServletResponse response) throws Exception {

        ModelAndView mav = new ModelAndView("hello");
        mav.addObject("message", "Simple Controller");

        new Exception().printStackTrace();
        return mav;        
    }
}

