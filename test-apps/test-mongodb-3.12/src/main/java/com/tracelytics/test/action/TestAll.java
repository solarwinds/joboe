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
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
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


@SuppressWarnings("serial")
@Results({
    @Result(name="success", location="index.jsp"),
    @Result(name="error", location="index.jsp"),
    @Result(name="input", location="index.jsp")
})
public class TestAll extends AbstractMongoDbSyncAction {
    private Random random = new Random();
    
    private static String KEY_VALUE = "testKey";
    
    public String execute() throws Exception {
    	MongoCollection<Document> collection = getCollection();
    	MongoDatabase db = getDatabase();
    	ReadPreference readPreference = db.getReadPreference();
    	
    	Document basicDocument = new Document("key", KEY_VALUE);
    	Class<Document> resultClass = collection.getDocumentClass();
    	
    	logger.info(String.format("Checking existing document(s) of key [%s]", KEY_VALUE));
    	iterateResult(collection.find(basicDocument));
    	
    	//insert
    	logger.info(String.format("Inserting entry of key [%s]", KEY_VALUE));
    	collection.insertOne(basicDocument.append("randomInt", random.nextInt()));
    	
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
    	collection.bulkWrite(bulkWriteRequests);
    	
    	bulkWriteRequests = new ArrayList<WriteModel<Document>>();
        for (int i = 0 ; i < 10; i ++) {
            WriteModel<Document> writeRequest = new InsertOneModel<Document>(new Document("key", KEY_VALUE).append("randomInt", random.nextInt()));
            bulkWriteRequests.add(writeRequest);
        }
    	collection.bulkWrite(bulkWriteRequests, new BulkWriteOptions());
    	
    	//count
    	collection.count();
    	collection.count(basicDocument);
    	collection.count(basicDocument, new CountOptions());
    	
    	//create_index
    	Document indexKeys = new Document("key", 1);
    	collection.createIndex(indexKeys);
    	collection.createIndex(indexKeys, new IndexOptions());
    	
    	//create_indexes
    	collection.createIndexes(Collections.singletonList(new IndexModel(indexKeys)));
    	
    	//delete
    	collection.deleteMany(new Document());
    	collection.deleteOne(new Document());
    	collection.deleteOne(new Document("randomInt", random.nextInt()));
    	
    	//distinct
    	iterateResult(collection.distinct("key", resultClass));
    	iterateResult(collection.distinct("key", resultClass).filter(basicDocument));
    	
    	//drop_collection
    	db.createCollection("dummyCollection");
    	MongoCollection<Document> dummyCollection = db.getCollection("dummyCollection"); 
    	dummyCollection.drop();
    	
    	//drop_index
    	collection.dropIndex(indexKeys);
    	String generatedIndexKeys = collection.createIndex(indexKeys);
    	collection.dropIndex(generatedIndexKeys);
    	
    	//drop_indexes
    	collection.dropIndexes();
    	
    	//find
    	iterateResult(collection.find());
    	iterateResult(collection.find(basicDocument));
    	iterateResult(collection.find(resultClass));
    	iterateResult(collection.find(basicDocument, resultClass));
    	
    	
    	//find_one_and_delete
    	collection.findOneAndDelete(basicDocument);
    	collection.findOneAndDelete(basicDocument, new FindOneAndDeleteOptions());
    	
    	//find_one_and_replace
    	collection.findOneAndReplace(basicDocument, basicDocument);
    	collection.findOneAndReplace(basicDocument, basicDocument, new FindOneAndReplaceOptions());
    	
    	//find_one_and_update
    	collection.findOneAndUpdate(basicDocument, new Document("$set", new Document("randomInt", random.nextInt())));
    	collection.findOneAndUpdate(basicDocument, new Document("$set", new Document("randomInt", random.nextInt())), new FindOneAndUpdateOptions());
    	
    	//insert_many
    	List<Document> documents = new ArrayList<Document>();
        for (int i = 0 ; i < 10; i ++) {
            documents.add(new Document("key", KEY_VALUE).append("randomInt", random.nextInt()));
        }
        collection.insertMany(documents);
        documents = new ArrayList<Document>();
        for (int i = 0 ; i < 10; i ++) {
            documents.add(new Document("key", KEY_VALUE).append("randomInt", random.nextInt()));
        }
    	collection.insertMany(documents, new InsertManyOptions());
    	
    	//insert_one
    	collection.insertOne(basicDocument);
    	
    	//list_indexes
    	iterateResult(collection.listIndexes());
    	iterateResult(collection.listIndexes(resultClass));
    	
    	//map_reduce
    	String map = "function() { emit(this.key, this.randomInt); }";
    	String reduce2 =  "function(key, randomInts) { return Array.sum(randomInts); }";
    	iterateResult(collection.mapReduce(map, reduce2));
    	iterateResult(collection.mapReduce(map, reduce2, resultClass));
    	
    	//rename
    	collection.renameCollection(new MongoNamespace(TEST_DB, "newName"));
    	getDatabase().getCollection("newName").renameCollection(new MongoNamespace(TEST_DB, TEST_COLLECTION), new RenameCollectionOptions());
    	
    	//replace_one
    	collection.replaceOne(basicDocument, basicDocument);
    	collection.replaceOne(basicDocument, basicDocument, new UpdateOptions());
    	
    	//update_many
    	collection.updateMany(basicDocument, new Document("$set", new Document("randomInt", random.nextInt())));
    	collection.updateMany(basicDocument, new Document("$set", new Document("randomInt", random.nextInt())), new UpdateOptions());
    	
    	//update_one
    	collection.updateOne(basicDocument, new Document("$set", new Document("randomInt", random.nextInt())));
    	collection.updateOne(basicDocument, new Document("$set", new Document("randomInt", random.nextInt())), new UpdateOptions());
    	
    	//Admin (MongoDatabase)
    	//create_collection
    	MongoDatabase dummyDb = mongoClient.getDatabase("dummy_database");
    	dummyDb.createCollection("dummy_collection");
    	dummyDb.getCollection("dummy_collection").drop();
    	dummyDb.createCollection("dummy_collection", new CreateCollectionOptions());
    	dummyDb.getCollection("dummy_collection").drop();
    	
    	//drop
    	dummyDb.drop();
    	
    	//command
    	Bson command = new Document("ping", 1);
    	db.runCommand(command);
    	db.runCommand(command, resultClass);
    	db.runCommand(command, readPreference);
    	db.runCommand(command, readPreference, resultClass);
    	   	
    	//MongoClient
    	//get_database
    	mongoClient.getDatabase("dummy_db");
    	//drop
    	mongoClient.dropDatabase("dummy_db");
    	
    	//list_database
    	iterateResult(mongoClient.listDatabaseNames());
    	iterateResult(mongoClient.listDatabases());
    	iterateResult(mongoClient.listDatabases(Document.class));
    	
    	addActionMessage("Tested all of the support operations");
    	
    	return SUCCESS;
    }
    
    
   
}
