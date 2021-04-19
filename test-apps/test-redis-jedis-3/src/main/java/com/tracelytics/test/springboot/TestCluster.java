package com.tracelytics.test.springboot;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

import javax.servlet.http.HttpSession;
import java.util.HashSet;
import java.util.Set;

@Controller
public class TestCluster extends AbstractJedisController {
    //private static final String[] REDIS_NODE_ADDRESSES = new String[] { host + ":7000" };//, host + ":7001", host + ":7002" };
    //private static final String[] REDIS_NODE_ADDRESSES = new String[] { "redis://t2" + ":17000" , "redis://t2" + ":17001", "redis://t2" + ":17002" };

    @GetMapping("/test-cluster")
    protected ModelAndView test(@RequestParam("hostsString")String hostsString, HttpSession session) {
        String[] hosts = hostsString.split("\\s");
        session.setAttribute("hostsString", hostsString);

        Set<HostAndPort> hostAndPorts = new HashSet<>();
        for (String host : hosts) {
            hostAndPorts.add(HostAndPort.from(host));
        }

        JedisCluster cluster = new JedisCluster(hostAndPorts);


        for (int i = 0 ; i < 10; i++) {
            String string = String.valueOf(i);
            cluster.set(string, string);
        }

        for (int i = 0 ; i < 10; i++) {
            String string = String.valueOf(i);
            cluster.get(string);
        }


        printToOutput("Finished testing cluster");
        clearExtendedOutput();

        return getModelAndView("index");
    }

}
