package com.tracelytics.instrumentation.nosql;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.TvContextObjectAware;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;
import com.tracelytics.joboe.Metadata;

/**
 * Instrumentation for HbaseClient.Call. This is the lower client level class HBase used for request traffic. Take note that we trace the exit event here while 
 * the entry event (async) is started in HBaseClient.Connection instrumented by {@link HbaseClientConnectionInstrumentation}
 * @author Patson Luk
 *
 */
public class HbaseClientCallInstrumentation extends HbaseBaseInstrumentation {
    private static final String CLASS_NAME = HbaseClientCallInstrumentation.class.getName();
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        
        addTvContextObjectAware(cc);
        
        CtMethod callCompleteMethod = cc.getMethod("callComplete", "()V");
        
       
        if (shouldModify(cc, callCompleteMethod)) {
            insertAfter(callCompleteMethod, CLASS_NAME + ".layerExit(this);", true, false);
        }
         
        
        return true;
    }
    

    
    public static void layerExit(Object callObject) {
    	TvContextObjectAware call = (TvContextObjectAware) callObject;
        Metadata asyncContext = call.getTvContext();
		if (asyncContext != null) {
			boolean isAsync = call.getTvFromThreadId() != Thread.currentThread().getId(); 
			
			if (isAsync) { //probably all async anyway, but just to play safe...
				Context.setMetadata(asyncContext);
				Event event = Context.createEvent();
	            event.addInfo("Layer", LAYER_NAME,
	                          "Label", "exit");
	                          
	            event.setAsync();        
	            event.report();
				Context.clearMetadata(); //cleanup
			} else {
				Event event = Context.createEvent();
	            event.addInfo("Layer", LAYER_NAME,
	                          "Label", "exit");
	                          
	                    
	            event.report();
			}
            call.setTvContext(null);
        }
    }
}