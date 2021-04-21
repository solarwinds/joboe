package com.tracelytics.test.springboot;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;

import javax.servlet.http.HttpSession;
import java.util.Collections;

@Controller
public class TestSentinel extends AbstractJedisController {
    private static final int SENTINEL_PORT = 26379;
    public static final String MASTER_NAME = "mymaster";

    @GetMapping("/test-sentinel")
    public ModelAndView test(@RequestParam("sentinel")String sentinel, HttpSession session) {
        session.setAttribute("sentinel", sentinel);
//        JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, Collections.singleton(sentinel));
//        try (Jedis sentinelJedis = pool.getResource()) {
        try (Jedis sentinelJedis = new Jedis(HostAndPort.from(sentinel))) {
            sentinelJedis.sentinelFailover(MASTER_NAME);
            sentinelJedis.sentinelGetMasterAddrByName(MASTER_NAME);
            sentinelJedis.sentinelMasters();
            //sentinelJedis.sentinelMonitor(masterName, ip, port, quorum);
            //        sentinelJedis.sentinelRemove("not-exist");
            sentinelJedis.sentinelReset("*");
            sentinelJedis.sentinelSet(MASTER_NAME, Collections.singletonMap("down-after-milliseconds", "1000"));
            sentinelJedis.sentinelSlaves(MASTER_NAME);
        }
//    }



        printToOutput("Finished testing sentinel");

        return getModelAndView("index");
    }

}
