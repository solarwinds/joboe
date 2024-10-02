package com.tracelytics.instrumentation.http.webflow;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;

/**
 * 
 * Base class for flow entry point instrumentation
 * @author Patson Luk
 *
 */
public abstract class FlowEntryPointInstrumentation extends BaseWebflowInstrumentation {
    public static final String CLASS_NAME = FlowEntryPointInstrumentation.class.getName();
    
    /**
     *   Modifies the handle method
     * @param method
     * @throws CannotCompileException
     */
    protected void modifyHandleMethod(CtMethod method)
            throws CannotCompileException {

        insertBefore(method, CLASS_NAME + ".layerEntry();");
        insertAfter(method, CLASS_NAME + ".layerExit();", true);
    }



    /**
     * Track the method entry as the start point of a layer/extend
     */
    public static void layerEntry() {
        Event event = Context.createEvent();
        
        event.addInfo("Layer", LAYER_NAME,
                      "Label", "entry");
        
        event.report();
    }
    
   
    /**
     * Track the method exit as the end point of a layer/extend
     */
    public static void layerExit() {
    	Event event = Context.createEvent();
        event.addInfo("Layer", LAYER_NAME,
                      "Label", "exit");
                
        event.report();
    	
    }
}



