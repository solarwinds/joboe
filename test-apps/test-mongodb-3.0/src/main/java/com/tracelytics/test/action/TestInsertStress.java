package com.tracelytics.test.action;

import java.util.Random;

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
public class TestInsertStress extends AbstractMongoDbStressAction {
	private static final Random random = new Random(); 

    @Override
    protected void unitExecute(int currentRun) throws Exception {
        getLegacyCollection().insert(new BasicDBObject("key", "bigBatch").append("randomInt", random.nextInt()));    
    }


    @Override
    protected String getOperation() {
        return "insert";
    }
}
