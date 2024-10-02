package com.tracelytics.test.action;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.bson.Document;

import com.mongodb.async.SingleResultCallback;
import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoClients;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.async.client.MongoDatabase;
import com.mongodb.async.client.MongoIterable;

public abstract class AbstractMongoDbAsyncAction extends AbstractMongoDbAction {
    private static final String HOST_STRING = "mongodb://localhost"; //single setup
//    private static final String HOST_STRING = "mongodb://localhost:27017,localhost:27018"; //shard cluster query router 1/2
    
    private Set<Integer> activeLines = Collections.synchronizedSet(new HashSet<Integer>());
    
    private Lock lock = new ReentrantLock();
    private Condition waitCondition = lock.newCondition(); 
    
    protected MongoClient mongoClient;
    protected MongoDatabase mongoDatabase;
    protected MongoCollection<Document> mongoCollection;

    protected AbstractMongoDbAsyncAction() {
        mongoClient = MongoClients.create(HOST_STRING);
        mongoDatabase = mongoClient.getDatabase(TEST_DB);
        mongoCollection = mongoDatabase.getCollection(TEST_COLLECTION);
    }

    protected MongoCollection<Document> getCollection() {
        return mongoCollection;
    }

    protected MongoDatabase getDatabase() {
        return mongoDatabase;
    }

    protected static void iterateResult(MongoIterable<?> iterable) {
        iterable.forEach(null, null);
    }

    //    protected <T> SingleResultCallback<T> getCounterCallback() {
    //        counterCallback.increaseCount();
    //        return (CounterCallback<T>) counterCallback;
    //    }

    protected <T> SingleResultCallback<T> getCallback() {
        final int lineNumber = getLineNumber();
        
        activeLines.add(lineNumber);
        return new SingleResultCallback<T>() {
            @Override
            public void onResult(T result, Throwable t) {
                lock.lock();
                try {
                    activeLines.remove(lineNumber);
                    System.out.println(activeLines);
                    waitCondition.signalAll();
                } finally {
                    lock.unlock();
                }
            }
        };
    }

    public int getLineNumber() {
        for (StackTraceElement frame : Thread.currentThread().getStackTrace()) {
            if (frame.getClassName().equals(getClass().getName())) {
                return frame.getLineNumber();
            }
        }

        return 0;
    }



    protected boolean waitUntilAllFinishes() {
        while (!activeLines.isEmpty()) {
            lock.lock();
            try {
                if (!waitCondition.await(30, TimeUnit.SECONDS)) {
                    addActionError("Time out! still running lines are " + activeLines + " from class " + getClass().getName());
                    return false;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }
        return true;
        
    }
}
