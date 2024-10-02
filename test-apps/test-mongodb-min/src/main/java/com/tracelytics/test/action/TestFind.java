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
public class TestFind extends AbstractMongoDbAction {
	private static final String FIND_QUERY = "{ \"key\" : \"bigBatch\" }";
	
	
    public String execute() throws Exception {
DBObject queryObject = (DBObject) JSON.parse(FIND_QUERY);
    	
    	DBCursor cursor = getCollection().find(queryObject).batchSize(300);
    	
    	printCursorToExtendedOutput(cursor);
    	
    	cursor = getCollection().find(queryObject).sort(new BasicDBObject("randomInt", -1)).limit(100);
    	
    	printCursorToExtendedOutput(cursor);
    	
    	addActionMessage("find executed successfully");
    	
    	return SUCCESS;
    }
}
