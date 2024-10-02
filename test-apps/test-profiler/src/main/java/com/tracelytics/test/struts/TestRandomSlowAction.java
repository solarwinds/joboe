package com.tracelytics.test.struts;

import com.opensymphony.xwork2.ActionSupport;

/**
 * This is a test Struts action.  It extends ActionSupport, which implements the Action interface.
 */
public class TestRandomSlowAction extends ActionSupport {
    private static final double ODDS = 0.01;
    public String execute() {
        if (Math.random() < ODDS) {
            slowMethod();
        }
        return SUCCESS;
    }
    
    private void slowMethod() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
