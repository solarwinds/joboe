/**
 * EJB3 Test Class
 */
package com.tracelytics.test.ejb3;

import javax.ejb.*;

import org.jboss.ejb3.annotation.RemoteBinding;

import net.rubyeye.xmemcached.*;
import net.rubyeye.xmemcached.utils.*;
 
@Stateless
@RemoteBinding(jndiBinding="TestEJB3/remote")
public class TestEJB3 implements TestEJB3Remote {

    public String testOp(Integer a) {
        // Connect to memcache and increment a counter.
        String ret = ""; 
        MemcachedClient client = null;
        try { 
            XMemcachedClientBuilder builder = new XMemcachedClientBuilder(AddrUtil.getAddresses("localhost:11211"));
            client = builder.build();

            Counter counter = client.getCounter("test_service_counter", 0);
            ret = "" + counter.incrementAndGet();

            Thread.sleep(10);
        } catch(Exception ex) {
            ex.printStackTrace();
        } finally {
            if (client != null) {
                try {
                  client.shutdown();
                } catch(Exception ex) {
                  ex.printStackTrace();
                }
            }
        }

        return ret;
    }

    public String anotherOp(Integer a) {
        try {
            Thread.sleep(100);
        } catch(Exception ex) { 
        }

        return testOp(a);
    }
    
    public String testException() {
        throw new RuntimeException("Testing exception");
    }
}

