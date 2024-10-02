package com.tracelytics.test.action;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;
import com.tracelytics.test.dbobject.CustomDbObject;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;

import java.util.Collections;
import java.util.Random;

@Results({
    @Result(name="success", location="index.jsp"),
    @Result(name="error", location="index.jsp"),
})
public class TestCustomBson extends AbstractMongoDbAction {
	private static final DBObject INSERT_OBJECT = new CustomDbObject(Collections.singletonMap("key", "bigBatch")).append("randomInt", new Random().nextInt());

	private static final DBObject FIND_OBJECT = new CustomDbObject(Collections.singletonMap("key", "bigBatch")).append("randomInt", new BasicDBObject("$gt", 0));
	private static final DBObject UPDATE_OBJECT = new CustomDbObject(Collections.singletonMap("$inc", new CustomDbObject(Collections.singletonMap("randomInt", -1))));
	
	
    public String execute() throws Exception {
    	getCollection().insert(INSERT_OBJECT);
    	WriteResult result = getCollection().updateMulti(FIND_OBJECT, UPDATE_OBJECT);
    	
    	addActionMessage("Insert/Update executed successfully with custom Bson object, " + result.getN() + " document(s) updated");
    	return SUCCESS;
    }
}
