package com.tracelytics.test.action;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;

import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MapReduceCommand;
import com.mongodb.MapReduceCommand.OutputType;
import com.mongodb.MapReduceOutput;
import com.mongodb.QueryBuilder;
   
@Results({
    @Result(name="success", location="index.jsp"),
    @Result(name="error", location="index.jsp"),
})
public class TestRename extends AbstractMongoDbSyncAction {
	private static String NEW_NAME = "newName";

	public String execute() throws Exception {
    	DBCollection collection = getLegacyCollection();
    	
    	DBCollection newCollection = collection.rename(NEW_NAME);
    	newCollection.rename(collection.getName());
    	
    	addActionMessage("Set collection to [" + NEW_NAME + "] and then renamed it back to [" + collection.getName() + "]");
    	
    	return SUCCESS;
    }
}
