package com.tracelytics.test.action;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;

import com.mongodb.client.MongoDatabase;


@Results({
    @Result(name="success", location="index.jsp"),
    @Result(name="error", location="index.jsp"),
    @Result(name="input", location="index.jsp")
})
public class ResetDatabase extends AbstractMongoDbSyncAction {
    public String execute() throws Exception {
    	MongoDatabase mongoDatabase = getDatabase();
    	
    	if (mongoDatabase != null) {
    	    mongoDatabase.drop();
    	}
    	
    	addActionMessage("Reset the database");
    	
    	return SUCCESS;
    }
    
    
   
}
