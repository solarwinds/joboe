package com.tracelytics.instrumentation.http.webflow;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;

/**
 * Wrapper for <code>org.springframework.webflow.action.ActionResultExposer</code> so we can obtain the result (name expression) embedded
 * 
 * @author pluk
 *
 */
public class ActionResultExposerPatcher extends ClassInstrumentation {
            
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        cc.addMethod(CtNewMethod.make("public Object tvGetExpression() { return getNameExpression(); }", cc)); 
        
        
        return tagInterface(cc, ActionResultExposer.class.getName());
    }
}