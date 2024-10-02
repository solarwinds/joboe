package com.tracelytics.test.action;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;

import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MapReduceCommand;
import com.mongodb.MapReduceCommand.OutputType;
import com.mongodb.MapReduceOutput;
import com.mongodb.QueryBuilder;
   
@Results({
    @Result(name="success", location="index.jsp"),
    @Result(name="error", location="index.jsp"),
})
public class TestMapReduce extends AbstractMongoDbAction {
	public String execute() throws Exception {
    	DBCollection collection = getCollection();
    	DBObject condition = QueryBuilder.start("randomInt").greaterThan(0).get();
    	
    	//inline_map_reduce
    	String map = "function() { emit(this.key, this.randomInt); }";
    	String reduce2 =  "function(key, randomInts) { return Array.sum(randomInts); }";
    	MapReduceCommand mapReduceCommand = new MapReduceCommand(collection, map, reduce2, null, OutputType.INLINE, condition);
    	MapReduceOutput output = collection.mapReduce(mapReduceCommand);
    	logger.info("Map reduced output (inline):");
    	logger.info(output.toString());
    	
    	//inline_map_reduce using plain DBObject
    	collection.mapReduce(mapReduceCommand.toDBObject());
    	logger.info("Map reduced output (inline dbObject):");
    	logger.info(output.toString());
    	
    	//map_reduce
    	mapReduceCommand = new MapReduceCommand(collection, map, reduce2, "mapReduceResult", OutputType.MERGE, condition);
    	output = collection.mapReduce(mapReduceCommand);
    	logger.info("Map reduced output (merge):");
    	logger.info(output.toString());
    	
    	//map_reduce using plain DBObject
    	collection.mapReduce(mapReduceCommand.toDBObject());
    	logger.info("Map reduced output (dbObject):");
    	logger.info(output.toString());
    	
    	addActionMessage("Executed map_reduce successfully");
    	
    	for (DBObject entry : output.results()) {
    		appendExtendedOutput(entry.toString());
    	}
    	
    	return SUCCESS;
    }
}
