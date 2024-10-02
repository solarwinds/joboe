package com.tracelytics.test.action;

import java.util.Arrays;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;

import com.mongodb.AggregationOptions;
import com.mongodb.BasicDBObject;
import com.mongodb.Cursor;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.ReadPreference;
   
@Results({
    @Result(name="success", location="index.jsp"),
    @Result(name="error", location="index.jsp"),
})
public class TestAggregate extends AbstractMongoDbSyncAction {
	public String execute() throws Exception {
    	DBCollection collection = getLegacyCollection();
    	
    	DBObject match = new BasicDBObject("$match", new BasicDBObject("randomInt", new BasicDBObject("$lte", 0)));
    	//aggregate with only 1 op (old signature)
    	collection.aggregate(match);
    			
    	
    	//aggregate with multiple ops (old signature)
    	DBObject groupFields = new BasicDBObject( "_id", "$key");
    	groupFields.put("average", new BasicDBObject( "$avg", "$randomInt"));
    	DBObject group = new BasicDBObject("$group", groupFields);
    	
    	collection.aggregate(match, group);
    	
    	//aggregate with multiple ops (new signature)
    	collection.aggregate(Arrays.asList(match, group));
    	
    	//aggregate with multiple ops, options and readPreferences (new signature)
    	Cursor output = collection.aggregate(Arrays.asList(match, group), AggregationOptions.builder().batchSize(100).build(), ReadPreference.primary()); 
    	
    	addActionMessage("Executed aggregate successfully");
    	
    	while (output.hasNext()) {
    		appendExtendedOutput(output.next().toString());
    	}
    	
    	return SUCCESS;
    }
}
