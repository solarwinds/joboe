package com.appoptics.test.controller;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.redisson.api.*;
import org.redisson.api.listener.MessageListener;
import org.redisson.api.map.MapLoader;
import org.redisson.api.mapreduce.RCollectionMapper;
import org.redisson.api.mapreduce.RCollector;
import org.redisson.api.mapreduce.RMapper;
import org.redisson.api.mapreduce.RReducer;
import org.redisson.client.RedisClient;
import org.redisson.client.RedisClientConfig;
import org.redisson.client.RedisConnection;
import org.redisson.client.codec.StringCodec;
import org.redisson.client.protocol.RedisCommands;
import org.redisson.codec.KryoCodec;
import org.redisson.config.Config;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Controller
public class TestAllReactive extends AbstractRedissonController {

    @GetMapping("/test-all-reactive")
    public ModelAndView test(Model model) throws Throwable {
        clearExtendedOutput();

        RedissonReactiveClient client = getReactiveClient();

        RBucketReactive<Object> bucket = client.getBucket("bucket");
        SingleResultSubscriber subscriber = new SingleResultSubscriber();

        bucket.set(VALUE).subscribe(subscriber);
        subscriber.get();
        bucket.get().subscribe(subscriber);

        bucket.getAndDelete().subscribe(subscriber);
        bucket.getAndSet(VALUE).subscribe(subscriber);
        bucket.set(VALUE, EXPIRY, TimeUnit.SECONDS).subscribe(subscriber);
        bucket.trySet(VALUE).subscribe(subscriber);
        bucket.trySet(VALUE, EXPIRY, TimeUnit.SECONDS).subscribe(subscriber);
        bucket.compareAndSet(VALUE, "2").subscribe(subscriber);;
        bucket.size().subscribe(subscriber);
        testRObject(bucket, "bucket");

        RListReactive<Object> list = client.getList("list");
        executeSingleResult(list.addAll(Arrays.asList(LIST)));
        list.get(0).subscribe(subscriber);
        list.get(0, 2).subscribe(subscriber);

        executeSingleResult(list.addAll(new Publisher<String>() {
            @Override
            public void subscribe(Subscriber<? super String> s) {
                s.onSubscribe(new Subscription() {
                    private boolean consumed = false;
                    @Override
                    public void request(long n) {
                        if (n > 0) {
                            if (!consumed) {
                                s.onNext("from publisher");
                                consumed = true;
                            } else {
                                s.onComplete();
                            }
                        }
                    }

                    @Override
                    public void cancel() {

                    }
                });
            }
        }));
        executeSingleResult(list.addAfter(executeSingleResult(list.get(3)), "123"));
        executeSingleResult(list.addBefore(executeSingleResult(list.get(3)), "321"));
        executeSingleResult(list.fastSet(0, "0"));
        executeSingleResult(list.fastRemove(0));
        list.readAll().subscribe(subscriber);
        executeSingleResult(list.trim(0, 1));
        executeSingleResult(list.remove(1));

        iterateCollection(list);
        testRObject(list, "list");
        list.remove(0).subscribe(subscriber);
        list.size().subscribe(subscriber);
        list.indexOf("not found").subscribe(subscriber);


//        RAtomicLongReactive atomicLong = client.getAtomicLong("atomic");
//        atomicLong.set(1000);
//        atomicLong.addAndGet(5);
//        atomicLong.getAndIncrement();
//        atomicLong.getAndDecrement();
//        atomicLong.decrementAndGet();
//        atomicLong.incrementAndGet();
//        atomicLong.compareAndSet(0, 1);
//        atomicLong.get();
//        atomicLong.getAndAdd(2);
//        atomicLong.getAndDelete();
//        atomicLong.set(1000);
//        testRObject(atomicLong, "atomic");
//
//        Config config = client.getConfig();
//        config.useSingleServer().getDatabase();
//
//
//        RDequeReactive<Object> deque = client.getDeque("dequeue");
//
//        deque.push(VALUE);
//        deque.size();
//        deque.addAll(Arrays.asList("2", "3", "4", "5"));
//        deque.pollFirst();
//        deque.pollLast();
//        deque.poll();
//        deque.pop();
//        deque.pollLastAndOfferFirstTo("dequeue");
//        iterateCollection(deque);
//
//        Publisher<Object> publisher = deque.descendingIterator();
//        publisher.subscribe(new PrintSubscriber<>());
//        testRObject(deque, "dequeue");
//
//        RHyperLogLogReactive<Object> log = client.getHyperLogLog("log");
//        RHyperLogLogReactive<Object> log2 = client.getHyperLogLog("log2");
//        log.add("abc");
//        log2.addAll(Arrays.asList("def", "zzz"));
//        log.countWith("log-2");
//        log.mergeWith("log-2");
//        log.count();
//        testRObject(log, "log");
//
//        RLockReactive lock = client.getLock("lock");
//        lock.forceUnlock();
//        lock.lock();
//        lock.tryLock(1, 1, TimeUnit.SECONDS);
//        lock.unlock();
//        lock.forceUnlock();
//        lock.lock();
//        testRObject(lock, "lock");
//
//        Random random = new Random();
//        Map<Integer, Float> source = new HashMap<>();
//        for (int i = 0; i < 10; i++) {
//            source.put(i, random.nextFloat());
//        }
//        LocalCachedMapOptions<Integer, Float> options = LocalCachedMapOptions.<Integer, Float>defaults().loader(new MapLoader<Integer, Float>() {
//            @Override
//            public Float load(Integer key) {
//                return source.get(key);
//            }
//
//            @Override
//            public Iterable<Integer> loadAllKeys() {
//                return source.keySet();
//            }
//        });
//
//        RMapReactive<Integer, Float> map = client.getMap("map", options);
//        iterateCollection(map.valueIterator());
//        map.put(1, random.nextFloat());
//        map.put(2, random.nextFloat());
//        map.putIfAbsent(4, random.nextFloat());
//        map.putAll(Collections.singletonMap(3, random.nextFloat()));
//        map.get(1);
//        map.getAll(new HashSet<Integer>(Arrays.asList(1, 2, 3)));
//
//        map.loadAll(false, 1);
//        map.loadAll(Collections.singleton(1), false, 1);
//
//        map.getPermitExpirableSemaphore(1);
//        map.getSemaphore(1);
//        map.getFairLock(1);
//        map.getReadWriteLock(1);
//        map.getLock(1);
//        map.valueSize(1);
//
//        //map.addAndGet(1, 2F); //not working
//        map.remove(1);
//        map.replace(2, random.nextFloat());
//        map.replace(2, 0F, random.nextFloat());
//        map.fastRemove(2);
//        map.fastPut(2, random.nextFloat());
//        map.fastPutIfAbsent(5, random.nextFloat());
//        iterateCollection(map.readAllEntrySet());
//        iterateCollection(map.readAllKeySet());
//        map.readAllMap();
//        iterateCollection(map.keyIterator());
//        iterateCollection(map.keyIterator(1));
//        iterateCollection(map.keyIterator("*"));
//        iterateCollection(map.keyIterator("*", 1));
//        iterateCollection(map.valueIterator());
//        iterateCollection(map.valueIterator(1));
//        iterateCollection(map.valueIterator("*"));
//        iterateCollection(map.valueIterator("*", 1));
//        iterateCollection(map.entryIterator());
//        iterateCollection(map.entryIterator(1));
//        iterateCollection(map.entryIterator("*"));
//        iterateCollection(map.entryIterator("*", 1));
//        testRObject(map, "map");
//
//
//        RQueueReactive<Object> queue = client.getQueue("queue");
//
//        queue.add(VALUE);
//        queue.offer("1");
//        queue.size();
//        queue.addAll(Arrays.asList("2", "3", "4", "5"));
//        queue.peek();
//        queue.poll();
//        queue.remove("3");
//        queue.pollLastAndOfferFirstTo("dequeue");
//        testRObject(queue, "queue");
//        iterateCollection(queue);
//        queue.removeAll(Arrays.asList("2", "3"));
//
//        RSetReactive<Object> set = client.getSet("set");
//        set.add(1);
//        set.addAll(Arrays.asList(2, 3, 4, 5, 6));
//        set.contains(1);
//        set.remove(1);
//        set.size();
//        iterateCollection(set);
//
//        set.getPermitExpirableSemaphore(1);
//        set.getSemaphore(1);
//        set.getFairLock(1);
//        set.getReadWriteLock(1);
//        set.getLock(1);
//        iterateCollection(set.iterator(1));
//        iterateCollection(set.iterator("*"));
//        iterateCollection(set.iterator("*",1));
//
//        set.random();
//        set.removeRandom();
//        set.removeRandom(1);
//
//        RSetReactive<Object> set2 = client.getSet("set-2");
//        set.move("set-2", 1);
//        set.readAll();
//        set.union("set-2");
//        set.readUnion("set-2");
//        set.diff("set-2");
//        set.readDiff("set-2");
//        set.intersection("set-2");
//        set.readIntersection("set-2");
//        set.add(1);
//        iterateCollection(set);
//        testRObject(set, "set");
//
//        RLexSortedSetReactive sortedSet = client.getLexSortedSet("sorted-set");
//        sortedSet.add("1");
//        sortedSet.addAll(Arrays.asList("2", "3", "4", "5", "6"));
//        sortedSet.contains("1");
//        sortedSet.remove("1");
//
//        //RLexSortedSetReactive
//        sortedSet.removeRange("1", false, "3", false);
//        sortedSet.removeRangeTail("5", false);
//        sortedSet.removeRangeHead("3", false);
//        sortedSet.countTail("3", true);
//        sortedSet.countHead("5", true);
//        sortedSet.rangeTail("3", true);
//        sortedSet.rangeHead("3", true);
//        sortedSet.range("3", true, "4", true);
//        sortedSet.rangeTail("3", true);
//        sortedSet.rangeHead("3", true);
//        sortedSet.range("3", true, "4", true, 0, 1);
//        sortedSet.count("3", true, "4", true);
//
////RScoredSortedSetReactive
//        sortedSet.pollLastFromAny(1, TimeUnit.SECONDS, "queue");
//        sortedSet.pollFirstFromAny(1, TimeUnit.SECONDS, "queue");
//        sortedSet.pollFirst();
//        sortedSet.pollLast();
//        sortedSet.first();
//        sortedSet.last();
//        sortedSet.firstScore();
//        sortedSet.lastScore();
//        iterateCollection(sortedSet.iterator());
//        sortedSet.removeRangeByScore(0, true, 0, true);
//        sortedSet.removeRangeByRank(0, 1);
//        sortedSet.rank("3");
//        sortedSet.revRank("3");
//        sortedSet.getScore("3");
//        sortedSet.addAndGetRank(1, "1");
//        sortedSet.addAndGetRevRank(5, "2");
//        sortedSet.tryAdd(7, "7");
//
//        RScoredSortedSetReactive<String> scoredSortedSet = client.getScoredSortedSet("scored-set");
//        executeSingleResult(scoredSortedSet.add(8, "8"));
//        sortedSet.intersection("scored-set");
//
//        //RSortableReactive
//        sortedSet.readSorted(SortOrder.ASC);
//        sortedSet.sortTo("dest", SortOrder.DESC);
//
//        iterateCollection(sortedSet);
//        testRObject(sortedSet, "sorted-set");
//
//
//        //RTopicReactive is not instrumented, all of them should be super short and not that useful
////        RTopicReactive topic = client.getTopic("topic");
////        Integer listenerId = executeSingleResult(topic.addListener(String.class, new MessageListener<String>() {
////            @Override
////            public void onMessage(CharSequence channel, String msg) {
////                System.out.println(msg);
////            }
////        }));
////        topic.publish("hi");
////        topic.countSubscribers();
////        topic.removeListener(listenerId);
////        topic.getChannelNames();
//
//
//        //Test different codec
//        RBucketReactive<Object> kryoBucket = client.getBucket("bucket", new KryoCodec());
//        kryoBucket.set("kryo");
//
//        //Test script
//        RScriptReactive script = client.getScript(StringCodec.INSTANCE);
//        executeSingleResult(script.eval(RScript.Mode.READ_ONLY, "return true", RScript.ReturnType.BOOLEAN, Collections.emptyList()));

        printToOutput("Finished testing on reactive java objects");
        return getModelAndView("index");
    }

