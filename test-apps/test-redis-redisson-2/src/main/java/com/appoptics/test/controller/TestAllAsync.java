package com.appoptics.test.controller;

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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Controller
public class TestAllAsync extends AbstractRedissonController {

    @GetMapping("/test-all-async")
    public ModelAndView testAsync(Model model) throws InterruptedException, ExecutionException {
        RedissonClient client = getClient();
        clearExtendedOutput();
        RBucket<String> bucket = client.getBucket("bucket");
        bucket.setAsync(VALUE).get();
        bucket.getAsync();
        bucket.getAndDeleteAsync();
        bucket.getAndSetAsync(VALUE);
        bucket.setAsync(VALUE, EXPIRY, TimeUnit.SECONDS).get();
        bucket.trySetAsync(VALUE);
        bucket.trySetAsync(VALUE, EXPIRY, TimeUnit.SECONDS);
        bucket.compareAndSetAsync(VALUE, "2");
        bucket.sizeAsync();
        testRObject(bucket, "bucket");

        RList<String> list = client.getList("list");
        list.addAllAsync(Arrays.asList(LIST)).get();
        list.getAsync(0).get();
        list.getAsync(0, 2).get();

        list.<Boolean, Double>mapReduce().mapper(new ListMapper()).reducer(new Reducer()).executeAsync();
        list.addAfterAsync(list.get(3), "123").get();
        list.addBeforeAsync(list.get(3), "321").get();
        list.fastSetAsync(0, "0").get();
        list.fastRemoveAsync(0).get();
        list.readAllAsync();
        list.trimAsync(0, 2).get();
        list.removeAsync("0", 1).get();

        testRObject(list, "list");
        list.removeAsync(0).get();
        list.sizeAsync();
        list.indexOfAsync("not found");
        list.clear();


        RAtomicLong atomicLong = client.getAtomicLong("atomic");
        atomicLong.setAsync(1000);
        atomicLong.addAndGetAsync(5);
        atomicLong.getAndIncrementAsync();
        atomicLong.getAndDecrementAsync();
        atomicLong.decrementAndGetAsync();
        atomicLong.incrementAndGetAsync();
        atomicLong.compareAndSetAsync(0, 1);
        atomicLong.getAsync();
        atomicLong.getAndAddAsync(2);
        atomicLong.getAndDeleteAsync().get();
        atomicLong.setAsync(1000).get();
        testRObject(atomicLong, "atomic");

        Config config = client.getConfig();
        config.useSingleServer().getDatabase();

        RCountDownLatch latch = client.getCountDownLatch("count-down-latch");
        latch.trySetCountAsync(1).get();
        latch.countDownAsync().get();
        latch.getCountAsync().get();
        latch.await();
        latch.await(1, TimeUnit.SECONDS);

        latch.trySetCountAsync(1).get();
        testRObject(latch, "count-down-latch");

        RDeque<String> deque = client.getDeque("dequeue");

        deque.pushAsync(VALUE);
        deque.sizeAsync();
        deque.addAllAsync(Arrays.asList("2", "3", "4", "5"));
        deque.pollFirstAsync();
        deque.pollLastAsync();
        deque.pollAsync();
        deque.popAsync();
        deque.pollLastAndOfferFirstToAsync("dequeue");
        deque.readAll();

        testRObject(deque, "dequeue");

        RHyperLogLog<String> log = client.getHyperLogLog("log");
        RHyperLogLog<String> log2 = client.getHyperLogLog("log2");
        log.addAsync("abc");
        log2.addAllAsync(Arrays.asList("def", "zzz"));
        log.countWithAsync("log-2");
        log.mergeWithAsync("log-2");
        log.countAsync();
        testRObject(log, "log");

        RLock lock = client.getLock("lock");
        lock.forceUnlockAsync().get();
        lock.lockAsync().get();
        lock.tryLockAsync(1, 1, TimeUnit.SECONDS).get();
        lock.unlockAsync().get();
        lock.forceUnlockAsync().get();
        lock.getHoldCountAsync().get();
        lock.lockAsync().get();
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
        map.putAsync(1, random.nextFloat()).get();
        map.putAsync(2, random.nextFloat());
        map.putIfAbsentAsync(4, random.nextFloat());
        map.putAllAsync(Collections.singletonMap(3, random.nextFloat()));
        map.putAllAsync(Collections.singletonMap(4, random.nextFloat()), 1);
        map.getAsync(1);
        map.getAllAsync(new HashSet<Integer>(Arrays.asList(1, 2, 3))).get();

        map.loadAllAsync(false, 1);
        map.loadAllAsync(Collections.singleton(1), false, 1);

        map.<Boolean, Double>mapReduce().mapper(new MapMapper()).reducer(new Reducer()).executeAsync();
        map.valueSizeAsync(1);

        //map.addAndGet(1, 2F); //not working
        map.removeAsync(1);
        map.replaceAsync(2, random.nextFloat());
        map.replaceAsync(2, 0F, random.nextFloat());
        map.fastRemoveAsync(2);
        map.fastPutAsync(2, random.nextFloat());
        map.fastReplaceAsync(1, random.nextFloat());
        map.fastPutIfAbsentAsync(5, random.nextFloat());
        map.readAllMapAsync();
        testRObject(map, "map");


        RQueue<String> queue = client.getQueue("queue");

        queue.addAsync(VALUE);
        queue.offerAsync("1");
        queue.sizeAsync();
        queue.addAllAsync(Arrays.asList("2", "3", "4", "5"));
        queue.peekAsync();
        queue.pollAsync();
        queue.removeAsync("3").get();
        queue.readAllAsync();
        queue.pollLastAndOfferFirstToAsync("dequeue");
        testRObject(queue, "queue");
        queue.removeAllAsync(Arrays.asList("2", "3"));
        queue.clear();

        RSet<Integer> set = client.getSet("set");
        set.addAsync(1);
        set.addAllAsync(Arrays.asList(2, 3, 4, 5, 6));
        set.containsAsync(1);
        set.removeAsync(1).get();
        set.sizeAsync();

        set.<Boolean, Double>mapReduce().mapper(new SetMapper()).reducer(new Reducer()).executeAsync();
        set.randomAsync().get();
        set.randomAsync(2).get();
        set.removeRandomAsync().get();
        set.removeRandomAsync(1).get();

        RSet<Integer> set2 = client.getSet("set-2");
        set.moveAsync("set-2", 1).get();
        set.readAllAsync();
        set.unionAsync("set-2").get();
        set.readUnionAsync("set-2");
        set.diffAsync("set-2").get();
        set.readDiffAsync("set-2");
        set.intersectionAsync("set-2").get();
        set.readIntersectionAsync("set-2");
        set.addAsync(1).get();
        testRObject(set, "set");
        set.clear();

        RSortedSet<Integer> sortedSet = client.getSortedSet("sorted-set");
        sortedSet.addAsync(1);
        sortedSet.removeAsync(1);
        sortedSet.<Boolean, Double>mapReduce().mapper(new SetMapper()).reducer(new Reducer()).executeAsync();
        sortedSet.readAllAsync();
        sortedSet.addAsync(1).get();
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
//        topic.publishAsync("hi");
//        topic.countSubscribersAsync();
//        topic.removeListenerAsync(listenerId);
//        topic.removeAllListeners();

        RLexSortedSet lexSortedSet = client.getLexSortedSet("lex-sorted-set");
        lexSortedSet.addAllAsync(Arrays.asList("1", "2")).get();
        lexSortedSet.rangeAsync("1,", true, "2", false);

        RScoredSortedSet scoredSortedSet = client.getScoredSortedSet("scored-sorted-set");
        scoredSortedSet.addAsync(1, "1").get();
        scoredSortedSet.rankAsync("1");

        RListMultimap<Object, Object> listMultimap = client.getListMultimap("list-multi-map");
        listMultimap.putAsync("1", "a").get();
        listMultimap.putAsync("1", "b").get();
        listMultimap.getAllAsync("1");

        //Test different codec
        RBucket<String> kryoBucket = client.getBucket("bucket", new KryoCodec());
        kryoBucket.setAsync("kryo");

        //Test script
        RScript script = client.getScript(StringCodec.INSTANCE);
        script.evalAsync(RScript.Mode.READ_ONLY, "return true", RScript.ReturnType.BOOLEAN, Collections.emptyList()).get();

        printToOutput("Finished testing on distributed java objects");
        return getModelAndView("index");
    }

