package com.tracelytics.test.action;

import java.util.concurrent.CountDownLatch;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.bson.Document;

import com.mongodb.async.SingleResultCallback;
import com.mongodb.async.client.MongoCollection;

@Results({
    @Result(name="success", location="index.jsp"),
    @Result(name="error", location="index.jsp"),
})

public class TestAsyncInsert extends AbstractMongoDbAsyncAction {
    private Document testDoc = new Document("testKey", "testValue");

    public String execute() throws Exception {
        MongoCollection<Document> collection = getCollection();
        
        final CountDownLatch latch = new CountDownLatch(1);
            
        collection.insertOne(testDoc, new SingleResultCallback<Void>() {
            @Override
            public void onResult(Void result, Throwable t) {
               System.out.println("Inserted!");
               latch.countDown();
            }
        });
        
        latch.await();
        
        addActionMessage("Tested async insert");
        
        return SUCCESS;
    }
}
