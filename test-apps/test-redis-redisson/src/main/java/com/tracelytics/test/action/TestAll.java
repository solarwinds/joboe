package com.tracelytics.test.action;

import java.util.Arrays;
import java.util.Date;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;

import org.redisson.Config;
import org.redisson.core.MessageListener;
import org.redisson.core.Predicate;
import org.redisson.core.RAtomicLong;
import org.redisson.core.RBucket;
import org.redisson.core.RCountDownLatch;
import org.redisson.core.RHyperLogLog;
import org.redisson.core.RList;
import org.redisson.core.RLock;
import org.redisson.core.RMap;
import org.redisson.core.RTopic;

@SuppressWarnings("serial")
@org.apache.struts2.convention.annotation.Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
})
public class TestAll extends AbstractRedissonAction {

    @Override
    protected String test() throws Exception {
        RBucket<String> bucket = redisson.getBucket("bucket");
        
        bucket.clearExpire();
        bucket.delete();
        bucket.exists();
        bucket.existsAsync();
        bucket.expire(EXPIRY, TimeUnit.SECONDS);
        bucket.expireAt(new Date(System.currentTimeMillis() + EXPIRY * 1000));
        bucket.expireAt(System.currentTimeMillis() + EXPIRY * 1000);
        bucket.get();
        bucket.getAsync();
        bucket.remainTimeToLive();
        bucket.set(VALUE);
        bucket.set(VALUE, EXPIRY, TimeUnit.SECONDS);
        bucket.setAsync(VALUE);
        bucket.setAsync(VALUE, EXPIRY, TimeUnit.SECONDS);
        
        
        RList<String> list = redisson.getList("list");
        list.addAll(Arrays.asList(LIST));
        list.remove(0);
        list.size();
        list.indexOf("not found");
        list.clear();
        
        RAtomicLong atomicLong = redisson.getAtomicLong("atomic");
        atomicLong.set(1000);
        atomicLong.addAndGet(5);
        atomicLong.getAndIncrement();
        atomicLong.remainTimeToLive();
        atomicLong.delete();
        
        Config config = redisson.getConfig();
        config.useSingleServer().getDatabase();
        
        RCountDownLatch latch = redisson.getCountDownLatch("count-down-latch");
        latch.trySetCount(100);
        latch.countDown();
        latch.getCount();
        latch.delete();
        
        Deque<String> deque = redisson.getDeque("dequeue");
        deque.push(VALUE);
        deque.size();
        deque.addAll(Arrays.asList("2", "3", "4", "5"));
        deque.pollFirst();
        deque.pollLast();
        deque.poll();
        deque.pop();
        deque.remove("3");
        deque.clear();
        
        for (String item : deque) {
            System.out.println(item);
        }
        
        Iterator<String> iter = deque.descendingIterator();
        while (iter.hasNext()) {
            System.out.println(iter.next());
        }

        RHyperLogLog<String> log = redisson.getHyperLogLog("log");
        log.count();
        log.add("abc");
        log.addAsync("cde");
        log.delete();
        
        RLock lock = redisson.getLock("lock");
        lock.isLocked();
        lock.isHeldByCurrentThread();
        lock.lock();
        lock.unlock();
        lock.delete();
        
        RMap<Integer, String> map = redisson.getMap("map");
        map.put(1, "a");
        map.put(2, "b");
        map.putAsync(3, "c");
        map.putIfAbsent(4, "d");
        map.get(1);
        map.getAll(new HashSet<Integer>(Arrays.asList(1, 2, 3)));
        map.containsKey(1);
        map.filterKeys(new Predicate<Integer>() {
            @Override
            public boolean apply(Integer input) {
                return input > 1;
            } });
        map.fastPut(1, "b");
        map.fastPutAsync(2, "c");
        map.fastRemove(1);
        map.fastRemoveAsync(2);
        
        for (Integer key : map.keySet()) {
            System.out.println(key);
        }
        
        map.replace(1, "a");
        map.remove(1, "a");
        map.remainTimeToLive();
        map.removeAsync(2);
        map.size();
        
        
        Queue<String> queue = redisson.getQueue("queue");
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
        queue.clear();
        
        Set<Integer> set = redisson.getSet("set");
        set.add(1);
        set.addAll(Arrays.asList(2, 3, 4, 5, 6));
        set.contains(1);
        set.remove(1);
        set.size();
        set.toArray();
        for (Integer value : set) {
            System.out.println(value);
        }
        set.clear();
        
        SortedSet<Integer> sortedSet = redisson.getSortedSet("sorted-set");
        sortedSet.add(1);
        sortedSet.addAll(Arrays.asList(2, 3, 4, 5, 6));
        sortedSet.contains(1);
        sortedSet.remove(1);
        sortedSet.size();
        sortedSet.toArray();
        for (Integer value : sortedSet) {
            System.out.println(value);
        }
        sortedSet.first();
        sortedSet.last();
        sortedSet.clear();
        
        RTopic<String> topic = redisson.getTopic("topic");
        int listenerId = topic.addListener(new MessageListener<String>() {            
            @Override
            public void onMessage(String msg) {
                System.out.println(msg);
            }
        });
        topic.publish("hi");
        topic.publishAsync("hi-async");
        topic.removeListener(listenerId);
        
        printToOutput("Finished testing on distributed java objects");
        return SUCCESS;
    }

}