    private void testRObject(RObjectReactive object, String keyName) throws Throwable {
        byte[] state = executeSingleResult(object.dump());
        executeSingleResult(object.delete());
        executeSingleResult(object.restore(state));
        executeSingleResult(object.delete());
        executeSingleResult(object.restore(state, EXPIRY, TimeUnit.SECONDS));
        executeSingleResult(object.restoreAndReplace(state));
        executeSingleResult(object.restoreAndReplace(state, EXPIRY, TimeUnit.SECONDS));
        executeSingleResult(object.touch());
//        object.migrate(host, port, 1, 10000);
//        object.copy(host, port, 1, 10000);
        executeSingleResult(object.move(1));
        executeSingleResult(object.delete());
        executeSingleResult(object.restore(state));
        executeSingleResult(object.unlink());
        executeSingleResult(object.restore(state));
        executeSingleResult(object.rename(keyName));
        executeSingleResult(object.renamenx(keyName));
        executeSingleResult(object.isExists());

        if (object instanceof RExpirableReactive) {
            RExpirableReactive expirable = (RExpirableReactive) object;

            executeSingleResult(expirable.clearExpire());
            executeSingleResult(expirable.expireAt(new Date(System.currentTimeMillis() + EXPIRY * 1000)));
            executeSingleResult(expirable.expireAt(System.currentTimeMillis() + EXPIRY * 1000));
            executeSingleResult(expirable.clearExpire());
            executeSingleResult(expirable.remainTimeToLive());
        }
    }

