package com.sample;

import java.util.Random;

import javax.jws.HandlerChain;
import javax.jws.WebMethod;
import javax.jws.WebService;

import com.appoptics.api.ext.Trace;
import com.appoptics.api.ext.TraceEvent;

@WebService(targetNamespace = "http://com.sample", name = "sample-soap", endpointInterface ="")
@HandlerChain(file = "handlers.xml")
public class Operator {
    
    private static final Random random = new Random();
    
    private static ThreadLocal<String> xtraceIdThreadLocal = new ThreadLocal<String>();
    
    @WebMethod(action="getInt")
    public int getInt(int max) {
        System.out.println("Recieved request with max : " + max);
        TraceEvent event;
        
        String xtraceId = xtraceIdThreadLocal.get();
        
        if (xtraceId != null) {
            event = Trace.continueTrace("SampleSoap", xtraceId);
        } else {
            event = Trace.startTrace("SampleSoap");
        }
        
        event.report();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        setContext(Trace.endTrace("SampleSoap"));
        
        return random.nextInt(max);
    }
    
    static void setContext(String xTraceId) {
        xtraceIdThreadLocal.set(xTraceId);
    }
    
    static String getContext() {
        return xtraceIdThreadLocal.get();
    }
    
    
}
