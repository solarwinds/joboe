package com.tracelytics.test.springboot;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;
import redis.clients.jedis.Jedis;

@Controller
public class TestGet extends AbstractJedisController {
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

    @GetMapping("/test-get")
    public ModelAndView test(Model model) {
        try (Jedis jedis = getJedis()) {
            jedis.get(LONG_STRING_KEY); //long string key
            jedis.get(LONG_BYTE_KEY); //long byte key
        }


        printToOutput("Finished testing GET");
        return getModelAndView("index");
    }
}