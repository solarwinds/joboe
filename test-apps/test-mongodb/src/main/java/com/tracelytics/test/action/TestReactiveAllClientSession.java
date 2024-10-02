package com.tracelytics.test.action;

import com.mongodb.MongoNamespace;
import com.mongodb.ReadPreference;
import com.mongodb.client.model.*;
import com.mongodb.reactivestreams.client.ClientSession;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.*;
import java.util.function.Function;

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
public class TestReactiveAllClientSession extends AbstractMongoDbReactiveAction {
    private Random random = new Random();
    
    private static String KEY_VALUE = "testKey";
    
    public String execute() throws Exception {
    	MongoCollection<Document> collection = getDatabase().getCollection(TEST_COLLECTION);
    	MongoDatabase db = getDatabase();
    	ReadPreference readPreference = db.getReadPreference();
		ClientSession clientSession = getClientSession();
    	
    	Document basicDocument = new Document("key", KEY_VALUE);
    	Class<Document> resultClass = collection.getDocumentClass();
    	
    	logger.info(String.format("Checking existing document(s) of key [%s]", KEY_VALUE));
    	collection.find(clientSession, basicDocument).subscribe(getSubscriber());

    	//insert
    	logger.info(String.format("Inserting entry of key [%s]", KEY_VALUE));
    	collection.insertOne(clientSession, basicDocument.append("randomInt", random.nextInt())).subscribe(getSubscriber());

    	logger.info("Checking all existing document(s)");
    	collection.find(clientSession).subscribe(getSubscriber());
    	
    	logger.info(String.format("Checking existing document(s) of key [%s]", KEY_VALUE));
    	collection.find(clientSession, basicDocument).subscribe(getSubscriber());

    	//aggregate
    	Document groupFields = new Document( "_id", "$key");
    	groupFields.put("average", new Document( "$avg", "randomInt"));
    	Document group = new Document("$group", groupFields);

    	collection.aggregate(clientSession, Collections.singletonList(group)).subscribe(getSubscriber());
    	collection.aggregate(clientSession, Collections.singletonList(group), resultClass).subscribe(getSubscriber());

    	//bulk_write
    	List<WriteModel<Document>> bulkWriteRequests = new ArrayList<WriteModel<Document>>();
    	for (int i = 0 ; i < 10; i ++) {
    	    WriteModel<Document> writeRequest = new InsertOneModel<Document>(new Document("key", KEY_VALUE).append("randomInt", random.nextInt()));
    	    bulkWriteRequests.add(writeRequest);
    	}
    	blockingExecute(collection.bulkWrite(clientSession, bulkWriteRequests));
    	
    	bulkWriteRequests = new ArrayList<WriteModel<Document>>();
        for (int i = 0 ; i < 10; i ++) {
            WriteModel<Document> writeRequest = new InsertOneModel<Document>(new Document("key", KEY_VALUE).append("randomInt", random.nextInt()));
            bulkWriteRequests.add(writeRequest);
        }
    	blockingExecute(collection.bulkWrite(clientSession, bulkWriteRequests, new BulkWriteOptions()));

    	//count
    	collection.countDocuments(clientSession).subscribe(getSubscriber());
    	collection.countDocuments(clientSession, basicDocument).subscribe(getSubscriber());
    	collection.countDocuments(clientSession, basicDocument, new CountOptions()).subscribe(getSubscriber());

    	//create_index
    	Document indexKeys = new Document("key", 1);
    	collection.createIndex(clientSession, indexKeys).subscribe(getSubscriber());
    	collection.createIndex(clientSession, indexKeys, new IndexOptions()).subscribe(getSubscriber());
    	
    	//create_indexes
    	collection.createIndexes(clientSession, Collections.singletonList(new IndexModel(indexKeys))).subscribe(getSubscriber());
    	collection.createIndexes(clientSession, Collections.singletonList(new IndexModel(indexKeys)), new CreateIndexOptions()).subscribe(getSubscriber());
    	
    	//delete
    	blockingExecute(collection.deleteMany(clientSession, new Document()));
		blockingExecute(collection.deleteMany(clientSession, new Document(), new DeleteOptions()));
		blockingExecute(collection.deleteOne(clientSession, new Document()));
		blockingExecute(collection.deleteOne(clientSession, new Document(), new DeleteOptions()));

    	//distinct
    	collection.distinct(clientSession, "key", String.class).subscribe(getSubscriber());
    	collection.distinct(clientSession, "key", basicDocument, String.class).subscribe(getSubscriber());

    	//drop_collection
		blockingExecute(db.createCollection(clientSession, "dummyCollection"));
    	MongoCollection<Document> dummyCollection = db.getCollection("dummyCollection");
		blockingExecute(dummyCollection.drop(clientSession));

    	waitUntilAllFinishes(); //a pause point before testing drop index

    	//drop_index
		LineNumberSubscriber<String> createIndexSubscriber;
		LineNumberSubscriber dropIndexSubscriber;
		String generatedKeys;

		dropIndexSubscriber = getSubscriber();
    	collection.dropIndex(clientSession, indexKeys).subscribe(dropIndexSubscriber);
		dropIndexSubscriber.waitUntilThisFinishes();

		createIndexSubscriber = getSubscriber();
		collection.createIndex(clientSession, indexKeys).subscribe(createIndexSubscriber);
		generatedKeys = createIndexSubscriber.waitUntilThisFinishes();
		dropIndexSubscriber = getSubscriber();
    	collection.dropIndex(clientSession, generatedKeys).subscribe(dropIndexSubscriber);
		dropIndexSubscriber.waitUntilThisFinishes();

		createIndexSubscriber = getSubscriber();
		collection.createIndex(clientSession, indexKeys).subscribe(createIndexSubscriber);
		createIndexSubscriber.waitUntilThisFinishes();
		dropIndexSubscriber = getSubscriber();
		collection.dropIndex(clientSession, indexKeys, new DropIndexOptions()).subscribe(dropIndexSubscriber);
		dropIndexSubscriber.waitUntilThisFinishes();

		createIndexSubscriber = getSubscriber();
		collection.createIndex(clientSession, indexKeys).subscribe(createIndexSubscriber);
		generatedKeys = createIndexSubscriber.waitUntilThisFinishes();
		dropIndexSubscriber = getSubscriber();
		collection.dropIndex(clientSession, generatedKeys, new DropIndexOptions()).subscribe(dropIndexSubscriber);
		dropIndexSubscriber.waitUntilThisFinishes();

        final StringBuffer generatedIndexKeys = new StringBuffer();

        synchronized(generatedIndexKeys) {
            collection.createIndex(clientSession, indexKeys).subscribe(new Subscriber<String>() {
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

		collection.dropIndex(clientSession, generatedIndexKeys.toString()).subscribe(getSubscriber());

        generatedIndexKeys.delete(0, generatedIndexKeys.length());
		synchronized(generatedIndexKeys) {
			collection.createIndex(clientSession, indexKeys, new IndexOptions()).subscribe(new Subscriber<String>() {
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

		collection.dropIndex(clientSession, generatedIndexKeys.toString()).subscribe(getSubscriber());
    	
    	//drop_indexes
    	collection.dropIndexes(clientSession).subscribe(getSubscriber());
    	collection.dropIndexes(clientSession, new DropIndexOptions()).subscribe(getSubscriber());

    	
    	//find
    	collection.find(clientSession).subscribe(getSubscriber());
    	collection.find(clientSession, basicDocument).subscribe(getSubscriber());
    	collection.find(clientSession, resultClass).subscribe(getSubscriber());
    	collection.find(clientSession, basicDocument, resultClass).subscribe(getSubscriber());

    	
    	//find_one_and_delete
    	blockingExecute(collection.findOneAndDelete(clientSession, basicDocument));
		blockingExecute(collection.findOneAndDelete(clientSession, basicDocument, new FindOneAndDeleteOptions()));
    	
    	//find_one_and_replace
		blockingExecute(collection.findOneAndReplace(clientSession, basicDocument, basicDocument));
		blockingExecute(collection.findOneAndReplace(clientSession, basicDocument, basicDocument, new FindOneAndReplaceOptions()));
    	
    	//find_one_and_update
		blockingExecute(collection.findOneAndUpdate(clientSession, basicDocument, new Document("$set", new Document("randomInt", random.nextInt()))));
		blockingExecute(collection.findOneAndUpdate(clientSession, basicDocument, new Document("$set", new Document("randomInt", random.nextInt())), new FindOneAndUpdateOptions()));
		blockingExecute(collection.findOneAndUpdate(clientSession, basicDocument, Arrays.asList(new Document("$set", new Document("randomInt", random.nextInt())), new Document("$set", new Document("randomInt", random.nextInt())))));
		blockingExecute(collection.findOneAndUpdate(clientSession, basicDocument, Arrays.asList(new Document("$set", new Document("randomInt", random.nextInt())), new Document("$set", new Document("randomInt", random.nextInt()))), new FindOneAndUpdateOptions()));

    	//insert_many
    	List<Document> documents = new ArrayList<Document>();
        for (int i = 0 ; i < 10; i ++) {
            documents.add(new Document("key", KEY_VALUE).append("randomInt", random.nextInt()));
        }
		blockingExecute(collection.insertMany(documents));
        documents = new ArrayList<Document>();
        for (int i = 0 ; i < 10; i ++) {
            documents.add(new Document("key", KEY_VALUE).append("randomInt", random.nextInt()));
        }
		blockingExecute(collection.insertMany(clientSession, documents, new InsertManyOptions()));

    	//insert_one
    	blockingExecute(collection.insertOne(clientSession, basicDocument));
		blockingExecute(collection.deleteOne(clientSession, basicDocument));
		blockingExecute(collection.insertOne(clientSession, basicDocument, new InsertOneOptions()));
    	
    	//list_indexes
    	collection.listIndexes(clientSession).subscribe(getSubscriber());
    	collection.listIndexes(clientSession, resultClass).subscribe(getSubscriber());

    	//map_reduce
    	String map = "function() { emit(this.key, this.randomInt); }";
    	String reduce2 =  "function(key, randomInts) { return Array.sum(randomInts); }";
    	collection.mapReduce(clientSession, map, reduce2).subscribe(getSubscriber());
    	collection.mapReduce(clientSession, map, reduce2, resultClass).subscribe(getSubscriber());

    	//pause here before testing renames

		waitUntilAllFinishes();

    	//rename
    	blockingExecute(collection.renameCollection(clientSession, new MongoNamespace(TEST_DB, "newName")));
    	blockingExecute(getDatabase().getCollection("newName").renameCollection(clientSession, new MongoNamespace(TEST_DB, TEST_COLLECTION), new RenameCollectionOptions()));

    	//pause here after testing renames
		waitUntilAllFinishes();
    	
    	//replace_one
		blockingExecute(collection.replaceOne(clientSession, basicDocument, basicDocument));
		blockingExecute(collection.replaceOne(clientSession, basicDocument, basicDocument, new ReplaceOptions()));

    	//update_many
    	blockingExecute(collection.updateMany(clientSession, basicDocument, new Document("$set", new Document("randomInt", random.nextInt()))));
    	blockingExecute(collection.updateMany(clientSession, basicDocument, new Document("$set", new Document("randomInt", random.nextInt())), new UpdateOptions()));
		blockingExecute(collection.updateMany(clientSession, basicDocument, Arrays.asList(new Document("$set", new Document("randomInt", random.nextInt())), new Document("$set", new Document("randomInt", random.nextInt())))));
		blockingExecute(collection.updateMany(clientSession, basicDocument, Arrays.asList(new Document("$set", new Document("randomInt", random.nextInt())), new Document("$set", new Document("randomInt", random.nextInt()))), new UpdateOptions()));

    	//update_one
		blockingExecute(collection.updateOne(clientSession, basicDocument, new Document("$set", new Document("randomInt", random.nextInt()))));
		blockingExecute(collection.updateOne(clientSession, basicDocument, new Document("$set", new Document("randomInt", random.nextInt())), new UpdateOptions()));
		blockingExecute(collection.updateOne(clientSession, basicDocument, Arrays.asList(new Document("$set", new Document("randomInt", random.nextInt())), new Document("$set", new Document("randomInt", random.nextInt())))));
		blockingExecute(collection.updateOne(clientSession, basicDocument, Arrays.asList(new Document("$set", new Document("randomInt", random.nextInt())), new Document("$set", new Document("randomInt", random.nextInt()))), new UpdateOptions()));

    	
    	//Admin (MongoDatabase)
    	//create_collection
    	MongoDatabase dummyDb = mongoClient.getDatabase("dummy_database");
    	blockingExecute(dummyDb.createCollection(clientSession, "dummy_collection"));
		blockingExecute(dummyDb.getCollection("dummy_collection").drop(clientSession));
		blockingExecute(dummyDb.createCollection(clientSession, "dummy_collection", new CreateCollectionOptions()));
		blockingExecute(dummyDb.getCollection("dummy_collection").drop(clientSession));

    	//drop
    	dummyDb.drop(clientSession).subscribe(getSubscriber());

    	//command
    	Bson command = new Document("ping", 1);
    	db.runCommand(clientSession, command).subscribe(getSubscriber());
    	db.runCommand(clientSession, command, resultClass).subscribe(getSubscriber());
    	db.runCommand(clientSession, command, readPreference).subscribe(getSubscriber());
    	db.runCommand(clientSession, command, readPreference, resultClass).subscribe(getSubscriber());
    	   	
    	//MongoClient
    	
    	//list_database
    	mongoClient.listDatabaseNames(clientSession).subscribe(getSubscriber());
    	mongoClient.listDatabases(clientSession).subscribe(getSubscriber());
    	mongoClient.listDatabases(clientSession, Document.class).subscribe(getSubscriber());

    	
    	if (waitUntilAllFinishes()) {
    	    addActionMessage("Tested reactive streams operations");
    	}
    	
    	return SUCCESS;
    }

    private <T> T blockingExecute(Publisher<T> publisher) {
    	LineNumberSubscriber<T> subscriber = getSubscriber();
    	publisher.subscribe(subscriber);

    	return subscriber.waitUntilThisFinishes();
	}
}
   
