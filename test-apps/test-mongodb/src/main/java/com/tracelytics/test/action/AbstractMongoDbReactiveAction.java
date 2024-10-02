package com.tracelytics.test.action;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.mongodb.ClientSessionOptions;
import com.mongodb.reactivestreams.client.*;
import org.bson.Document;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

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

    protected ClientSession getClientSession() {
        SubscriberHelpers.ObservableSubscriber<ClientSession> subscriber = new SubscriberHelpers.ObservableSubscriber<>();
        mongoClient.startSession(ClientSessionOptions.builder().causallyConsistent(false).build()).subscribe(subscriber);

        try {
            return subscriber.await().getReceived().get(0);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return null;
        }
    }


    protected <T> LineNumberSubscriber<T> getSubscriber() {
        final int lineNumber = getLineNumber();
        activeLines.add(lineNumber);
        return new LineNumberSubscriber<T>(lineNumber);
    }

    public int getLineNumber() {
        for (StackTraceElement frame : Thread.currentThread().getStackTrace()) {
            if (frame.getClassName().equals(getClass().getName())) {
                return frame.getLineNumber();
            }
        }

        return 0;
    }

    protected class LineNumberSubscriber<T> implements Subscriber<T> {
        private final int lineNumber;
        private final CountDownLatch localLatch;
        private T lastValue;

        public LineNumberSubscriber(int lineNumber) {
            this.lineNumber = lineNumber;
            localLatch = new CountDownLatch(1);
        }

        @Override
        public void onComplete() {
            lock.lock();
            try {
                activeLines.remove(lineNumber);
                System.out.println(activeLines);
                waitCondition.signalAll();
            } finally {
                lock.unlock();
                localLatch.countDown();
            }

        }

        @Override
        public void onError(Throwable arg0) {
            System.err.println("Error executing line " + lineNumber);
            arg0.printStackTrace();
            onComplete();
        }

        @Override
        public void onNext(T arg0) {
            //System.out.println(arg0);
            lastValue = arg0;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            subscription.request(Long.MAX_VALUE);
        }

        protected T waitUntilThisFinishes() {
            try {
                localLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                return lastValue;
            }
        }
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

    protected <T> T blockingExecute(Publisher<T> publisher) {
        LineNumberSubscriber<T> subscriber = getSubscriber();
        publisher.subscribe(subscriber);

        return subscriber.waitUntilThisFinishes();
    }
}
