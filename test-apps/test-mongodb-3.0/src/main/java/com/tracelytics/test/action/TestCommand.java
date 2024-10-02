package com.tracelytics.test.action;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;

import com.mongodb.BasicDBObject;
import com.mongodb.CommandResult;
import com.mongodb.DBEncoder;
import com.mongodb.DBObject;
import com.mongodb.ReadPreference;
   
@Results({
    @Result(name="success", location="index.jsp"),
    @Result(name="error", location="index.jsp"),
})
public class TestCommand extends AbstractMongoDbSyncAction {
    private static final String COMMAND = "ping";
	
    public String execute() throws Exception {
    	CommandResult result;
    	DBObject commandObject = new BasicDBObject(COMMAND, Boolean.TRUE);
    	ReadPreference readPreference = getLegacyDb().getReadPreference();
    	
    	getLegacyDb().command(COMMAND);
    	getLegacyDb().command(commandObject);
    	getLegacyDb().command(commandObject, (DBEncoder)null);
    	getLegacyDb().command(commandObject, readPreference);
    	getLegacyDb().command(COMMAND, readPreference);
    	result = getLegacyDb().command(commandObject, readPreference, null);
    	
    	addActionMessage("Called " + COMMAND + " and the result is :" + result.ok());
    	    	
    	return SUCCESS;
    }
}

