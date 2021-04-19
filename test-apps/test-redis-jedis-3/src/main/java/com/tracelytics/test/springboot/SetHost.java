package com.tracelytics.test.springboot;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpSession;

@Controller
public class SetHost extends AbstractJedisController {
    @GetMapping("/set-host")
    protected ModelAndView test(@RequestParam("host")String host, @RequestParam("port")String port, HttpSession session) {
        session.setAttribute("host", host);
        session.setAttribute("port", port);

        super.host = host;
        super.port = Integer.parseInt(port);

        clearExtendedOutput();
        printToOutput("Set host and port to " + host + ":" + port);

        return getModelAndView("index");
    }

}
