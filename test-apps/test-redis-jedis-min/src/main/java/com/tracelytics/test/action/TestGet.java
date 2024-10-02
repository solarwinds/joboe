package com.tracelytics.test.action;

import redis.clients.jedis.Jedis;

@SuppressWarnings("serial")
@org.apache.struts2.convention.annotation.Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
})
public class TestGet extends AbstractJedisAction {
    private static final String LONG_STRING_KEY;
    static {
        StringBuffer longStringBuffer = new StringBuffer("long-string-key");
        for (int i = 0; i < 1000; i ++) {
            longStringBuffer.append(i);
        }
        LONG_STRING_KEY = longStringBuffer.toString();
    }
    
    
    @Override
    protected String test(Jedis jedis) throws Exception {
        jedis.get(LONG_STRING_KEY); //long string key
        
        printToOutput("Get operation result: ", jedis.get(STRING_KEY));
        return SUCCESS;
    }

}
