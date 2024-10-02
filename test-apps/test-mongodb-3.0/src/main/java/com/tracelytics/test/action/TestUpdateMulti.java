package com.tracelytics.test.action;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;
import com.mongodb.util.JSON;
   
@Results({
    @Result(name="success", location="index.jsp"),
    @Result(name="error", location="index.jsp"),
})
public class TestUpdateMulti extends AbstractMongoDbSyncAction {
	private static final DBObject FIND_OBJECT = new BasicDBObject("key", "bigBatch").append("randomInt", new BasicDBObject("$gt", 0));
	private static final DBObject UPDATE_OBJECT = new BasicDBObject("$inc", new BasicDBObject("randomInt", -1));
	
	
    public String execute() throws Exception {
        DBCollection collection = getLegacyCollection();
    	WriteResult result = collection.updateMulti(FIND_OBJECT, UPDATE_OBJECT);
    	
    	addActionMessage("Updated (multi) executed successfully, " + result.getN() + " document(s) updated");
    	return SUCCESS;
    }
}
