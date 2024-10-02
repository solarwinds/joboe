package com.tracelytics.test.action;

import redis.clients.jedis.Jedis;

@SuppressWarnings("serial")
@org.apache.struts2.convention.annotation.Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
})
public class TestSet extends AbstractJedisAction {

    @Override
    protected String test(Jedis jedis) throws Exception {
        jedis.set(STRING_KEY, "testing");
        
        printToOutput("Set key [" + STRING_KEY + "] to value \"testing\"");
        return SUCCESS;
    }

}
