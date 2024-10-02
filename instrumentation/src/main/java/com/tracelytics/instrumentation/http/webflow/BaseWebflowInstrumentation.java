package com.tracelytics.instrumentation.http.webflow;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodSignature;

/**
 * Base class for all the webflow instrumentation
 * @author Patson Luk
 *
 */
public abstract class BaseWebflowInstrumentation extends ClassInstrumentation {
    protected static String LAYER_NAME = "webflow";
    
    protected static CtMethod findMethod(CtClass clazz, MethodSignature...signatures) {
        for (MethodSignature s: signatures) {
            
            try {
                return clazz.getMethod(s.getName(), s.getSignature()); 
            } catch(NotFoundException ex) {
                continue;
            }
        }
        return null;
    }
}
