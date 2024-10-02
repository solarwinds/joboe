package com.tracelytics.test.action;

import com.mongodb.ClientSessionOptions;
import com.mongodb.client.*;
import org.bson.Document;


public abstract class AbstractMongoDbSyncAction extends AbstractMongoDbAction {
//    private static final String CONNECTION_STRING = "mongodb://localhost:27017,localhost:27018";
    private static final String CONNECTION_STRING = "mongodb://localhost";
    

    
    protected MongoClient mongoClient;
    protected AbstractMongoDbSyncAction() {
    	mongoClient = MongoClients.create(CONNECTION_STRING);
	}
    
    
    protected MongoCollection<Document> getCollection() {
        return mongoClient.getDatabase(TEST_DB).getCollection(TEST_COLLECTION);
    }
    
    protected MongoDatabase getDatabase() {
        return mongoClient.getDatabase(TEST_DB);
    }

    protected ClientSession getClientSession() {
        return mongoClient.startSession(ClientSessionOptions.builder().causallyConsistent(false).build());
    }


    protected static int iterateResult(MongoIterable<?> iterable) {
        int count = 0;
        for (Object obj : iterable) {
            count ++;
        }
        return count;
    }
}
