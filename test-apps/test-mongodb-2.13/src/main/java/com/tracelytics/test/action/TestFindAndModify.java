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
public class TestFindAndModify extends AbstractMongoDbAction {
	private static final DBObject FIND_OBJECT = new BasicDBObject("key", "bigBatch");
	private static final DBObject SORT_OBJECT = new BasicDBObject("randomInt", 1);
	private static final DBObject UPDATE_OBJECT = new BasicDBObject("randomInt", 0);
	
    public String execute() throws Exception {
    	getCollection().findAndModify(FIND_OBJECT, SORT_OBJECT, UPDATE_OBJECT);
    	
    	addActionMessage("find_and_modify executed successfully");
    	return SUCCESS;
    }
}
