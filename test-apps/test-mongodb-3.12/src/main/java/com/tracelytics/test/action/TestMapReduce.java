package com.tracelytics.test.action;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;

import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MapReduceCommand;
import com.mongodb.ReadPreference;
import com.mongodb.MapReduceCommand.OutputType;
import com.mongodb.MapReduceOutput;
import com.mongodb.QueryBuilder;
   
@Results({
    @Result(name="success", location="index.jsp"),
    @Result(name="error", location="index.jsp"),
})
public class TestMapReduce extends AbstractMongoDbSyncAction {
	public String execute() throws Exception {
    	DBCollection collection = getLegacyCollection();
    	DBObject condition = QueryBuilder.start("randomInt").greaterThan(0).get();
    	
    	//inline_map_reduce
    	String map = "function() { emit(this.key, this.randomInt); }";
    	String reduce =  "function(key, randomInts) { return Array.sum(randomInts); }";
    	MapReduceCommand mapReduceCommand = new MapReduceCommand(collection, map, reduce, null, OutputType.INLINE, condition);
    	MapReduceOutput output = collection.mapReduce(mapReduceCommand);
    	logger.info("Map reduced output (inline):");
    	logger.info(output.toString());
    	
    	//map_reduce
    	mapReduceCommand = new MapReduceCommand(collection, map, reduce, "mapReduceResult", OutputType.MERGE, condition);
    	output = collection.mapReduce(mapReduceCommand);
    	logger.info("Map reduced output (merge):");
    	logger.info(output.toString());
    	
    	output = collection.mapReduce(map, reduce, "mapReduceResult", condition);
    	logger.info("Map reduced output (merge):");
        logger.info(output.toString());
    	
        output = collection.mapReduce(map, reduce, "mapReduceResult", OutputType.MERGE, condition);
        logger.info("Map reduced output (merge):");
        logger.info(output.toString());
        
        output = collection.mapReduce(map, reduce, null, OutputType.INLINE, condition, ReadPreference.primaryPreferred());
    	logger.info("Map reduced output (inline):");
        logger.info(output.toString());
    	
    	addActionMessage("Executed map_reduce successfully");
    	
    	for (DBObject entry : output.results()) {
    		appendExtendedOutput(entry.toString());
    	}
    	
    	return SUCCESS;
    }
}
