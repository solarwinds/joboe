package com.tracelytics.test.action;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.bson.Document;

import com.mongodb.MongoClient;
@SuppressWarnings("serial")
@Results({
    @Result(name="success", location="index.jsp"),
    @Result(name="error", location="index.jsp"),
    @Result(name="input", location="index.jsp")
})

public class TestInvalidHost extends AbstractMongoDbSyncAction {
    private static final String INVALID_HOST = "localhost:28000";
    
    @Override
    public String execute() throws Exception {
        iterateResult(new MongoClient(INVALID_HOST).getDatabase(TEST_DB).getCollection(TEST_COLLECTION).find());
        return SUCCESS;
    }
}
