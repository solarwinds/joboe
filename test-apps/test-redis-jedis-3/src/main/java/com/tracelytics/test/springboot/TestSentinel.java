package com.tracelytics.test.springboot;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;

import java.util.Collections;

@Controller
public class TestSentinel extends AbstractJedisController {
    private static final int SENTINEL_PORT = 26379;

    @GetMapping("/test-sentinel")
    protected ModelAndView test() {
        JedisSentinelPool pool = new JedisSentinelPool("mymaster", Collections.singleton(host + ":" + port));
        try (Jedis jedis = pool.getResource()) {
            try (Jedis sentinelJedis = new Jedis(host, SENTINEL_PORT)) {
                sentinelJedis.sentinelFailover("mymaster");
                sentinelJedis.sentinelGetMasterAddrByName("mymaster");
                sentinelJedis.sentinelMasters();
                //sentinelJedis.sentinelMonitor(masterName, ip, port, quorum);
                //        sentinelJedis.sentinelRemove("not-exist");
                sentinelJedis.sentinelReset("*");
                sentinelJedis.sentinelSet("mymaster", Collections.singletonMap("down-after-milliseconds", "1000"));
                sentinelJedis.sentinelSlaves("mymaster");
            }
        }



        printToOutput("Finished testing sentinel");

        return getModelAndView("index");
    }

}
