package com.tracelytics.test.controller;

import org.springframework.web.servlet.mvc.*;
import org.springframework.web.servlet.mvc.multiaction.*;
import org.springframework.web.servlet.*;
import javax.servlet.http.*;

public class MultiController extends MultiActionController {

    public ModelAndView add(
        HttpServletRequest request,
        HttpServletResponse response) throws Exception {

        ModelAndView mav = new ModelAndView("hello");
        mav.addObject("message", "MultiAction Controller: add");

        new Exception().printStackTrace();
        return mav;        
    }

    public ModelAndView delete(
        HttpServletRequest request,
        HttpServletResponse response) throws Exception {

        ModelAndView mav = new ModelAndView("hello");
        mav.addObject("message", "MultiAction Controller: delete");

        new Exception().printStackTrace();
        return mav;        
    }

    public ModelAndView list(
        HttpServletRequest request,
        HttpServletResponse response) throws Exception {

        ModelAndView mav = new ModelAndView("hello");
        mav.addObject("message", "MultiAction Controller: list");

        new Exception().printStackTrace();
        return mav;        
    }

    public ModelAndView update(
        HttpServletRequest request,
        HttpServletResponse response) throws Exception {

        ModelAndView mav = new ModelAndView("hello");
        mav.addObject("message", "MultiAction Controller: update");

        new Exception().printStackTrace();
        return mav;        
    }
}

