package com.tracelytics.test.thrift.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

import com.appoptics.api.ext.Trace;
import com.appoptics.api.ext.TraceEvent;
import com.tracelytics.test.thrift.ServiceExample;

public class ThriftPerformanceTest {
    private static final int RUN_COUNT = 1000;
    private static List<Long> durations = new ArrayList<Long>();

    public static void main(String[] args) throws TException, InterruptedException {
        TTransport transport = new TFramedTransport(new TSocket("localhost", 8081));
        TProtocol protocol = new TCompactProtocol(transport);
        final ServiceExample.Client client = new ServiceExample.Client(protocol);
        
        TraceEvent e = Trace.startTrace("thrift-test");
        e.report();
        
        transport.open();
        for (int i = 1; i <= RUN_COUNT; i++) {
            long start = System.nanoTime();
            client.getBean(1, "string");
            long end = System.nanoTime();
         
            long duration = end - start;
            
            int dividend = RUN_COUNT / 10;
            if (dividend == 0) {
                dividend = 1;
            }
            
            if (i % dividend == 0) {
                System.out.println((i * 100 / RUN_COUNT) + "% Median: " + getMedian(durations));
            }
            
            durations.add(duration);
        }
        transport.close();
        
        Trace.endTrace("thrift-test");
        
        System.out.println("Average: " + getAverage(durations));
    }
    
    private static Long getAverage(List<Long> durations) {
        long sum = 0;
        for (long duration : durations) {
            sum += duration;
        }
        
        return sum / durations.size();
    }


    private static <T extends Comparable<? super T>> T getMedian(List<T> data) {
        if (data.isEmpty()) {
            return null;
        }
        
        Collections.sort(data);
        
        return data.get(data.size() / 2);
    }
}
