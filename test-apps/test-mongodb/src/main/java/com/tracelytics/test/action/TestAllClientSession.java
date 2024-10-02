package com.tracelytics.test.action;

import com.mongodb.MongoNamespace;
import com.mongodb.ReadPreference;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;


@SuppressWarnings("serial")
@Results({
    @Result(name="success", location="index.jsp"),
    @Result(name="error", location="index.jsp"),
    @Result(name="input", location="index.jsp")
})
public class TestAllClientSession extends AbstractMongoDbSyncAction {
    private Random random = new Random();
    
    private static String KEY_VALUE = "testKey";
    
    public String execute() throws Exception {
    	MongoCollection<Document> collection = getCollection();
    	MongoDatabase db = getDatabase();
    	ReadPreference readPreference = db.getReadPreference();
		ClientSession clientSession = getClientSession();

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

		iterateResult(collection.aggregate(clientSession, Collections.singletonList(group)));
		iterateResult(collection.aggregate(clientSession, Collections.singletonList(group), resultClass));

    	//bulk_write
    	List<WriteModel<Document>> bulkWriteRequests = new ArrayList<WriteModel<Document>>();
		for (int i = 0 ; i < 10; i ++) {
			WriteModel<Document> writeRequest = new InsertOneModel<Document>(new Document("key", KEY_VALUE).append("randomInt", random.nextInt()));
			bulkWriteRequests.add(writeRequest);
		}
		collection.bulkWrite(clientSession, bulkWriteRequests);
		bulkWriteRequests = new ArrayList<WriteModel<Document>>();
		for (int i = 0 ; i < 10; i ++) {
			WriteModel<Document> writeRequest = new InsertOneModel<Document>(new Document("key", KEY_VALUE).append("randomInt", random.nextInt()));
			bulkWriteRequests.add(writeRequest);
		}
		collection.bulkWrite(clientSession, bulkWriteRequests, new BulkWriteOptions());

		//count
    	collection.countDocuments(clientSession);
		collection.countDocuments(clientSession, basicDocument);
		collection.countDocuments(clientSession, basicDocument, new CountOptions());

    	//create_index
    	Document indexKeys = new Document("key", 1);
    	collection.createIndex(clientSession, indexKeys);
		collection.createIndex(clientSession, indexKeys, new IndexOptions());
    	
    	//create_indexes
    	collection.createIndexes(clientSession, Collections.singletonList(new IndexModel(indexKeys)));
		collection.createIndexes(clientSession, Collections.singletonList(new IndexModel(indexKeys)), new CreateIndexOptions());

		//delete
    	collection.deleteMany(clientSession, new Document());
		collection.deleteMany(clientSession, new Document(), new DeleteOptions());
    	collection.deleteOne(clientSession, new Document("randomInt", random.nextInt()));
		collection.deleteOne(clientSession, new Document("randomInt", random.nextInt()), new DeleteOptions());

    	//distinct
		iterateResult(collection.distinct(clientSession, "key", resultClass));
		iterateResult(collection.distinct(clientSession, "key", resultClass).filter(basicDocument));
    	
    	//drop_collection
    	db.createCollection(clientSession,"dummyCollection");
		MongoCollection<Document> dummyCollection = db.getCollection("dummyCollection", resultClass);
		dummyCollection.drop(clientSession);

    	
    	//drop_index
		collection.createIndex(indexKeys);
    	collection.dropIndex(clientSession, indexKeys);
		collection.createIndex(indexKeys);
		collection.dropIndex(clientSession, indexKeys, new DropIndexOptions());
    	String generatedIndexKeys = collection.createIndex(indexKeys);
    	collection.dropIndex(clientSession, generatedIndexKeys);
		generatedIndexKeys = collection.createIndex(indexKeys);
		collection.dropIndex(generatedIndexKeys, new DropIndexOptions());
		generatedIndexKeys = collection.createIndex(indexKeys);
		collection.dropIndex(clientSession, generatedIndexKeys, new DropIndexOptions());
    	
    	//drop_indexes
    	collection.dropIndexes(clientSession);
		collection.dropIndexes(clientSession, new DropIndexOptions());
    	
    	//find
		iterateResult(collection.find(clientSession));
		iterateResult(collection.find(clientSession, basicDocument));
		iterateResult(collection.find(clientSession, resultClass));
		iterateResult(collection.find(clientSession, basicDocument, resultClass));
    	
    	
    	//find_one_and_delete
		collection.findOneAndDelete(clientSession, basicDocument);
		collection.findOneAndDelete(clientSession, basicDocument, new FindOneAndDeleteOptions());
    	
    	//find_one_and_replace
		collection.findOneAndReplace(clientSession, basicDocument, basicDocument);
		collection.findOneAndReplace(clientSession, basicDocument, basicDocument, new FindOneAndReplaceOptions());
    	
    	//find_one_and_update
		collection.findOneAndUpdate(clientSession, basicDocument, new Document("$set", new Document("randomInt", random.nextInt())));
		collection.findOneAndUpdate(clientSession, basicDocument, Collections.singletonList(new Document("$set", new Document("randomInt", random.nextInt()))));
		collection.findOneAndUpdate(clientSession, basicDocument, new Document("$set", new Document("randomInt", random.nextInt())), new FindOneAndUpdateOptions());
		collection.findOneAndUpdate(clientSession, basicDocument, Collections.singletonList(new Document("$set", new Document("randomInt", random.nextInt()))), new FindOneAndUpdateOptions());

		//insert_many
    	List<Document> documents = new ArrayList<Document>();
		for (int i = 0 ; i < 10; i ++) {
			documents.add(new Document("key", KEY_VALUE).append("randomInt", random.nextInt()));
		}
		collection.insertMany(clientSession, documents);
		documents = new ArrayList<Document>();
		for (int i = 0 ; i < 10; i ++) {
			documents.add(new Document("key", KEY_VALUE).append("randomInt", random.nextInt()));
		}
		collection.insertMany(clientSession, documents, new InsertManyOptions());

    	//insert_one
    	collection.insertOne(clientSession, basicDocument);
		collection.findOneAndDelete(clientSession, basicDocument); //have to delete otherwise next statement fails with duplicate key error
		collection.insertOne(clientSession, basicDocument, new InsertOneOptions());

    	//list_indexes
    	iterateResult(collection.listIndexes(clientSession));
		iterateResult(collection.listIndexes(clientSession, resultClass));

    	//map_reduce
    	String map = "function() { emit(this.key, this.randomInt); }";
    	String reduce2 =  "function(key, randomInts) { return Array.sum(randomInts); }";
    	iterateResult(collection.mapReduce(clientSession, map, reduce2));
		iterateResult(collection.mapReduce(clientSession, map, reduce2, resultClass));
    	
    	//rename
    	collection.renameCollection(clientSession, new MongoNamespace(TEST_DB, "newName"));
		getDatabase().getCollection("newName").renameCollection(clientSession, new MongoNamespace(TEST_DB, TEST_COLLECTION), new RenameCollectionOptions());
    	
    	//replace_one
    	collection.replaceOne(clientSession, basicDocument, basicDocument);
		collection.replaceOne(clientSession, basicDocument, basicDocument, new ReplaceOptions());

    	//update_many
    	collection.updateMany(clientSession, basicDocument, new Document("$set", new Document("randomInt", random.nextInt())));
		collection.updateMany(clientSession, basicDocument, new Document("$set", new Document("randomInt", random.nextInt())), new UpdateOptions());
		collection.updateMany(clientSession, basicDocument, Collections.singletonList(new Document("$set", new Document("randomInt", random.nextInt()))));
		collection.updateMany(clientSession, basicDocument, Collections.singletonList(new Document("$set", new Document("randomInt", random.nextInt()))), new UpdateOptions());

    	//update_one
    	collection.updateOne(clientSession, basicDocument, new Document("$set", new Document("randomInt", random.nextInt())));
		collection.updateOne(clientSession, basicDocument, new Document("$set", new Document("randomInt", random.nextInt())), new UpdateOptions());
		collection.updateOne(clientSession, basicDocument, Collections.singletonList(new Document("$set", new Document("randomInt", random.nextInt()))));
		collection.updateOne(clientSession, basicDocument, Collections.singletonList(new Document("$set", new Document("randomInt", random.nextInt()))), new UpdateOptions());

		//watch operations will hang this thread since there's no events
//		iterateResult(collection.watch(clientSession));
//		iterateResult(collection.watch(clientSession, resultClass));
//		iterateResult(collection.watch(clientSession, Arrays.asList(Aggregates.match(Filters.in("operationType", Arrays.asList("insert", "update", "replace", "delete"))))));
//		iterateResult(collection.watch(clientSession, Arrays.asList(Aggregates.match(Filters.in("operationType", Arrays.asList("insert", "update", "replace", "delete")))), resultClass));

    	//Admin (MongoDatabase)
    	//create_collection
    	MongoDatabase dummyDb = mongoClient.getDatabase("dummy_database");
    	dummyDb.createCollection(clientSession, "dummy_collection");
    	dummyDb.getCollection("dummy_collection").drop();
    	dummyDb.createCollection(clientSession, "dummy_collection", new CreateCollectionOptions());
    	dummyDb.getCollection("dummy_collection").drop();


    	//drop
    	dummyDb.drop(clientSession);

    	//command
    	Bson command = new Document("ping", 1);
    	db.runCommand(clientSession, command);
    	db.runCommand(clientSession, command, resultClass);
    	db.runCommand(clientSession, command, readPreference);
    	db.runCommand(clientSession, command, readPreference, resultClass);

    	//MongoClient
    	//drop
    	mongoClient.getDatabase("dummy_db").drop(clientSession);
    	
    	//list_database
    	iterateResult(mongoClient.listDatabaseNames(clientSession));
    	iterateResult(mongoClient.listDatabases(clientSession));
    	iterateResult(mongoClient.listDatabases(clientSession, Document.class));
    	
    	addActionMessage("Tested all of the support operations");
    	
    	return SUCCESS;
    }
    
    
   
}
