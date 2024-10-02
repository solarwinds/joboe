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
public class QueryInRange extends AbstractMongoDbSyncAction {
	private static final int MIN_SIZE = 1;
    private static final String KEY_VALUE = "bigBatch";
    
    private int fromIndex;
    private int toIndex;
	private int size;
      
    public int getFromIndex() {
        return fromIndex;
    }
    
    public int getToIndex() {
		return toIndex;
	}

    public void setFromIndex(String value) {
    	try {
    		this.fromIndex = Integer.valueOf(value);
    	} catch (NumberFormatException e) {
    		addFieldError("fromIndex", "Invalid From Index");
    	}
    }
    
    public void setToIndex(String value) {
    	try {
    		this.toIndex = Integer.valueOf(value);
    	} catch (NumberFormatException e) {
    		addFieldError("toIndex", "Invalid To Index");
    	}
    }
    
    @Override
    public void validate() {
    	if (fromIndex < 0) {
    		addFieldError("fromIndex", "Invalid From Index");
    	}
    	
    	size = toIndex - fromIndex + 1;
    	if (size < MIN_SIZE) {
    		addFieldError("toIndex", "Invalid To Index");
    	}
    	
    	 
    	
    	super.validate();
    }
    
    public String execute() throws Exception {
    	DBCollection collection = getLegacyCollection();
    	
    	DBCursor cursor = collection.find(new BasicDBObject("key", KEY_VALUE));
    	
    	cursor.skip(fromIndex).limit(size);
    	
    	while (cursor.hasNext()) {
    		cursor.next();
    	}
    	
    	addActionMessage("Read " + cursor.numSeen() + " document(s)");
    	
    	return SUCCESS;
    }
    
    
    
}
