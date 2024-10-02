package com.tracelytics.test.action;

import java.util.List;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;

import com.mongodb.BasicDBObject;
   
@Results({
    @Result(name="success", location="index.jsp"),
    @Result(name="error", location="index.jsp"),
})
public class TestDistinct extends AbstractMongoDbSyncAction {
	private static final String KEY = "key";
	
    public String execute() throws Exception {
    	List<Object> keys = getLegacyCollection().distinct(KEY);
    	
    	getLegacyCollection().distinct(KEY, getLegacyDb().getReadPreference());
    	getLegacyCollection().distinct(KEY, new BasicDBObject("randomInt", new BasicDBObject("$gte", 0)));
    	getLegacyCollection().distinct(KEY, new BasicDBObject("randomInt", new BasicDBObject("$gte", 0)), getLegacyDb().getReadPreference());
    	
    	addActionMessage("Found " + keys.size() + " distinct values for key [" + KEY + "]");
    	    	
    	return SUCCESS;
    }
}
