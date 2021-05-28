package com.tracelytics.test.action;

import java.util.*;

import com.mongodb.client.model.*;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import com.mongodb.MongoNamespace;
import com.mongodb.ReadPreference;
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
		blockingExecute(collection.bulkWrite(bulkWriteRequests));

		bulkWriteRequests = new ArrayList<WriteModel<Document>>();
		for (int i = 0 ; i < 10; i ++) {
			WriteModel<Document> writeRequest = new InsertOneModel<Document>(new Document("key", KEY_VALUE).append("randomInt", random.nextInt()));
			bulkWriteRequests.add(writeRequest);
		}
		blockingExecute(collection.bulkWrite(bulkWriteRequests, new BulkWriteOptions()));

		//count
		collection.countDocuments().subscribe(getSubscriber());
		collection.countDocuments(basicDocument).subscribe(getSubscriber());
		collection.countDocuments(basicDocument, new CountOptions()).subscribe(getSubscriber());

		//create_index
		Document indexKeys = new Document("key", 1);
		collection.createIndex(indexKeys).subscribe(getSubscriber());
		collection.createIndex(indexKeys, new IndexOptions()).subscribe(getSubscriber());

		//create_indexes
		collection.createIndexes(Collections.singletonList(new IndexModel(indexKeys))).subscribe(getSubscriber());
		collection.createIndexes(Collections.singletonList(new IndexModel(indexKeys)), new CreateIndexOptions()).subscribe(getSubscriber());

		//delete
		blockingExecute(collection.deleteMany(new Document()));
		blockingExecute(collection.deleteMany(new Document(), new DeleteOptions()));
		blockingExecute(collection.deleteOne(new Document()));
		blockingExecute(collection.deleteOne(new Document(), new DeleteOptions()));

		//distinct
		collection.distinct("key", String.class).subscribe(getSubscriber());
		collection.distinct("key", basicDocument, String.class).subscribe(getSubscriber());

		//drop_collection
		blockingExecute(db.createCollection("dummyCollection"));
		MongoCollection<Document> dummyCollection = db.getCollection("dummyCollection");
		blockingExecute(dummyCollection.drop());

		waitUntilAllFinishes(); //a pause point before testing drop index

		//drop_index
		LineNumberSubscriber<String> createIndexSubscriber;
		LineNumberSubscriber dropIndexSubscriber;
		String generatedKeys;

		dropIndexSubscriber = getSubscriber();
		collection.dropIndex(indexKeys).subscribe(dropIndexSubscriber);
		dropIndexSubscriber.waitUntilThisFinishes();

		createIndexSubscriber = getSubscriber();
		collection.createIndex(indexKeys).subscribe(createIndexSubscriber);
		generatedKeys = createIndexSubscriber.waitUntilThisFinishes();
		dropIndexSubscriber = getSubscriber();
		collection.dropIndex(generatedKeys).subscribe(dropIndexSubscriber);
		dropIndexSubscriber.waitUntilThisFinishes();

		createIndexSubscriber = getSubscriber();
		collection.createIndex(indexKeys).subscribe(createIndexSubscriber);
		createIndexSubscriber.waitUntilThisFinishes();
		dropIndexSubscriber = getSubscriber();
		collection.dropIndex(indexKeys, new DropIndexOptions()).subscribe(dropIndexSubscriber);
		dropIndexSubscriber.waitUntilThisFinishes();

		createIndexSubscriber = getSubscriber();
		collection.createIndex(indexKeys).subscribe(createIndexSubscriber);
		generatedKeys = createIndexSubscriber.waitUntilThisFinishes();
		dropIndexSubscriber = getSubscriber();
		collection.dropIndex(generatedKeys, new DropIndexOptions()).subscribe(dropIndexSubscriber);
		dropIndexSubscriber.waitUntilThisFinishes();

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

		generatedIndexKeys.delete(0, generatedIndexKeys.length());
		synchronized(generatedIndexKeys) {
			collection.createIndex(indexKeys, new IndexOptions()).subscribe(new Subscriber<String>() {
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
		collection.dropIndexes(new DropIndexOptions()).subscribe(getSubscriber());


		//find
		collection.find().subscribe(getSubscriber());
		collection.find(basicDocument).subscribe(getSubscriber());
		collection.find(resultClass).subscribe(getSubscriber());
		collection.find(basicDocument, resultClass).subscribe(getSubscriber());


		//find_one_and_delete
		blockingExecute(collection.findOneAndDelete(basicDocument));
		blockingExecute(collection.findOneAndDelete(basicDocument, new FindOneAndDeleteOptions()));

		//find_one_and_replace
		blockingExecute(collection.findOneAndReplace(basicDocument, basicDocument));
		blockingExecute(collection.findOneAndReplace(basicDocument, basicDocument, new FindOneAndReplaceOptions()));

		//find_one_and_update
		blockingExecute(collection.findOneAndUpdate(basicDocument, new Document("$set", new Document("randomInt", random.nextInt()))));
		blockingExecute(collection.findOneAndUpdate(basicDocument, new Document("$set", new Document("randomInt", random.nextInt())), new FindOneAndUpdateOptions()));
		blockingExecute(collection.findOneAndUpdate(basicDocument, Arrays.asList(new Document("$set", new Document("randomInt", random.nextInt())), new Document("$set", new Document("randomInt", random.nextInt())))));
		blockingExecute(collection.findOneAndUpdate(basicDocument, Arrays.asList(new Document("$set", new Document("randomInt", random.nextInt())), new Document("$set", new Document("randomInt", random.nextInt()))), new FindOneAndUpdateOptions()));

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
		blockingExecute(collection.insertMany(documents, new InsertManyOptions()));

		//insert_one
		blockingExecute(collection.insertOne(basicDocument));
		blockingExecute(collection.deleteOne(basicDocument));
		blockingExecute(collection.insertOne(basicDocument, new InsertOneOptions()));

		//list_indexes
		collection.listIndexes().subscribe(getSubscriber());
		collection.listIndexes(resultClass).subscribe(getSubscriber());

		//map_reduce
		String map = "function() { emit(this.key, this.randomInt); }";
		String reduce2 =  "function(key, randomInts) { return Array.sum(randomInts); }";
		collection.mapReduce(map, reduce2).subscribe(getSubscriber());
		collection.mapReduce(map, reduce2, resultClass).subscribe(getSubscriber());

		//pause here before testing renames

		waitUntilAllFinishes();

		//rename
		blockingExecute(collection.renameCollection(new MongoNamespace(TEST_DB, "newName")));
		blockingExecute(getDatabase().getCollection("newName").renameCollection(new MongoNamespace(TEST_DB, TEST_COLLECTION), new RenameCollectionOptions()));

		//pause here after testing renames
		waitUntilAllFinishes();

		//replace_one
		blockingExecute(collection.replaceOne(basicDocument, basicDocument));
		blockingExecute(collection.replaceOne(basicDocument, basicDocument, new ReplaceOptions()));

		//update_many
		blockingExecute(collection.updateMany(basicDocument, new Document("$set", new Document("randomInt", random.nextInt()))));
		blockingExecute(collection.updateMany(basicDocument, new Document("$set", new Document("randomInt", random.nextInt())), new UpdateOptions()));
		blockingExecute(collection.updateMany(basicDocument, Arrays.asList(new Document("$set", new Document("randomInt", random.nextInt())), new Document("$set", new Document("randomInt", random.nextInt())))));
		blockingExecute(collection.updateMany(basicDocument, Arrays.asList(new Document("$set", new Document("randomInt", random.nextInt())), new Document("$set", new Document("randomInt", random.nextInt()))), new UpdateOptions()));

		//update_one
		blockingExecute(collection.updateOne(basicDocument, new Document("$set", new Document("randomInt", random.nextInt()))));
		blockingExecute(collection.updateOne(basicDocument, new Document("$set", new Document("randomInt", random.nextInt())), new UpdateOptions()));
		blockingExecute(collection.updateOne(basicDocument, Arrays.asList(new Document("$set", new Document("randomInt", random.nextInt())), new Document("$set", new Document("randomInt", random.nextInt())))));
		blockingExecute(collection.updateOne(basicDocument, Arrays.asList(new Document("$set", new Document("randomInt", random.nextInt())), new Document("$set", new Document("randomInt", random.nextInt()))), new UpdateOptions()));


		//Admin (MongoDatabase)
		//create_collection
		MongoDatabase dummyDb = mongoClient.getDatabase("dummy_database");
		blockingExecute(dummyDb.createCollection("dummy_collection"));
		blockingExecute(dummyDb.getCollection("dummy_collection").drop());
		blockingExecute(dummyDb.createCollection("dummy_collection", new CreateCollectionOptions()));
		blockingExecute(dummyDb.getCollection("dummy_collection").drop());

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
   
