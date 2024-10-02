package com.tracelytics.test.action;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;

import com.mongodb.CommandResult;
   
@Results({
    @Result(name="success", location="index.jsp"),
    @Result(name="error", location="index.jsp"),
})
public class TestCommand extends AbstractMongoDbAction {
    private static final String COMMAND = "ping";
	
    public String execute() throws Exception {
    	CommandResult result = getDb().command(COMMAND);
    	
    	addActionMessage("Called " + COMMAND + " and the result is :" + result.ok());
    	    	
    	return SUCCESS;
    }
}
