package com.tracelytics.test.action;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.bson.Document;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import com.mongodb.Block;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;

public abstract class AbstractMongoDbReactiveAction extends AbstractMongoDbAction {
    private static final String HOST_STRING = "mongodb://localhost"; //single setup
//    private static final String HOST_STRING = "mongodb://localhost:27017,localhost:27018"; //shard cluster query router 1/2
    
    private Set<Integer> activeLines = Collections.synchronizedSet(new HashSet<Integer>());
    
    private Lock lock = new ReentrantLock();
    private Condition waitCondition = lock.newCondition(); 
    
    protected MongoClient mongoClient;
    protected MongoDatabase mongoDatabase;
    protected MongoCollection<Document> mongoCollection;

    protected AbstractMongoDbReactiveAction() {
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

    


    protected <T> Subscriber<T> getSubscriber() {
        final int lineNumber = getLineNumber();
        activeLines.add(lineNumber);
        return new Subscriber<T>() {
            @Override
            public void onComplete() {
                lock.lock();
                try {
                    activeLines.remove(lineNumber);
                    System.out.println(activeLines);
                    waitCondition.signalAll();
                } finally {
                    lock.unlock();
                }
                
            }

            @Override
            public void onError(Throwable arg0) {
                arg0.printStackTrace();
                onComplete();
            }
            
            @Override
            public void onNext(T arg0) {
                //System.out.println(arg0);
            }

            @Override
            public void onSubscribe(Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
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
