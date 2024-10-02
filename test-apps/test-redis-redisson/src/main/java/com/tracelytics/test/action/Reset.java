package com.tracelytics.test.action;


@SuppressWarnings("serial")
@org.apache.struts2.convention.annotation.Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
})
public class Reset extends AbstractRedissonAction {

    @Override
    protected String test() throws Exception {
        if (initializeRedis()) {
            printToOutput("Initialized Redis");
        } else {
            printToOutput("Failed to initialize Redis");
        }
        
        return SUCCESS;
    }

}
