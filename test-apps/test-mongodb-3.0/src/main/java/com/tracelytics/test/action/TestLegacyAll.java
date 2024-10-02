package com.tracelytics.test.action;

import java.util.Arrays;
import java.util.Random;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.GroupCommand;
import com.mongodb.MapReduceCommand;
import com.mongodb.MongoClient;
import com.mongodb.ReadPreference;
import com.mongodb.MapReduceCommand.OutputType;
import com.mongodb.MapReduceOutput;
import com.mongodb.ParallelScanOptions;
import com.mongodb.QueryBuilder;


@Results({
    @Result(name="success", location="index.jsp"),
    @Result(name="error", location="index.jsp"),
    @Result(name="input", location="index.jsp")
})
public class TestLegacyAll extends AbstractMongoDbSyncAction {
    private Random random = new Random();
    
    private static String KEY_VALUE = "testKey";
    
    public String execute() throws Exception {
    	DBCollection collection = getLegacyCollection();
    	DB db = getLegacyDb();
    	ReadPreference readPreference = db.getReadPreference();
    	
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

    	//aggregate
    	DBObject groupFields = new BasicDBObject( "_id", "$key");
    	groupFields.put("average", new BasicDBObject( "$avg", "randomInt"));
    	DBObject group = new BasicDBObject("$group", groupFields);
    	//older method signature
    	collection.aggregate(new BasicDBObject("$match", new BasicDBObject("randomInt", new BasicDBObject("$lte", 0))),
    			             group);
    	//newer method signature
    	collection.aggregate(Arrays.asList(new BasicDBObject("$match", new BasicDBObject("randomInt", new BasicDBObject("$lte", 0))), group));
    	
    	//command
    	BasicDBObject cmd = new BasicDBObject();
        cmd.put("count", collection.getName());
    	logger.info(db.command(cmd));
    	
    	//count
    	collection.count();
    	collection.count(basicDBObject);
    	collection.count(basicDBObject, readPreference);
    	
    	//create_collection
    	DBCollection dummyCollection = db.createCollection("dummyCollection", new BasicDBObject());
    	dummyCollection.insert(basicDBObject.append("randomInt", random.nextInt()));
    	
    	//create_index
    	DBObject indexKeys = new BasicDBObject("key", 1);
    	collection.createIndex(indexKeys);
    	collection.createIndex("key");
    	collection.createIndex(indexKeys, new BasicDBObject());
    	collection.createIndex(indexKeys, (String)null);
    	collection.createIndex(indexKeys, (String)null, false);
    	
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
    	
    	//find
    	collection.find().toArray();
    	    	
    	//find_and_modify
    	collection.findAndModify(basicDBObject, ((BasicDBObject)basicDBObject.clone()).append("extraField", true));
    	
    	//find_one
    	collection.findOne();
    	collection.findOne(basicDBObject);
    	collection.findOne(0);
    	collection.findOne(basicDBObject, null);
    	collection.findOne(0, null);
    	collection.findOne(basicDBObject, null, new BasicDBObject("randomInt", -1));
    	collection.findOne(basicDBObject, null, readPreference);
    	collection.findOne(basicDBObject, null, new BasicDBObject("randomInt", -1), readPreference);
    	    	
    	//group
    	DBObject condition = QueryBuilder.start("randomInt").greaterThan(0).get();
    	DBObject initial = new BasicDBObject("total", 0).append("count", 0);
    	String reduce = "function(curr, result) { result.total += curr.randomInt; result.count++; } ";
    	String finalize = "function(result) { result.average = Math.round(result.total / result.count); }";
    	GroupCommand groupCommand = new GroupCommand(collection, basicDBObject, condition, initial, reduce, finalize);
    	
    	collection.group(groupCommand);
    	collection.group(groupCommand, readPreference);
    	collection.group(basicDBObject, condition, initial, reduce);
    	collection.group(basicDBObject, condition, initial, reduce, finalize);
    	collection.group(basicDBObject, condition, initial, reduce, finalize, readPreference);
    	
    	//index_information
    	collection.getIndexInfo();
    	
    	//stats
    	collection.getStats();
    	
    	//inline_map_reduce
    	String map = "function() { emit(this.key, this.randomInt); }";
    	String reduce2 =  "function(key, randomInts) { return Array.sum(randomInts); }";
    	MapReduceCommand mapReduceCommand = new MapReduceCommand(collection, map, reduce2, null, OutputType.INLINE, condition);
    	collection.mapReduce(mapReduceCommand);
    	    	
    	//options. No longer instrumented. See https://github.com/librato/joboe/commit/9f8a3da7f496fd0d574803dd7204123d543340ea
    	//collection.addOption(1);
    	//collection.setOptions(collection.getOptions());
    	//collection.resetOptions();
    	
    	//parallel scan
    	collection.parallelScan(ParallelScanOptions.builder().numCursors(2).build());
    	
    	//remove
    	collection.remove(basicDBObject);
    	collection.remove(basicDBObject, collection.getWriteConcern());
    	collection.remove(basicDBObject, collection.getWriteConcern(), null);
    	
    	//rename
    	collection.rename("newName");
    	getLegacyDb().getCollection("newName").rename(TEST_COLLECTION, false);
    	
    	//save
    	collection.save(((BasicDBObject)basicDBObject.clone()).append("randomInt", random.nextInt()));
    	collection.save(((BasicDBObject)basicDBObject.clone()).append("randomInt", random.nextInt()), collection.getWriteConcern());
    	
    	//update
    	DBObject findObject = new BasicDBObject("key", "bigBatch").append("randomInt", new BasicDBObject("$gt", 0));
        DBObject updateObject = new BasicDBObject("$inc", new BasicDBObject("randomInt", -1));
        
        collection.update(findObject, updateObject);
        collection.update(findObject, updateObject, false, false);
        collection.update(findObject, updateObject, false, false, collection.getWriteConcern());
        collection.update(findObject, updateObject, false, false, collection.getWriteConcern(), null);
    	
    	addActionMessage("Tested all of the support operations");
    	
    	return SUCCESS;
    }
    
    
   
}
