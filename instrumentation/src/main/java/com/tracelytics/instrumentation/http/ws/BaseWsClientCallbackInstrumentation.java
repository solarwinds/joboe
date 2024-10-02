package com.tracelytics.instrumentation.http.ws;

import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.TvContextObjectAware;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;
import com.tracelytics.joboe.Metadata;

public abstract class BaseWsClientCallbackInstrumentation extends ClassInstrumentation {

    
  
    public static void layerExitAsync(String layerName, Object clientCallback, String responseXTraceId) {
        Event event;
        
        if (!(clientCallback instanceof TvContextObjectAware)) {
            logger.warn("ClientCallback is not wrapped by " + TvContextObjectAware.class.getName() + "! Unexpected.");
            return;
        }
        
        //Attempt to lookup the metadata of the entry event that spawns this client callback object
        Metadata entryContext = ((TvContextObjectAware) clientCallback).getTvContext();
        
        if (entryContext != null) {
            Context.setMetadata(entryContext);
            
            event = Context.createEvent();
            
            if (responseXTraceId != null) {
                event.addEdge(responseXTraceId);
            }
            
            event.addInfo("Layer", layerName,
                          "Label", "exit");
            
            event.setAsync(); //it should be an asynchronous event as it is using the Callback
            
            event.report();
        } else {
            logger.debug("Cannot retrieve previous edge for asynchronous CXF client calls, not from instrumented SOAP client.");
        }
        
        
    }
    
   
    
}
