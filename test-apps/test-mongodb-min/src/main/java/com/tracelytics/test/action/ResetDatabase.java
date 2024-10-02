package com.tracelytics.test.action;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;

import com.mongodb.DB;


@Results({
    @Result(name="success", location="index.jsp"),
    @Result(name="error", location="index.jsp"),
    @Result(name="input", location="index.jsp")
})
public class ResetDatabase extends AbstractMongoDbAction {
    public String execute() throws Exception {
    	DB db = getDb();
    	
    	if (db != null) {
    	    db.dropDatabase();
    	}
    	
    	addActionMessage("Reset the database");
    	
    	return SUCCESS;
    }
    
    
   
}
