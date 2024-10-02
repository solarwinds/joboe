package com.tracelytics.test.action;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.MongoCollection;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Results({
    @Result(name="success", location="index.jsp"),
    @Result(name="error", location="index.jsp"),
})
public class InsertBigBatch extends AbstractMongoDbSyncAction {
    private Random random = new Random();
    private static final int BATCH_SIZE = 1000;
    private static final String KEY_VALUE = "bigBatch";
    
    public String execute() throws Exception {
		MongoCollection<Document> collection = getCollection();
    	
    	List<DBObject> insertObjects = new ArrayList<DBObject>();

		List<Document> documents = new ArrayList<Document>();
		for (int i = 0 ; i < BATCH_SIZE; i ++) {
			documents.add(new Document("key", "bigBatch").append("randomInt", random.nextInt()));
		}

    	collection.insertMany(documents);
    	
    	long count = collection.countDocuments(new BasicDBObject("key", KEY_VALUE));
    	
    	addActionMessage("Inserted " + BATCH_SIZE + " document(s). There are now " + count + " document(s) with key [" + KEY_VALUE + "]");
    	    	
    	return SUCCESS;
    }
}
