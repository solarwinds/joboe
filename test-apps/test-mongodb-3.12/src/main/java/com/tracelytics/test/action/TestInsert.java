package com.tracelytics.test.action;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
   
@Results({
    @Result(name="success", location="index.jsp"),
    @Result(name="error", location="index.jsp"),
})
public class TestInsert extends AbstractMongoDbSyncAction {
	private static final String INSERT_JSON = "{ key : \"bigBatch\", randomInt : 123 }";
	
	
    public String execute() throws Exception {
    	DBObject insertObject = (DBObject) JSON.parse(INSERT_JSON);
    	
    	getLegacyCollection().insert(insertObject);
    	
    	addActionMessage("Object inserted successfully");
    	
    	appendExtendedOutput(insertObject.toString());
    	return SUCCESS;
    }
}
