package com.tracelytics.test.springboot;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;
import redis.clients.jedis.*;

import java.util.ArrayList;
import java.util.List;

@Controller
public class TestShard extends AbstractJedisController {
    @GetMapping("/test-shard")
    public ModelAndView test(Model model) {
        List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>();
        JedisShardInfo si = new JedisShardInfo(host, port);
        shards.add(si);
        si = new JedisShardInfo(host, 6380);
        shards.add(si);

        ShardedJedisPool pool = new ShardedJedisPool(new JedisPoolConfig(), shards);

        try (ShardedJedis jedis = pool.getResource()) {
            jedis.set(STRING_KEY, "testing");
        }

        printToOutput("Finished testing shard");
        return getModelAndView("index");
    }
}