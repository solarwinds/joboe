package com.tracelytics.test.action;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;

import com.mongodb.DBCollection;
   
@Results({
    @Result(name="success", location="index.jsp"),
    @Result(name="error", location="index.jsp"),
})
public class TestRename extends AbstractMongoDbAction {
	private static String NEW_NAME = "newName";

	public String execute() throws Exception {
    	DBCollection collection = getCollection();
    	
    	DBCollection newCollection = collection.rename(NEW_NAME);
    	newCollection.rename(collection.getName());
    	
    	addActionMessage("Set collection to [" + NEW_NAME + "] and then renamed it back to [" + collection.getName() + "]");
    	
    	return SUCCESS;
    }
}
