package com.appoptics.test.controller;

import org.redisson.api.*;
import org.redisson.api.listener.MessageListener;
import org.redisson.api.map.MapLoader;
import org.redisson.api.mapreduce.*;
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
import java.util.concurrent.TimeUnit;

@Controller
public class TestAll extends AbstractRedissonController {

    @GetMapping("/test-all")
    public ModelAndView testAll(Model model) throws InterruptedException {
        RedissonClient client = getClient();
        clearExtendedOutput();
        RBucket<String> bucket = client.getBucket("bucket");
        bucket.set(VALUE);
        bucket.get();
        bucket.getAndDelete();
        bucket.getAndSet(VALUE);
        bucket.set(VALUE, EXPIRY, TimeUnit.SECONDS);
        bucket.trySet(VALUE);
        bucket.trySet(VALUE, EXPIRY, TimeUnit.SECONDS);
        bucket.compareAndSet(VALUE, "2");
        bucket.size();
        testRObject(bucket, "bucket");

        RList<String> list = client.getList("list");
        list.addAll(Arrays.asList(LIST));
        list.get(0);
        list.get(0, 2);

        list.<Boolean, Double>mapReduce().mapper(new ListMapper()).reducer(new Reducer()).execute();
        list.addAfter(list.get(3), "123");
        list.addBefore(list.get(3), "321");
        list.fastSet(0, "0");
        list.fastRemove(0);
        list.subList(0, 2);
        list.readAll();
        list.trim(0, 2);
        list.remove("0", 1);

        iterateCollection(list);
        testRObject(list, "list");
        list.remove(0);
        list.size();
        list.indexOf("not found");
        list.clear();


        RAtomicLong atomicLong = client.getAtomicLong("atomic");
        atomicLong.set(1000);
        atomicLong.addAndGet(5);
        atomicLong.getAndIncrement();
        atomicLong.getAndDecrement();
        atomicLong.decrementAndGet();
        atomicLong.incrementAndGet();
        atomicLong.compareAndSet(0, 1);
        atomicLong.get();
        atomicLong.getAndAdd(2);
        atomicLong.getAndDelete();
        atomicLong.set(1000);
        testRObject(atomicLong, "atomic");

        Config config = client.getConfig();
        config.useSingleServer().getDatabase();

        RCountDownLatch latch = client.getCountDownLatch("count-down-latch");
        latch.trySetCount(1);
        latch.countDown();
        latch.getCount();
        latch.await();
        latch.await(1, TimeUnit.SECONDS);

        latch.trySetCount(1);
        testRObject(latch, "count-down-latch");

        RDeque<String> deque = client.getDeque("dequeue");

        deque.push(VALUE);
        deque.size();
        deque.addAll(Arrays.asList("2", "3", "4", "5"));
        deque.pollFirst();
        deque.pollLast();
        deque.poll();
        deque.pop();
        deque.pollLastAndOfferFirstTo("dequeue");
        deque.readAll();
        iterateCollection(deque);

        Iterator<String> iter = deque.descendingIterator();
        while (iter.hasNext()) {
            System.out.println(iter.next());
        }
        testRObject(deque, "dequeue");

        RHyperLogLog<String> log = client.getHyperLogLog("log");
        RHyperLogLog<String> log2 = client.getHyperLogLog("log2");
        log.add("abc");
        log2.addAll(Arrays.asList("def", "zzz"));
        log.countWith("log-2");
        log.mergeWith("log-2");
        log.count();
        testRObject(log, "log");

        RLock lock = client.getLock("lock");
        lock.isLocked();
        lock.forceUnlock();
        lock.isHeldByCurrentThread();
        lock.lock();
        lock.lockInterruptibly();
        lock.tryLock(1, 1, TimeUnit.SECONDS);
        lock.unlock();
        lock.forceUnlock();
        lock.isHeldByThread(1);
        lock.isHeldByCurrentThread();
        lock.getHoldCount();
        lock.lock();
        testRObject(lock, "lock");

        Random random = new Random();
        Map<Integer, Float> source = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            source.put(i, random.nextFloat());
        }
        LocalCachedMapOptions<Integer, Float> options = LocalCachedMapOptions.<Integer, Float>defaults().loader(new MapLoader<Integer, Float>() {
            @Override
            public Float load(Integer key) {
                return source.get(key);
            }

            @Override
            public Iterable<Integer> loadAllKeys() {
                return source.keySet();
            }
        });
        RMap<Integer, Float> map = client.getMap("map", options);
        iterateCollection(map.values());
        map.put(1, random.nextFloat());
        map.put(2, random.nextFloat());
        map.putIfAbsent(4, random.nextFloat());
        map.putAll(Collections.singletonMap(3, random.nextFloat()));
        map.putAll(Collections.singletonMap(4, random.nextFloat()), 1);
        map.get(1);
        map.getAll(new HashSet<Integer>(Arrays.asList(1, 2, 3)));