    private void testRObject(RObject object, String keyName) throws InterruptedException, ExecutionException {
        object.sizeInMemory();
        byte[] state = object.dumpAsync().get();
        object.deleteAsync().get();
        object.restoreAsync(state).get();
        object.deleteAsync().get();
        object.restoreAsync(state, EXPIRY, TimeUnit.SECONDS).get();
        object.restoreAndReplaceAsync(state).get();
        object.restoreAndReplaceAsync(state, EXPIRY, TimeUnit.SECONDS).get();
        object.touchAsync();
//        object.migrate(host, port, 1, 10000);
//        object.copy(host, port, 1, 10000);
        object.moveAsync(1).get();
        object.deleteAsync().get();
        object.restoreAsync(state).get();
        object.unlinkAsync().get();
        object.restoreAsync(state).get();
        object.renameAsync(keyName).get();
        object.renamenxAsync(keyName).get();
        object.isExistsAsync();

        if (object instanceof RExpirableAsync) {
            RExpirableAsync expirable = (RExpirableAsync) object;

            expirable.clearExpireAsync();
            expirable.expireAtAsync(new Date(System.currentTimeMillis() + EXPIRY * 1000));
            expirable.expireAtAsync(System.currentTimeMillis() + EXPIRY * 1000);
            expirable.clearExpireAsync();
            expirable.remainTimeToLiveAsync();
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

