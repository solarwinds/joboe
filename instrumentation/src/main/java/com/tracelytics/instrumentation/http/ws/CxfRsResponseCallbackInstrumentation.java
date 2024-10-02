package com.tracelytics.instrumentation.http.ws;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtConstructor;
import com.tracelytics.ext.javassist.CtField;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;


/**
 * Added a handle to get the clientCallback added in the ctor. This is used by Cxf's Webclient asynchronous calls instrumentation
 * @author Patson Luk
 *
 */
public class CxfRsResponseCallbackInstrumentation extends ClassInstrumentation {
    
    private static final String CALLBACK_FIELD_NAME = "tvCallback";

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        addTvContextObjectAware(cc);
        
        CtField callbackField = CtField.make("private org.apache.cxf.jaxrs.client.JaxrsClientCallback " + CALLBACK_FIELD_NAME + " = null;", cc);
        cc.addField(callbackField);
        
        CtConstructor constructor = cc.getDeclaredConstructor(new CtClass[] {classPool.getCtClass("org.apache.cxf.jaxrs.client.JaxrsClientCallback")});
        insertAfter(constructor, "this." + CALLBACK_FIELD_NAME + " = $1;", false, false); //not subjected to context check
        
        CtMethod getClientCallbackMethod = CtNewMethod.make("public Object getClientCallback() { return " + CALLBACK_FIELD_NAME + "; }", cc);
        cc.addMethod(getClientCallbackMethod);
        
        // Add our interface so we can access the modified class in callbacks
        CtClass iface = classPool.getCtClass(CxfRsResponseCallback.class.getName());

        for(CtClass i : cc.getInterfaces()) {
            if (i.equals(iface)) {
                return true; // already tagged
            }
        }
        
        cc.addInterface(iface);
        return true;
    }
}