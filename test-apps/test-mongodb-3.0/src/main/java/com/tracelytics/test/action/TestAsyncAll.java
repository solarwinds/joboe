package com.tracelytics.test.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.MongoNamespace;
import com.mongodb.ReadPreference;
import com.mongodb.async.SingleResultCallback;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.async.client.MongoDatabase;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.CountOptions;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.FindOneAndDeleteOptions;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexModel;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.InsertManyOptions;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.RenameCollectionOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;


@SuppressWarnings("serial")
@Results({
    @Result(name="success", location="index.jsp"),
    @Result(name="error", location="index.jsp"),
    @Result(name="input", location="index.jsp")
})
public class TestAsyncAll extends AbstractMongoDbAsyncAction {
    private Random random = new Random();
    
    private static String KEY_VALUE = "testKey";
    
    public String execute() throws Exception {
    	MongoCollection<Document> collection = getDatabase().getCollection(TEST_COLLECTION);
    	MongoDatabase db = getDatabase();
    	ReadPreference readPreference = db.getReadPreference();
    	
    	Document basicDocument = new Document("key", KEY_VALUE);
    	Class<Document> resultClass = collection.getDocumentClass();
    	
    	logger.info(String.format("Checking existing document(s) of key [%s]", KEY_VALUE));
    	iterateResult(collection.find(basicDocument));
    	
    	//insert
    	logger.info(String.format("Inserting entry of key [%s]", KEY_VALUE));
    	collection.insertOne(basicDocument.append("randomInt", random.nextInt()), this.<Void>getCallback());

    	logger.info("Checking all existing document(s)");
    	iterateResult(collection.find());
    	
    	logger.info(String.format("Checking existing document(s) of key [%s]", KEY_VALUE));
    	iterateResult(collection.find(basicDocument));

    	//aggregate
    	Document groupFields = new Document( "_id", "$key");
    	groupFields.put("average", new Document( "$avg", "randomInt"));
    	Document group = new Document("$group", groupFields);

    	iterateResult(collection.aggregate(Collections.singletonList(group)));
    	iterateResult(collection.aggregate(Collections.singletonList(group), resultClass));
    	
    	//bulk_write
    	List<WriteModel<Document>> bulkWriteRequests = new ArrayList<WriteModel<Document>>();
    	for (int i = 0 ; i < 10; i ++) {
    	    WriteModel<Document> writeRequest = new InsertOneModel<Document>(new Document("key", KEY_VALUE).append("randomInt", random.nextInt()));
    	    bulkWriteRequests.add(writeRequest);
    	}
    	collection.bulkWrite(bulkWriteRequests, this.<BulkWriteResult>getCallback());
    	
    	bulkWriteRequests = new ArrayList<WriteModel<Document>>();
        for (int i = 0 ; i < 10; i ++) {
            WriteModel<Document> writeRequest = new InsertOneModel<Document>(new Document("key", KEY_VALUE).append("randomInt", random.nextInt()));
            bulkWriteRequests.add(writeRequest);
        }
    	collection.bulkWrite(bulkWriteRequests, new BulkWriteOptions(), this.<BulkWriteResult>getCallback());
    	
    	//count
    	collection.count(this.<Long>getCallback());
    	collection.count(basicDocument, this.<Long>getCallback());
    	collection.count(basicDocument, new CountOptions(), this.<Long>getCallback());
    	
    	//create_index
    	Document indexKeys = new Document("key", 1);
    	collection.createIndex(indexKeys, this.<String>getCallback());
    	collection.createIndex(indexKeys, new IndexOptions(), this.<String>getCallback());
    	
    	//create_indexes
    	collection.createIndexes(Collections.singletonList(new IndexModel(indexKeys)), this.<List<String>>getCallback());
    	
    	//delete
    	collection.deleteMany(new Document(), this.<DeleteResult>getCallback());
    	collection.deleteOne(new Document(), this.<DeleteResult>getCallback());
    	
    	//distinct
    	iterateResult(collection.distinct("key", resultClass));
    	iterateResult(collection.distinct("key", resultClass).filter(basicDocument));
    	
    	//drop_collection
    	db.createCollection("dummyCollection", this.<Void>getCallback());
    	MongoCollection<Document> dummyCollection = db.getCollection("dummyCollection"); 
    	dummyCollection.drop(this.<Void>getCallback());
    	
    	//drop_index
    	collection.dropIndex(indexKeys, this.<Void>getCallback());
  	
        final StringBuffer generatedIndexKeys = new StringBuffer();
        
        synchronized(generatedIndexKeys) {
            collection.createIndex(indexKeys, new SingleResultCallback<String>() {
                @Override
                public void onResult(String result, Throwable t) {
                    synchronized(generatedIndexKeys) {
                        generatedIndexKeys.append(result);
                        generatedIndexKeys.notifyAll();
                    }
                }
            });

            generatedIndexKeys.wait();
        }
    	
    	    	   	 
    	collection.dropIndex(generatedIndexKeys.toString(), this.<Void>getCallback());
    	
    	//drop_indexes
    	collection.dropIndexes(this.<Void>getCallback());
    	
    	//find
    	iterateResult(collection.find());
    	iterateResult(collection.find(basicDocument));
    	iterateResult(collection.find(resultClass));
    	iterateResult(collection.find(basicDocument, resultClass));
    	
    	
    	//find_one_and_delete
    	collection.findOneAndDelete(basicDocument, this.<Document>getCallback());
    	collection.findOneAndDelete(basicDocument, new FindOneAndDeleteOptions(), this.<Document>getCallback());
    	
    	//find_one_and_replace
    	collection.findOneAndReplace(basicDocument, basicDocument, this.<Document>getCallback());
    	collection.findOneAndReplace(basicDocument, basicDocument, new FindOneAndReplaceOptions(), this.<Document>getCallback());
    	
    	//find_one_and_update
    	collection.findOneAndUpdate(basicDocument, new Document("$set", new Document("randomInt", random.nextInt())), this.<Document>getCallback());
    	collection.findOneAndUpdate(basicDocument, new Document("$set", new Document("randomInt", random.nextInt())), new FindOneAndUpdateOptions(), this.<Document>getCallback());
    	
    	//insert_many
    	List<Document> documents = new ArrayList<Document>();
        for (int i = 0 ; i < 10; i ++) {
            documents.add(new Document("key", KEY_VALUE).append("randomInt", random.nextInt()));
        }
        collection.insertMany(documents, this.<Void>getCallback());
        documents = new ArrayList<Document>();
        for (int i = 0 ; i < 10; i ++) {
            documents.add(new Document("key", KEY_VALUE).append("randomInt", random.nextInt()));
        }
    	collection.insertMany(documents, new InsertManyOptions(), this.<Void>getCallback());
    	
    	//insert_one
    	collection.insertOne(basicDocument, this.<Void>getCallback());
    	
    	//list_indexes
    	iterateResult(collection.listIndexes());
    	iterateResult(collection.listIndexes(resultClass));
    	
    	//map_reduce
    	String map = "function() { emit(this.key, this.randomInt); }";
    	String reduce2 =  "function(key, randomInts) { return Array.sum(randomInts); }";
    	iterateResult(collection.mapReduce(map, reduce2));
    	iterateResult(collection.mapReduce(map, reduce2, resultClass));
    	
    	//rename
    	collection.renameCollection(new MongoNamespace(TEST_DB, "newName"), this.<Void>getCallback());
    	getDatabase().getCollection("newName").renameCollection(new MongoNamespace(TEST_DB, TEST_COLLECTION), new RenameCollectionOptions(), this.<Void>getCallback());
    	
    	//replace_one
    	collection.replaceOne(basicDocument, basicDocument, this.<UpdateResult>getCallback());
    	collection.replaceOne(basicDocument, basicDocument, new UpdateOptions(), this.<UpdateResult>getCallback());
    	
    	//update_many
    	collection.updateMany(basicDocument, new Document("$set", new Document("randomInt", random.nextInt())), this.<UpdateResult>getCallback());
    	collection.updateMany(basicDocument, new Document("$set", new Document("randomInt", random.nextInt())), new UpdateOptions(), this.<UpdateResult>getCallback());
    	
    	//update_one
    	collection.updateOne(basicDocument, new Document("$set", new Document("randomInt", random.nextInt())), this.<UpdateResult>getCallback());
    	collection.updateOne(basicDocument, new Document("$set", new Document("randomInt", random.nextInt())), new UpdateOptions(), this.<UpdateResult>getCallback());
    	
    	//Admin (MongoDatabase)
    	//create_collection
    	MongoDatabase dummyDb = mongoClient.getDatabase("dummy_database");
    	dummyDb.createCollection("dummy_collection", this.<Void>getCallback());
    	dummyDb.getCollection("dummy_collection").drop(this.<Void>getCallback());
    	dummyDb.createCollection("dummy_collection", new CreateCollectionOptions(), this.<Void>getCallback());
    	dummyDb.getCollection("dummy_collection").drop(this.<Void>getCallback());
    	
    	//drop
    	dummyDb.drop(this.<Void>getCallback());
    	
    	//command
    	Bson command = new Document("ping", 1);
    	db.runCommand(command, this.<Document>getCallback());
    	db.runCommand(command, resultClass, this.<Document>getCallback());
    	db.runCommand(command, readPreference, this.<Document>getCallback());
    	db.runCommand(command, readPreference, resultClass, this.<Document>getCallback());
    	   	
    	//MongoClient
    	
    	//list_database
    	iterateResult(mongoClient.listDatabaseNames());
    	iterateResult(mongoClient.listDatabases());
    	iterateResult(mongoClient.listDatabases(Document.class));
    	
    	
    	if (waitUntilAllFinishes()) {
    	    addActionMessage("Tested all of the support operations");
    	}
    	
    	return SUCCESS;
    }
    
    
}
   
