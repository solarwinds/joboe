package com.appoptics.test.action;

import com.appoptics.test.model.RequestForm;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.net.http.HttpRequest;


@Controller
@RequestMapping(value = "/")
public class Index {
   @GetMapping("")
    final ModelAndView getIndex() {
        return new ModelAndView("index", "request", new RequestForm());
    }
}
