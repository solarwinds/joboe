package com.tracelytics.test.action;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;

import com.datastax.driver.core.Session;
   
@SuppressWarnings("serial")
@Results({
    @Result(name="success", location="index.jsp"),
    @Result(name="error", location="index.jsp"),
})
public class TestExceptionAsync extends AbstractCqlAction {
    @Override
    protected String test(Session session) {
        session.executeAsync("SELECT * FROM " + TEST_TABLE + " WHERE albummmm  = 'OMG'").getUninterruptibly();
        
        //should throw exception as albummmm is not a valid indexed column ... will not run statement below
        addActionMessage("Should throw exception as albummmm is not a valid column!!");
    
        return SUCCESS;
    }
}
