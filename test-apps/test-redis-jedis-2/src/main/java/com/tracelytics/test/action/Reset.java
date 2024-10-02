package com.tracelytics.test.action;

import redis.clients.jedis.Jedis;

@SuppressWarnings("serial")
@org.apache.struts2.convention.annotation.Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
})
public class Reset extends AbstractJedisAction {

    @Override
    protected String test(Jedis jedis) throws Exception {
        if (initialize()) {
            printToOutput("Initialized Redis");
        } else {
            printToOutput("Failed to initialize Redis");
        }
        
        return SUCCESS;
    }

}
