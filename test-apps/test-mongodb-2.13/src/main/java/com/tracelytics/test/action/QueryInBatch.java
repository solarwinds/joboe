package com.tracelytics.test.action;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;


@Results({
    @Result(name="success", location="index.jsp"),
    @Result(name="error", location="index.jsp"),
    @Result(name="input", location="index.jsp")
})
public class QueryInBatch extends AbstractMongoDbAction {
	private static final int DEFAULT_SIZE = 1000;
    private int batchSize = DEFAULT_SIZE;
    private static final String KEY_VALUE = "bigBatch";
    
      
    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int value) {
        this.batchSize = value;
    }
    
    public void setBatchSize(String value) {
    	//incorrect format?
    	this.batchSize = DEFAULT_SIZE;
    }
    
    public void setBatchSize(String[] value) {
    	//incorrect format?
    	this.batchSize = DEFAULT_SIZE;
    }
    
    
    
    @Override
    public void validate() {
    	if (batchSize < DEFAULT_SIZE) {
    		addFieldError("batchSize", "Must be at least " + DEFAULT_SIZE);
    		batchSize = DEFAULT_SIZE;
    	}
    	
    	super.validate();
    }
    
    public String execute() throws Exception {
    	DBCollection collection = getCollection();
    	
    	DBCursor cursor = collection.find(new BasicDBObject("key", KEY_VALUE));
    	
    	cursor.batchSize(batchSize);
    	
    	while (cursor.hasNext()) {
    		cursor.next();
    	}
    	
    	
    	addActionMessage("Read " + cursor.numSeen() + "objects with " + cursor.numGetMores() + " batch(es)");
    	
    	return SUCCESS;
    }
    
    
    
}
