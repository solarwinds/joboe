package com.tracelytics.test.action;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.DeleteResult;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.bson.Document;

@Results({
    @Result(name="success", location="index.jsp"),
    @Result(name="error", location="index.jsp"),
})
public class ClearBigBatch extends AbstractMongoDbSyncAction {
    private static final String KEY_VALUE = "bigBatch";
    
    public String execute() throws Exception {
    	MongoCollection<Document> collection = getCollection();

		DeleteResult deleteResult = collection.deleteMany(new BasicDBObject("key", KEY_VALUE));

		addActionMessage("Removed " + deleteResult.getDeletedCount() + " document(s)");
    	    	
    	return SUCCESS;
    }
}
