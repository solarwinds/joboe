package com.tracelytics.test.action;

import java.util.Random;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.GroupCommand;
import com.mongodb.MapReduceCommand;
import com.mongodb.MapReduceCommand.OutputType;
import com.mongodb.MapReduceOutput;
import com.mongodb.QueryBuilder;


@Results({
    @Result(name="success", location="index.jsp"),
    @Result(name="error", location="index.jsp"),
    @Result(name="input", location="index.jsp")
})
public class TestAll extends AbstractMongoDbAction {
    private Random random = new Random();
    
    private static String KEY_VALUE= "testKey";
    
    public String execute() throws Exception {
    	DBCollection collection = getCollection();
    	DB db = getDb();
    	
    	DBObject indexKeys = new BasicDBObject("key", 1);
    	BasicDBObject basicDBObject = new BasicDBObject("key", KEY_VALUE);
    	
    	
    	logger.info(String.format("Checking existing document(s) of key [%s]", KEY_VALUE));
    	iterateCursor(collection.find(basicDBObject));
    	
    	//insert
    	logger.info(String.format("Inserting entry of key [%s]", KEY_VALUE));
    	collection.insert(basicDBObject.append("randomInt", random.nextInt()));
    	
    	logger.info("Checking all existing document(s)");
    	iterateCursor(collection.find());
    	
    	logger.info(String.format("Checking existing document(s) of key [%s]", KEY_VALUE));
    	iterateCursor(collection.find(basicDBObject));
    	
    	//command
    	BasicDBObject cmd = new BasicDBObject();
        cmd.put("count", collection.getName());
        logger.info(db.command(cmd));
    	
    	//count
    	collection.count();
    	
    	//create_collection
    	DBCollection dummyCollection = db.createCollection("dummyCollection", null);
    	dummyCollection.insert(basicDBObject.append("randomInt", random.nextInt()));
    	
    	//create_index
    	collection.createIndex(indexKeys);
    	
    	//distinct
    	collection.distinct("key");
    	collection.distinct("key", basicDBObject);
    	
    	//drop
    	DB dummyDb = mongoClient.getDB("dummyDb");
    	dummyDb.dropDatabase();
    	
    	//drop_collection
    	dummyCollection.drop();
    	
    	//drop_index
    	collection.dropIndex(indexKeys);
    	
    	//drop_indexes
    	collection.dropIndexes();
    	
    	//ensure_index
    	collection.ensureIndex("key");

    	//find
    	collection.find().toArray();
    	
    	//find_and_modify
    	collection.findAndModify(basicDBObject, ((BasicDBObject)basicDBObject.clone()).append("extraField", true));
    	
    	//find_one
    	collection.findOne();
    	    	
    	//group
    	DBObject condition = QueryBuilder.start("randomInt").greaterThan(0).get();
    	DBObject initial = new BasicDBObject("total", 0).append("count", 0);
    	String reduce = "function(curr, result) { result.total += curr.randomInt; result.count++; } ";
    	String finalize = "function(result) { result.average = Math.round(result.total / result.count); }";
    	GroupCommand groupCommand = new GroupCommand(collection, basicDBObject, condition, initial, reduce, finalize);
    	
    	collection.group(groupCommand);
    	
    	//index_information
    	collection.getIndexInfo();
    	
    	//stats
    	collection.getStats();
    	
    	//inline_map_reduce
    	//map_reduce
    	String map = "function() { emit(this.key, this.randomInt); }";
    	String reduce2 =  "function(key, randomInts) { return Array.sum(randomInts); }";
    	
    	MapReduceCommand mapReduceCommand = new MapReduceCommand(collection, map, reduce2, null, OutputType.INLINE, condition);
    	MapReduceOutput output = collection.mapReduce(mapReduceCommand);
    	logger.info("Map reduced output:");
    	logger.info(output.toString());
    	

    	//options. No longer instrumented see https://github.com/librato/joboe/commit/9f8a3da7f496fd0d574803dd7204123d543340ea
    	//collection.addOption(1);
    	//collection.setOptions(collection.getOptions());
    	//collection.resetOptions();
    	
    	//remove
    	collection.remove(basicDBObject);
    	
    	//rename
    	collection.rename("newName");
    	
    	getDb().getCollection("newName").rename(TEST_COLLECTION);
    	
    	//save
    	collection.save(((BasicDBObject)basicDBObject.clone()).append("randomInt", random.nextInt()));
    	
    	//update
    	collection.update(basicDBObject, ((BasicDBObject)basicDBObject.clone()).append("randomInt", random.nextInt()));
    	
    	addActionMessage("Tested all of the support operations");
    	
    	return SUCCESS;
    }
    
    
   
}