        map.loadAll(false, 1);
        map.loadAll(Collections.singleton(1), false, 1);

        map.<Boolean, Double>mapReduce().mapper(new MapMapper()).reducer(new Reducer()).execute();
        map.getCountDownLatch(1);
        map.getPermitExpirableSemaphore(1);
        map.getSemaphore(1);
        map.getFairLock(1);
        map.getReadWriteLock(1);
        map.getLock(1);
        map.valueSize(1);

        iterateCollection(map.keySet());
        iterateCollection(map.values());
        //map.addAndGet(1, 2F); //not working
        map.remove(1);
        map.replace(2, random.nextFloat());
        map.replace(2, 0F, random.nextFloat());
        map.fastRemove(2);
        map.fastPut(2, random.nextFloat());
        map.fastReplace(1, random.nextFloat());
        map.fastPutIfAbsent(5, random.nextFloat());
        iterateCollection(map.readAllEntrySet());
        iterateCollection(map.readAllKeySet());
        map.readAllMap();
        iterateCollection(map.keySet());
        iterateCollection(map.keySet(1));
        iterateCollection(map.keySet("*"));
        iterateCollection(map.keySet("*", 1));
        iterateCollection(map.values());
        iterateCollection(map.values(1));
        iterateCollection(map.values("*"));
        iterateCollection(map.values("*", 1));
        iterateCollection(map.entrySet());
        iterateCollection(map.entrySet(1));
        iterateCollection(map.entrySet("*"));
        iterateCollection(map.entrySet("*", 1));
        testRObject(map, "map");


        RQueue<String> queue = client.getQueue("queue");

        queue.add(VALUE);
        queue.offer("1");
        queue.size();
        queue.addAll(Arrays.asList("2", "3", "4", "5"));
        queue.peek();
        queue.poll();
        queue.remove();
        queue.element();
        queue.toArray();
        queue.remove("3");
        queue.readAll();
        queue.pollLastAndOfferFirstTo("dequeue");
        testRObject(queue, "queue");
        iterateCollection(queue);
        queue.removeAll(Arrays.asList("2", "3"));
        queue.clear();

        RSet<Integer> set = client.getSet("set");
        set.add(1);
        set.addAll(Arrays.asList(2, 3, 4, 5, 6));
        set.contains(1);
        set.remove(1);
        set.size();
        set.toArray();
        iterateCollection(set);

        set.getCountDownLatch(1);
        set.getPermitExpirableSemaphore(1);
        set.getSemaphore(1);
        set.getFairLock(1);
        set.getReadWriteLock(1);
        set.getLock(1);
        iterateCollection(set.iterator(1));
        iterateCollection(set.iterator("*"));
        iterateCollection(set.iterator("*",1));

        set.<Boolean, Double>mapReduce().mapper(new SetMapper()).reducer(new Reducer()).execute();
        set.random();
        set.random(2);
        set.removeRandom();
        set.removeRandom(1);

        RSet<Integer> set2 = client.getSet("set-2");
        set.move("set-2", 1);
        set.readAll();
        set.union("set-2");
        set.readUnion("set-2");
        set.diff("set-2");
        set.readDiff("set-2");
        set.intersection("set-2");
        set.readIntersection("set-2");
        set.add(1);
        iterateCollection(set);
        testRObject(set, "set");
        set.clear();

        RSortedSet<Integer> sortedSet = client.getSortedSet("sorted-set");
        sortedSet.add(1);
        sortedSet.addAll(Arrays.asList(2, 3, 4, 5, 6));
        sortedSet.contains(1);
        sortedSet.remove(1);
        sortedSet.size();
        sortedSet.toArray();
        sortedSet.first();
        sortedSet.last();
        sortedSet.<Boolean, Double>mapReduce().mapper(new SetMapper()).reducer(new Reducer()).execute();
        sortedSet.readAll();
        sortedSet.trySetComparator(new SetComparator());
        iterateCollection(sortedSet);
        testRObject(sortedSet, "sorted-set");
        sortedSet.clear();

