package com.tracelytics.test.springboot;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;
import redis.clients.jedis.Jedis;

@Controller
public class TestSet extends AbstractJedisController {
    @GetMapping("/test-set")
    public ModelAndView test(Model model) {
        try (Jedis jedis = getJedis()) {
            jedis.set(STRING_KEY, "testing");
        }

        printToOutput("Finished testing SET");
        return getModelAndView("index");
    }
}