package com.tracelytics.test.action;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.WriteResult;
   
@Results({
    @Result(name="success", location="index.jsp"),
    @Result(name="error", location="index.jsp"),
})
public class ClearBigBatch extends AbstractMongoDbAction {
    private static final String KEY_VALUE = "bigBatch";
    
    public String execute() throws Exception {
    	DBCollection collection = getCollection();
    	
    	WriteResult result = collection.remove(new BasicDBObject("key", KEY_VALUE));
    	
    	addActionMessage("Removed " + result.getN() + " document(s)");
    	    	
    	return SUCCESS;
    }
}
