package com.tracelytics.test.action;

import java.util.List;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
   
@Results({
    @Result(name="success", location="index.jsp"),
    @Result(name="error", location="index.jsp"),
})
public class TestDistinct extends AbstractMongoDbAction {
	private static final String KEY = "key";
	
    public String execute() throws Exception {
    	List<Object> keys = getCollection().distinct(KEY);
    	
    	addActionMessage("Found " + keys.size() + " distinct values for key [" + KEY + "]");
    	    	
    	return SUCCESS;
    }
}
