package com.tracelytics.test.action;

import java.util.concurrent.TimeUnit;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
   
@Results({
    @Result(name="success", location="index.jsp"),
    @Result(name="error", location="index.jsp"),
})
public class TestFindAndModify extends AbstractMongoDbSyncAction {
	private static final DBObject FIND_OBJECT = new BasicDBObject("key", "bigBatch");
	private static final DBObject SORT_OBJECT = new BasicDBObject("randomInt", 1);
	private static final DBObject UPDATE_OBJECT = new BasicDBObject("randomInt", 0);
	
    public String execute() throws Exception {
        getLegacyCollection().findAndModify(FIND_OBJECT, UPDATE_OBJECT);
    	getLegacyCollection().findAndModify(FIND_OBJECT, SORT_OBJECT, UPDATE_OBJECT);
    	getLegacyCollection().findAndModify(FIND_OBJECT, null, SORT_OBJECT, false, UPDATE_OBJECT, false, false);
    	getLegacyCollection().findAndModify(FIND_OBJECT, null, SORT_OBJECT, false, UPDATE_OBJECT, false, false, 0L, TimeUnit.MILLISECONDS);
    	
    	addActionMessage("find_and_modify executed successfully");
    	return SUCCESS;
    }
}
