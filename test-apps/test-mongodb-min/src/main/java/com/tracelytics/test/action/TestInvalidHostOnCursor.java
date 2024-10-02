package com.tracelytics.test.action;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;

import com.mongodb.Mongo;
   
@SuppressWarnings("serial")
@Results({
    @Result(name="success", location="index.jsp"),
    @Result(name="error", location="index.jsp"),
    @Result(name="input", location="index.jsp")
})

public class TestInvalidHostOnCursor extends AbstractMongoDbAction {
    private static final String INVALID_HOST = "localhost:28000";
    
    @Override
    public String execute() throws Exception {
        iterateCursor(new Mongo(INVALID_HOST).getDB(TEST_DB).getCollection(TEST_COLLECTION).find());
        return SUCCESS;
    }
}
