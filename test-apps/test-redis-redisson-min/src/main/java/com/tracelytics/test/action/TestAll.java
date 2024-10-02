package com.tracelytics.test.action;

import java.util.Arrays;
import java.util.Queue;
import java.util.Set;

import org.redisson.Config;
import org.redisson.Redisson;
import org.redisson.core.MessageListener;
import org.redisson.core.RAtomicLong;
import org.redisson.core.RCountDownLatch;
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
        Redisson redisson = Redisson.create(config);
        
        RList<String> list = redisson.getList("list");
        list.addAll(Arrays.asList(LIST));
        list.remove(0);
        list.size();
        list.indexOf("not found");
        list.clear();
        
        
        RAtomicLong atomicLong = redisson.getAtomicLong("atomic");
        atomicLong.set(new Long(1000));
        atomicLong.addAndGet(5);
        atomicLong.incrementAndGet();
        
        
        Config config = redisson.getConfig();
        config.getConnectionPoolSize();
        
        
        RCountDownLatch latch = redisson.getCountDownLatch("atomic");
        latch.countDown();
        latch.getCount();
        
        
        
        RLock lock = redisson.getLock("lock");
        lock.isLocked();
        lock.isHeldByCurrentThread();
        lock.lock();
        lock.unlock();
        
        RMap<Integer, String> map = redisson.getMap("map");
        map.put(1, "a");
        map.put(2, "b");
        map.putIfAbsent(4, "d");
        map.get(1);
        map.containsKey(1);
        
        for (Integer key : map.keySet()) {
            System.out.println(key);
        }
        
        map.replace(1, "a");
        map.remove(1, "a");
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
        
//        RTopic<String> topic = redisson.getTopic("topic");
//        int listenerId = topic.addListener(new MessageListener<String>() {            
//            @Override
//            public void onMessage(String msg) {
//                System.out.println(msg);
//            }
//        });
//        topic.publish("hi");
//        topic.removeListener(listenerId);
        
        redisson.shutdown();
        
        printToOutput("Finished testing on distributed java objects");
        return SUCCESS;
    }

}
