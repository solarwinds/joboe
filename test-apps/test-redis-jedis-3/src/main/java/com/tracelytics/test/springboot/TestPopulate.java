package com.tracelytics.test.springboot;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;
import redis.clients.jedis.Jedis;

@Controller
public class TestPopulate extends AbstractJedisController {
    private static final String LONG_STRING_KEY;
    private static final byte[] LONG_BYTE_KEY;
    static {
        StringBuffer longStringBuffer = new StringBuffer("long-string-key");
        for (int i = 0; i < 1000; i ++) {
            longStringBuffer.append(i);
        }
        LONG_STRING_KEY = longStringBuffer.toString();
        LONG_BYTE_KEY = LONG_STRING_KEY.getBytes();
    }

    @GetMapping("/test-populate")
    public ModelAndView test(Model model) {
        final int SIZE = 100000;
        String[] values = new String[SIZE];
        for (int i = 0; i < SIZE; i++) {
            values[i] = String.valueOf(Math.random());
        }

        try (Jedis jedis = getJedis()) {
            jedis.sadd("random-list", values);
        }


        printToOutput("Finished testing populate");
        return getModelAndView("index");
    }
}