package com.tracelytics.instrumentation.http.ws;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtField;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;

/**
 * Wrap the Client Request so we can set the Async flag from the AsyncWebResource
 * @author Patson Luk
 *
 */
public class SunJerseyClientRequestInstrumentation extends ClassInstrumentation {

    private static final String CLASS_NAME = SunJerseyClientRequestInstrumentation.class.getName();
    private static final String IS_ASYNC_FIELD_NAME = "tvIsAsync";
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        
        CtField field = CtField.make("private boolean " + IS_ASYNC_FIELD_NAME + " = false;", cc);
        cc.addField(field);
        
        cc.addMethod(CtNewMethod.make( "public void setAsync(boolean isAsync) { " + IS_ASYNC_FIELD_NAME + " = isAsync; }", cc));
        cc.addMethod(CtNewMethod.make( "public boolean isAsync() { return " + IS_ASYNC_FIELD_NAME + "; }", cc));
        
        tagInterface(cc, SunJerseyClientRequest.class.getName());
        
        return true;    
    }
}