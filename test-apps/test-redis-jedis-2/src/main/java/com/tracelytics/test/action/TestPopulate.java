package com.tracelytics.test.action;

import redis.clients.jedis.Jedis;

@SuppressWarnings("serial")
@org.apache.struts2.convention.annotation.Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
})
public class TestPopulate extends AbstractJedisAction {
    @Override
    protected String test(Jedis jedisO) throws Exception {
        //initialize();
        final int SIZE = 100000;
        String[] values = new String[SIZE];
        for (int i = 0; i < SIZE; i++) {
            values[i] = String.valueOf(Math.random());
        }
        jedisO.sadd("random-list", values);
         
        return SUCCESS;
    }
}

