package com.tracelytics.test.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import com.mongodb.MongoNamespace;
import com.mongodb.ReadPreference;
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
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;

/**
 * Test case using the reactive stream driver
 * @author pluk
 *
 */
@SuppressWarnings("serial")
@Results({
    @Result(name="success", location="index.jsp"),
    @Result(name="error", location="index.jsp"),
    @Result(name="input", location="index.jsp")
})
public class TestReactiveAll extends AbstractMongoDbReactiveAction {
    private Random random = new Random();
    
    private static String KEY_VALUE = "testKey";
    
    public String execute() throws Exception {
    	MongoCollection<Document> collection = getDatabase().getCollection(TEST_COLLECTION);
    	MongoDatabase db = getDatabase();
    	ReadPreference readPreference = db.getReadPreference();
    	
    	Document basicDocument = new Document("key", KEY_VALUE);
    	Class<Document> resultClass = collection.getDocumentClass();
    	
    	logger.info(String.format("Checking existing document(s) of key [%s]", KEY_VALUE));
    	collection.find(basicDocument).subscribe(getSubscriber());
    	
    	//insert
    	logger.info(String.format("Inserting entry of key [%s]", KEY_VALUE));
    	collection.insertOne(basicDocument.append("randomInt", random.nextInt())).subscribe(getSubscriber());

    	logger.info("Checking all existing document(s)");
    	collection.find().subscribe(getSubscriber());
    	
    	logger.info(String.format("Checking existing document(s) of key [%s]", KEY_VALUE));
    	collection.find(basicDocument).subscribe(getSubscriber());

    	//aggregate
    	Document groupFields = new Document( "_id", "$key");
    	groupFields.put("average", new Document( "$avg", "randomInt"));
    	Document group = new Document("$group", groupFields);

    	collection.aggregate(Collections.singletonList(group)).subscribe(getSubscriber());
    	collection.aggregate(Collections.singletonList(group), resultClass).subscribe(getSubscriber());
    	
    	//bulk_write
    	List<WriteModel<Document>> bulkWriteRequests = new ArrayList<WriteModel<Document>>();
    	for (int i = 0 ; i < 10; i ++) {
    	    WriteModel<Document> writeRequest = new InsertOneModel<Document>(new Document("key", KEY_VALUE).append("randomInt", random.nextInt()));
    	    bulkWriteRequests.add(writeRequest);
    	}
    	collection.bulkWrite(bulkWriteRequests).subscribe(getSubscriber());
    	
    	bulkWriteRequests = new ArrayList<WriteModel<Document>>();
        for (int i = 0 ; i < 10; i ++) {
            WriteModel<Document> writeRequest = new InsertOneModel<Document>(new Document("key", KEY_VALUE).append("randomInt", random.nextInt()));
            bulkWriteRequests.add(writeRequest);
        }
    	collection.bulkWrite(bulkWriteRequests, new BulkWriteOptions()).subscribe(getSubscriber());
    	
    	//count
    	collection.count().subscribe(getSubscriber());
    	collection.count(basicDocument).subscribe(getSubscriber());
    	collection.count(basicDocument, new CountOptions()).subscribe(getSubscriber());
    	
    	//create_index
    	Document indexKeys = new Document("key", 1);
    	collection.createIndex(indexKeys).subscribe(getSubscriber());
    	collection.createIndex(indexKeys, new IndexOptions()).subscribe(getSubscriber());
    	
    	//create_indexes
    	collection.createIndexes(Collections.singletonList(new IndexModel(indexKeys))).subscribe(getSubscriber());
    	
    	//delete
    	collection.deleteMany(new Document()).subscribe(getSubscriber());
    	collection.deleteOne(new Document()).subscribe(getSubscriber());
    	
    	//distinct
    	collection.distinct("key", resultClass).subscribe(getSubscriber());
    	collection.distinct("key", resultClass).filter(basicDocument).subscribe(getSubscriber());
    	
    	//drop_collection
    	db.createCollection("dummyCollection").subscribe(getSubscriber());
    	MongoCollection<Document> dummyCollection = db.getCollection("dummyCollection"); 
    	dummyCollection.drop().subscribe(getSubscriber());
    	
    	//drop_index
    	collection.dropIndex(indexKeys).subscribe(getSubscriber());
  	
        final StringBuffer generatedIndexKeys = new StringBuffer();
        
        synchronized(generatedIndexKeys) {
            collection.createIndex(indexKeys).subscribe(new Subscriber<String>() {
                @Override
                public void onSubscribe(Subscription s) {
                    s.request(1);
                }
    
                @Override
                public void onNext(String t) {
                    generatedIndexKeys.append(t);
                }
    
                @Override
                public void onError(Throwable t) {
                    onComplete();
                }
    
                @Override
                public void onComplete() {
                    synchronized(generatedIndexKeys) {
                        generatedIndexKeys.notifyAll();
                    }
                }
            });
            
            generatedIndexKeys.wait();
        }
            
    	    	   	 
    	collection.dropIndex(generatedIndexKeys.toString()).subscribe(getSubscriber());
    	
    	//drop_indexes
    	collection.dropIndexes().subscribe(getSubscriber());
    	
    	//find
    	collection.find().subscribe(getSubscriber());
    	collection.find(basicDocument).subscribe(getSubscriber());
    	collection.find(resultClass).subscribe(getSubscriber());
    	collection.find(basicDocument, resultClass).subscribe(getSubscriber());
    	
    	
    	//find_one_and_delete
    	collection.findOneAndDelete(basicDocument).subscribe(getSubscriber());
    	collection.findOneAndDelete(basicDocument, new FindOneAndDeleteOptions()).subscribe(getSubscriber());
    	
    	//find_one_and_replace
    	collection.findOneAndReplace(basicDocument, basicDocument).subscribe(getSubscriber());
    	collection.findOneAndReplace(basicDocument, basicDocument, new FindOneAndReplaceOptions()).subscribe(getSubscriber());
    	
    	//find_one_and_update
    	collection.findOneAndUpdate(basicDocument, new Document("$set", new Document("randomInt", random.nextInt()))).subscribe(getSubscriber());
    	collection.findOneAndUpdate(basicDocument, new Document("$set", new Document("randomInt", random.nextInt())), new FindOneAndUpdateOptions()).subscribe(getSubscriber());
    	
    	//insert_many
    	List<Document> documents = new ArrayList<Document>();
        for (int i = 0 ; i < 10; i ++) {
            documents.add(new Document("key", KEY_VALUE).append("randomInt", random.nextInt()));
        }
        collection.insertMany(documents).subscribe(getSubscriber());
        documents = new ArrayList<Document>();
        for (int i = 0 ; i < 10; i ++) {
            documents.add(new Document("key", KEY_VALUE).append("randomInt", random.nextInt()));
        }
    	collection.insertMany(documents, new InsertManyOptions()).subscribe(getSubscriber());
    	
    	//insert_one
    	collection.insertOne(basicDocument).subscribe(getSubscriber());
    	
    	//list_indexes
    	collection.listIndexes().subscribe(getSubscriber());
    	collection.listIndexes(resultClass).subscribe(getSubscriber());
    	
    	//map_reduce
    	String map = "function() { emit(this.key, this.randomInt); }";
    	String reduce2 =  "function(key, randomInts) { return Array.sum(randomInts); }";
    	collection.mapReduce(map, reduce2).subscribe(getSubscriber());
    	collection.mapReduce(map, reduce2, resultClass).subscribe(getSubscriber());
    	
    	//rename
    	collection.renameCollection(new MongoNamespace(TEST_DB, "newName")).subscribe(getSubscriber());
    	getDatabase().getCollection("newName").renameCollection(new MongoNamespace(TEST_DB, TEST_COLLECTION), new RenameCollectionOptions()).subscribe(getSubscriber());
    	
    	//replace_one
    	collection.replaceOne(basicDocument, basicDocument).subscribe(getSubscriber());
    	collection.replaceOne(basicDocument, basicDocument, new UpdateOptions()).subscribe(getSubscriber());
    	
    	//update_many
    	collection.updateMany(basicDocument, new Document("$set", new Document("randomInt", random.nextInt()))).subscribe(getSubscriber());
    	collection.updateMany(basicDocument, new Document("$set", new Document("randomInt", random.nextInt())), new UpdateOptions()).subscribe(getSubscriber());
    	
    	//update_one
    	collection.updateOne(basicDocument, new Document("$set", new Document("randomInt", random.nextInt()))).subscribe(getSubscriber());
    	collection.updateOne(basicDocument, new Document("$set", new Document("randomInt", random.nextInt())), new UpdateOptions()).subscribe(getSubscriber());
    	
    	//Admin (MongoDatabase)
    	//create_collection
    	MongoDatabase dummyDb = mongoClient.getDatabase("dummy_database");
    	dummyDb.createCollection("dummy_collection").subscribe(getSubscriber());
    	dummyDb.getCollection("dummy_collection").drop().subscribe(getSubscriber());
    	dummyDb.createCollection("dummy_collection", new CreateCollectionOptions()).subscribe(getSubscriber());
    	dummyDb.getCollection("dummy_collection").drop().subscribe(getSubscriber());
    	
    	//drop
    	dummyDb.drop().subscribe(getSubscriber());
    	
    	//command
    	Bson command = new Document("ping", 1);
    	db.runCommand(command).subscribe(getSubscriber());
    	db.runCommand(command, resultClass).subscribe(getSubscriber());
    	db.runCommand(command, readPreference).subscribe(getSubscriber());
    	db.runCommand(command, readPreference, resultClass).subscribe(getSubscriber());
    	   	
    	//MongoClient
    	
    	//list_database
    	mongoClient.listDatabaseNames().subscribe(getSubscriber());
    	mongoClient.listDatabases().subscribe(getSubscriber());
    	mongoClient.listDatabases(Document.class).subscribe(getSubscriber());
    	
    	
    	if (waitUntilAllFinishes()) {
    	    addActionMessage("Tested reactive streams operations");
    	}
    	
    	return SUCCESS;
    }
    
    
}
   
