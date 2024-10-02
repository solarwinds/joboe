package com.tracelytics.test.action;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.bson.BSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
   
@Results({
    @Result(name="success", location="index.jsp"),
    @Result(name="error", location="index.jsp"),
})
public class InsertBigBatch extends AbstractMongoDbSyncAction {
    private Random random = new Random();
    private static final int BATCH_SIZE = 1000;
    private static final String KEY_VALUE = "bigBatch";
    
    public String execute() throws Exception {
    	DBCollection collection = getLegacyCollection();
    	
    	List<DBObject> insertObjects = new ArrayList<DBObject>();
    	
    	for (int i = 0 ; i < BATCH_SIZE; i++) {
    		insertObjects.add(new BasicDBObject("key", "bigBatch").append("randomInt", random.nextInt()));
    	}
    	
    	collection.insert(insertObjects);
    	
    	long count = collection.count(new BasicDBObject("key", KEY_VALUE));
    	
    	addActionMessage("Inserted " + BATCH_SIZE + " document(s). There are now " + count + " document(s) with key [" + KEY_VALUE + "]");
    	    	
    	return SUCCESS;
    }
}
