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
public class TestFindStress extends AbstractMongoDbStressAction {
	private static final String FIND_QUERY = "{ key : \"bigBatch\" }";
	private static final DBObject QUERY_OBJECT = (DBObject) JSON.parse(FIND_QUERY);
	
    public void unitExecute(int currentRun) throws Exception {
    	DBCursor cursor = getCollection().find(QUERY_OBJECT);
    	iterateCursor(cursor);
    }


    @Override
    protected String getOperation() {
        return "find";
    }

}
