package com.tracelytics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import com.appoptics.api.ext.AgentChecker;
import com.appoptics.api.ext.Trace;
import com.appoptics.api.ext.TraceEvent;



public class TestStream {
    private static final String SPAN_LABEL = "test-span";
    private static final int RUN_COUNT = 10;
    public static void main(String[] args) throws IOException, InterruptedException {
        AgentChecker.waitUntilAgentReady(10, TimeUnit.SECONDS);
        Trace.startTrace(SPAN_LABEL).report();
        Trace.setTransactionName("test-parallel-stream");
      
        
        IntStream intStream = IntStream.range(0, RUN_COUNT);
        
        Trace.createEntryEvent("int-stream").report();
        intStream.forEach(i -> testOperation("stream" + i));
        Trace.createExitEvent("int-stream").report();
        
        intStream = IntStream.range(0, RUN_COUNT);
        Trace.createEntryEvent("parallel-int-stream").report();
        intStream.parallel().forEach(i -> testOperation("parallel-int-stream" + i));
        Trace.createExitEvent("parallel-int-stream").report();
        
        
        
        List<Integer> list = new ArrayList<>();
        IntStream.range(0, RUN_COUNT).forEach(i -> list.add(i));
        Trace.createEntryEvent("parallel-array-stream").report();
        list.parallelStream().forEach(i -> testOperation("parallel-array-list-stream" + i));
        Trace.createExitEvent("parallel-array-stream").report();
        
        Trace.endTrace(SPAN_LABEL);
        
    }
    
    
    public static void testOperation(String operationName) {
        TraceEvent entryEvent = Trace.createEntryEvent(operationName);
        entryEvent.addBackTrace();
        entryEvent.report();
        try {
            TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Trace.createExitEvent(operationName).report();
    }
    
   
}
