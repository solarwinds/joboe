package com.tracelytics.test.jmx;

import net.rubyeye.xmemcached.*;
import net.rubyeye.xmemcached.utils.*;

import org.jboss.system.ServiceMBeanSupport;

public class TestService extends ServiceMBeanSupport implements TestServiceMBean
{
   // Our message attribute
   private String message = "** not set **";

   // Getters and Setters
   public String getMessage()
   {
      return message;
   }
   public void setMessage(String message)
   {
      this.message = message;
   }

   // The printMessage operation
   public void printMessage()
   {
      log.info(message);
   }

   public String testOp(Integer a) {
      // Connect to memcache and increment a counter.
      String ret = ""; 
      MemcachedClient client = null;
      try { 
          XMemcachedClientBuilder builder = new XMemcachedClientBuilder(AddrUtil.getAddresses("localhost:11211"));
          client = builder.build();

          Counter counter = client.getCounter("test_service_counter", 0);
          ret = "" + counter.incrementAndGet();
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

   // The lifecycle
   protected void startService() throws Exception
   {
      log.info("Starting with message=" + message);
   }
   protected void stopService() throws Exception
   {
      log.info("Stopping with message=" + message);
   }
}