    private <T> T executeSingleResult(Publisher<T> publisher) throws Throwable {
        return executeSingleResult(publisher, false);
    }

    private <T> T executeSingleResult(Publisher<T> publisher, boolean ignoreException) throws Throwable {
        SingleResultSubscriber<T> subscriber = new SingleResultSubscriber<>();
        publisher.subscribe(subscriber);
        try {
            return subscriber.get();
        } catch (Throwable e) {
            if (!ignoreException) {
                throw e;
            }
            return null;
        }
    }



    private static class SingleResultSubscriber<T> implements Subscriber<T> {
        private final CountDownLatch latch = new CountDownLatch(1);
        private T result;
        @Override
        public void onSubscribe(Subscription s) {
            s.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(T o) {
            result = o;
            latch.countDown();
        }

        @Override
        public void onError(Throwable t) {
            latch.countDown();
        }

        @Override
        public void onComplete() {
            latch.countDown();
        }

        private T get() throws Throwable {
            latch.await();
            return result;
        }
    }


    private static class PrintSubscriber<T> implements Subscriber<T> {
        @Override
        public void onSubscribe(Subscription s) {
            s.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(Object o) {
            System.out.println(o);
        }

        @Override
        public void onError(Throwable t) {

        }

        @Override
        public void onComplete() {

        }
    }

    private <T> void iterateCollection(RCollectionReactive<T> collection) {
        iterateCollection(collection.iterator());
    }

    private <T> void iterateCollection(Publisher<T> publisher) {
        publisher.subscribe(new PrintSubscriber<>());
    }
}