        //RTopic is not instrumented, all of them should be super short and not that useful
//        RTopic topic = client.getTopic("topic");
//        int listenerId = topic.addListener(String.class, new MessageListener<String>() {
//            @Override
//            public void onMessage(CharSequence channel, String msg) {
//                System.out.println(msg);
//            }
//        });
//        topic.publish("hi");
//        topic.countListeners();
//        topic.countSubscribers();
//        topic.removeListener(listenerId);
//        topic.getChannelNames();
//        topic.removeAllListeners();

        RLexSortedSet lexSortedSet = client.getLexSortedSet("lex-sorted-set");
        lexSortedSet.addAll(Arrays.asList("1", "2"));
        lexSortedSet.range("1,", true, "2", false);

        RScoredSortedSet scoredSortedSet = client.getScoredSortedSet("scored-sorted-set");
        scoredSortedSet.add(1, "1");
        scoredSortedSet.rank("1");

        RListMultimap<Object, Object> listMultimap = client.getListMultimap("list-multi-map");
        listMultimap.put("1", "a");
        listMultimap.put("1", "b");
        listMultimap.get("1");

        //Test different codec
        RBucket<String> kryoBucket = client.getBucket("bucket", new KryoCodec());
        kryoBucket.set("kryo");

        //Test script
        RScript script = client.getScript(StringCodec.INSTANCE);
        script.eval(RScript.Mode.READ_ONLY, "return true", RScript.ReturnType.BOOLEAN, Collections.emptyList());

        printToOutput("Finished testing on distributed java objects");
        return getModelAndView("index");
    }

    private void testRObject(RObject object, String keyName) throws InterruptedException {
        RedisClientConfig config = new RedisClientConfig();
        config.setAddress(host, port);
        RedisClient client = RedisClient.create(config);

        RedisConnection connection = client.connect();
        System.out.println(connection.sync(RedisCommands.CONFIG_GET, "databases"));

        object.sizeInMemory();
        byte[] state = object.dump();
        object.delete();
        object.restore(state);
        object.delete();
        object.restore(state, EXPIRY, TimeUnit.SECONDS);
        object.restoreAndReplace(state);
        object.restoreAndReplace(state, EXPIRY, TimeUnit.SECONDS);
        object.touch();
//        object.migrate(host, port, 1, 10000);
//        object.copy(host, port, 1, 10000);
        object.move(1);
        object.delete();
        object.restore(state);
        object.unlink();
        object.restore(state);
        object.rename(keyName);
        object.renamenx(keyName);
        object.isExists();

        if (object instanceof RExpirable) {
            RExpirable expirable = (RExpirable) object;

            expirable.clearExpire();
            expirable.expireAt(new Date(System.currentTimeMillis() + EXPIRY * 1000));
            expirable.expireAt(System.currentTimeMillis() + EXPIRY * 1000);
            expirable.clearExpire();
            expirable.remainTimeToLive();
        }
    }

    static class SetMapper implements RCollectionMapper<Integer, Boolean, Double> {
        @Override
        public void map(Integer value, RCollector<Boolean, Double> collector) {
            collector.emit(value / 2 == 0, value.doubleValue());
        }
    }

    static class MapMapper implements RMapper<Integer, Float, Boolean, Double> {
        @Override
        public void map(Integer key, Float value, RCollector<Boolean, Double> collector) {
            collector.emit(key / 2 == 0, value.doubleValue());
        }
    }

    static class ListMapper implements RCollectionMapper<String, Boolean, Double> {
        @Override
        public void map(String value, RCollector<Boolean, Double> collector) {
            double doubleValue = Double.valueOf(value);
            collector.emit(doubleValue > 0.5, Double.valueOf(value));
        }
    }
    static class Reducer implements RReducer<Boolean, Double> {

        @Override
        public Double reduce(Boolean reducedKey, Iterator<Double> iter) {
            double total = 0;
            int count = 0;
            while (iter.hasNext()) {
                total += iter.next();
                count ++;
            }
            return total / count;
        }
    }

    static class SetComparator implements Comparator<Integer> {

        @Override
        public int compare(Integer o1, Integer o2) {
            return o1 - o2;
        }
    }

}



