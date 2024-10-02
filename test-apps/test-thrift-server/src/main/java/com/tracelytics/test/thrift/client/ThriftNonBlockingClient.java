package com.tracelytics.test.thrift.client;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.thrift.TException;
import org.apache.thrift.async.TAsyncClientManager;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.transport.TNonblockingSocket;
import org.apache.thrift.transport.TTransport;

import com.appoptics.api.ext.Trace;
import com.tracelytics.test.thrift.ServiceExample;
import com.tracelytics.test.thrift.ServiceExample.AsyncClient.getBean_call;

public class ThriftNonBlockingClient {
    private static final int DEFAULT_REQUEST_COUNT = 2000;
    private static final int DEFAULT_THREAD_POOL_SIZE = 40;

    public static void main(String[] args) {
        sendRequests(args.length >= 1 ? args[0] : null,
                     args.length >= 2 ? Integer.valueOf(args[1]) : DEFAULT_REQUEST_COUNT, 
                     args.length >= 3 ? Integer.valueOf(args[2]) : DEFAULT_THREAD_POOL_SIZE);
    }
    
    private static long start;

    public static void sendRequests(String host, int requestCount, int threadPoolSize) {
        Trace.startTrace("thrift-non-blocking").report();
        ThriftClientThread.serverUri = host == null ? "localhost" : host;

        

            //            TTransport transport = new TFramedTransport(new TSocket(serverUri, 8081));
            //            TProtocol protocol = new TCompactProtocol(transport);
            //final ServiceExample.Client client = new ServiceExample.Client(protocol);

          
            ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);
          
            start = System.currentTimeMillis();
            for (int i = 0; i < requestCount; i++) {
                executorService.submit(new ThriftClientThread(i));
            }
        
            executorService.shutdown();
            
            Trace.endTrace("thrift-non-blocking");
    }
    
    private static class ThriftClientThread extends Thread {
        private static String serverUri;
        
        private static TProtocolFactory protocolFactory = new TProtocolFactory() {

            @Override
            public TProtocol getProtocol(TTransport trans) {
                return new TCompactProtocol(trans);
            }
        };

        private static ServiceExample.AsyncClient.Factory factory;

        private static AtomicInteger counter = new AtomicInteger(0);
        
        static {
            try {
                factory =  new ServiceExample.AsyncClient.Factory(new TAsyncClientManager(), protocolFactory);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        private int id;
        
        public ThriftClientThread(int id) {
            this.id = id;
        }
        
        public void run() {
//            System.out.println("RUNNING thread with id [" + id + "]");
            try {
                TNonblockingSocket protocol = new TNonblockingSocket(serverUri, 8081);
                ServiceExample.AsyncClient client = factory.getAsyncClient(protocol);

                org.apache.thrift.async.AsyncMethodCallback<getBean_call> callback =  new org.apache.thrift.async.AsyncMethodCallback<getBean_call>() {

                    @Override
                    public void onComplete(getBean_call response) {
                        // TODO Auto-generated method stub
                        try {
                            int count = counter.incrementAndGet();
                            if (count % 100 == 0) {
                                System.out.println(count + " th (new) result: " + response.getResult());
                                System.out.println("Time elaspsed [" + (System.currentTimeMillis() - start) + "]");
                            }
                        } catch (TException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } finally {
                            synchronized(this) {
                                notifyAll();
                            }
                        }
                        
                    }

                    @Override
                    public void onError(Exception exception) {
                        int count = counter.incrementAndGet();
                        if (count % 100 == 0) {
                            System.out.println(count + " th (new) failed");
                            System.out.println("Time elaspsed [" + (System.currentTimeMillis() - start) + "]");
                        }
                        synchronized(this) {
                            notifyAll();
                        }
                    }
                };
                
                client.getBean(1, "string", callback);
                
                synchronized (callback) {
                    callback.wait();
                }
                
            } catch (TException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    
    
}
