package com.tracelytics.test.action;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.bson.Document;

import java.util.Random;

@Results({
    @Result(name="success", location="index.jsp"),
    @Result(name="error", location="index.jsp"),
})
public class TestInsertStress extends AbstractMongoDbStressAction {
	private static final Random random = new Random(); 

    @Override
    protected void unitExecute(int currentRun) throws Exception {
        getCollection().insertOne(new Document("key", "bigBatch").append("randomInt", random.nextInt()));
    }


    @Override
    protected String getOperation() {
        return "insert";
    }
}
