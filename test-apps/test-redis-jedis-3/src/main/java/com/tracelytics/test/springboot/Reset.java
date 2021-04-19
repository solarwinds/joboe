package com.tracelytics.test.springboot;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class Reset extends AbstractJedisController {

    @GetMapping("/reset")
    public ModelAndView reset(Model model) {
        if (initialize()) {
            printToOutput("Initialized Redis");
        } else {
            printToOutput("Failed to initialize Redis");
        }

        return getModelAndView("index");
    }
}
