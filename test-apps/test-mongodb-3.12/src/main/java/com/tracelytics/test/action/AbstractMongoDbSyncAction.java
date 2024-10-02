package com.tracelytics.test.action;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.bson.Document;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.Preparable;


public abstract class AbstractMongoDbSyncAction extends AbstractMongoDbAction {
    private static final List<ServerAddress> HOSTS = new ArrayList<ServerAddress>(); 
    
    static {
//        HOSTS.add(new ServerAddress("localhost:27017")); //shard cluster query router 1
//        HOSTS.add(new ServerAddress("localhost:27018")); //shard cluster query router 2
        HOSTS.add(new ServerAddress("localhost"));
    }
    
    
    protected MongoClient mongoClient;
    protected AbstractMongoDbSyncAction() {
    	mongoClient = new MongoClient(HOSTS);
	}
    
    
    protected DBCollection getLegacyCollection() {
        return mongoClient.getDB(TEST_DB).getCollection(TEST_COLLECTION);
    }
    
    protected DB getLegacyDb() {
        return mongoClient.getDB(TEST_DB);
    }
    
    protected MongoCollection<Document> getCollection() {
        return mongoClient.getDatabase(TEST_DB).getCollection(TEST_COLLECTION);
    }
    
    protected MongoDatabase getDatabase() {
        return mongoClient.getDatabase(TEST_DB);
    }
    
    protected static void iterateCursor(DBCursor cursor) {
        while (cursor.hasNext()) {
            cursor.next();
        }
    }
    
    protected static void iterateResult(MongoIterable<?> iterable) {
        for (Object obj : iterable) {
        }
    }
}
