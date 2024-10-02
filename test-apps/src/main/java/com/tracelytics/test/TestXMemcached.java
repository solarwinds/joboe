/**
  * Tests XMemcached running in an instrumented environment
  */

package com.tracelytics.test;

import net.rubyeye.xmemcached.*;
import net.rubyeye.xmemcached.utils.*;

public class TestXMemcached extends TracedApp {

    public TestXMemcached() {
        setLayerName("test-xmemcached");
    }

    public void run() throws Exception {
        XMemcachedClientBuilder builder = new XMemcachedClientBuilder(AddrUtil.getAddresses("localhost:11211"));
        MemcachedClient client=builder.build();

        Thread.sleep(200);

        // Add
        client.add("test1", 1000, "a");
        client.addWithNoReply("test2", 1000, "b");

        // Append
        client.append("test1", "b");

        // Basic set and get:
        client.set("test2", 1234, "aaaa");

        String val = client.get("test2");
        System.out.println("got: " + val);

        val = client.get("fail2");
        System.out.println("got: " + val);

        // Delete:
        client.delete("test1");
        client.deleteWithNoReply("test2");

        // Incr and decr
        client.set("a", 1000 ,0);
        client.incr("a", 5, 1);
        client.incr("a", 5, 2);
        client.decr("a", 5, 1);
        
        // Counter (wraps incr/decr)
        Counter counter = client.getCounter("counter", 0);
        counter.incrementAndGet();
        counter.decrementAndGet();
        counter.addAndGet(2);
 
        // Prepend and append:
        client.set("b", 1000, "b");
        client.prepend("b","000");
        client.append("b","999");
        System.out.println("got: " + client.get("b"));

        // Replace
        client.replace("b", 1000, "test");

        client.shutdown();

        Thread.sleep(200);
    }

    public static void main(String args[]) throws Exception {
        TracedApp test = new TestXMemcached();
        test.runTrace();
    }
}
