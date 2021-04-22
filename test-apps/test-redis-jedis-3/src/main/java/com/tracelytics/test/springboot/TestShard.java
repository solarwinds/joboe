package com.tracelytics.test.springboot;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import redis.clients.jedis.*;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

@Controller
public class TestShard extends AbstractJedisController {
    @GetMapping("/test-shard")
    public ModelAndView test(@RequestParam("shardString")String hostsString, HttpSession session) {
        String[] hosts = hostsString.split("\\s");
        session.setAttribute("shardString", hostsString);

        List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>();
        for (String host : hosts) {
            JedisShardInfo si = new JedisShardInfo(HostAndPort.from(host));
            shards.add(si);
        }
        ShardedJedisPool pool = new ShardedJedisPool(new GenericObjectPoolConfig<ShardedJedis>(), shards);

        try (ShardedJedis jedis = pool.getResource()) {
            for (int i = 0; i < 10; i++) {
                jedis.set(STRING_KEY + i, "testing");
            }
        }

        printToOutput("Finished testing shard");
        return getModelAndView("index");
    }
}