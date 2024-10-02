/**
 * Instruments HttpServletResponse
 * Only necessary because there is no other way to get a status code out of servlet response for some app servers (servlet spec <3.0)
 *  See http://stackoverflow.com/questions/1302072/how-can-i-get-the-http-status-code-out-of-a-servletresponse-in-a-servletfilter
 */
package com.tracelytics.instrumentation.http;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtConstructor;
import com.tracelytics.ext.javassist.CtField;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.ext.javassist.Modifier;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.ClassInstrumentation;

import java.util.Arrays;
import java.util.List;

public class ServletResponseInstrumentation extends ClassInstrumentation {
    //List of wrapper that should NOT be considered as wrapper for instrumentation
    //For example the Atmosphere one might return itself as `getResponse` which triggers infinite recursive call
    private static final List<String> excludedWrappers = Arrays.asList("org.atmosphere.cpr.AtmosphereResponse", "org.atmosphere.cpr.AtmosphereResponseImpl");

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {

        // See if class has already been instrumented:
        try {
            cc.getMethod("tlysGetStatus", "()I" );
            return true;
        } catch (NotFoundException ex) {
            // Continue....
        }
        
        // HttpServletResponseWrappers are a special case: delegate to the wrapped HTTPServletResponse.
        if (isWrapper(cc)) {
            logger.debug("Class " + className + " is a HttpServletResponseWrapper: instrumenting");
            applyWrapperInstrumentation(cc, className, classBytes);
            return true;
        }
        
        CtField f;
        CtMethod m;
        
        // Add a field so we can attach the XTrace ID to this response and get/retrieve it.
        CtClass strClass = classPool.get("java.lang.String");
        f = new CtField(strClass, TLYS_XTRACE_FIELD, cc);
        f.setModifiers(Modifier.PUBLIC);
        cc.addField(f);

        m = CtNewMethod.make( "public void tlysSetXTraceID(String tlys_str) { " + TLYS_XTRACE_FIELD + " = tlys_str; }", cc);
        cc.addMethod(m);
        
        m = CtNewMethod.make( "public String tlysGetXTraceID() { return " + TLYS_XTRACE_FIELD + "; }", cc);
        cc.addMethod(m);

        // Add a field to count how many times this request has been processed by a servlet. Servlets can have internal redirects, filters, etc.
        f = new CtField(CtClass.intType, "tlysReqCount", cc);
        cc.addField(f);

        m = CtNewMethod.make( "public int tlysReqCount() { return tlysReqCount; }", cc);
        cc.addMethod(m);

        m = CtNewMethod.make( "public int tlysIncReqCount() { tlysReqCount++; return tlysReqCount; }", cc);
        cc.addMethod(m);

        m = CtNewMethod.make( "public int tlysDecReqCount() { tlysReqCount--; return tlysReqCount; }", cc);
        cc.addMethod(m);

        
        // Add a field for framework counters: Used to count how many times a request has entered a particular framework, to avoid layers that call into themselves.
        f = CtField.make("private java.util.HashMap tlysFrameworkCounterMap = new java.util.HashMap();", cc);
        cc.addField(f);

        // Originally tried adding something like public int tlysGetFrameworkCount(String framework); ... but javassist seems to have
        // problems compiling complex code..
        m = CtNewMethod.make( "public java.util.HashMap tlysGetFrameworkCounterMap() { return tlysFrameworkCounterMap; }", cc);
        cc.addMethod(m);


        // Add a field to track current layer within framework code.
        f = CtField.make("private java.util.Stack tlysFrameworkLayerStack = new java.util.Stack();", cc);
        cc.addField(f);

        m = CtNewMethod.make( "public java.util.Stack tlysGetFrameworkLayerStack() { return tlysFrameworkLayerStack; }", cc);
        cc.addMethod(m);

        // Add our interface so we can access the modified class during layer entry/exit:
        CtClass iface = classPool.getCtClass("com.tracelytics.instrumentation.http.HttpServletResponse");
        cc.addInterface(iface);
        
        // See if class has getStatus method (servlet spec >= 3.0.) 
        try {
            CtMethod getStatusMethod = cc.getMethod("getStatus", "()I");
            
          //make sure it does have a concrete implementation of it. It might not be the case if the framework implement getStatus that returns java.lang.Integer
            if (!getStatusMethod.getDeclaringClass().isInterface()) { 
                m = CtNewMethod.make( "public int tlysGetStatus() { return getStatus(); }", cc);
                cc.addMethod(m);
                return true;
            }
        } catch (NotFoundException ex) {
            // Continue...
        } catch (CannotCompileException ex) {
            // It might have been caused by the servlet spec is 3.0 but the actual concrete class does not implements 3.0 spec
            // This is usually caused by conflict of the servlet api jar provided by the application server and the actual ServletResponse classes included in classpath jars
            // Should just continue
            logger.debug("Failed to insert [public int tlysGetStatus() { return getStatus(); }], perhaps there is disprepancy between the servlet api loaded and the actual concrete class. Trying the next option...");
            logger.debug(ex.getMessage(), ex);
        }

        // This code is only executed if we are instrumenting a servlet container that does not support
        // HttpServletResponse.getStatus()

        // Older versions of JBoss (actually, Tomcat 5.x) have a "ResponseFacade" that wraps a Response, which
        // does contain the status.
        if (cc.getName().equals("org.apache.catalina.connector.ResponseFacade")) {
            try {
                CtField response = cc.getField("response");
                CtClass responseCls = response.getType();
                if (responseCls.getName().equals("org.apache.catalina.connector.Response")) {
                    m = CtNewMethod.make( "public int tlysGetStatus() { return response.getStatus(); }", cc);
                    cc.addMethod(m);
                    return true;
                }

                // Otherwise continue and try to add our own status methods
            } catch(Exception ex) {
                logger.debug("Error instrumenting HttpServletResponse getStatus: found ResponseFacade", ex);
                // Continue...
            }
        }
        
        //Some older servlet container (websphere 7, resin 3.x) have <code>getStatusCode</code> method instead
        try {
            cc.getMethod("getStatusCode", "()I");
            
            m = CtNewMethod.make( "public int tlysGetStatus() { return getStatusCode(); }", cc);
            cc.addMethod(m);
            
            return true;
        } catch (NotFoundException e) {
            logger.debug("No getStatusCode method found in HttpServletResponse, trying next solution...");
        }

        // Last resort if we couldn't find a getStatus(): add a variable and methods to store/retrieve status
        // http://stackoverflow.com/questions/1302072/how-can-i-get-the-http-status-code-out-of-a-servletresponse-in-a-servletfilter
        // XXX: Need to check if this works in all environments
        
        f = new CtField(CtClass.intType, "tlysStatus", cc);
        cc.addField(f, CtField.Initializer.constant(200)); // default status code is 200 "OK"

        m = CtNewMethod.make( "public int tlysGetStatus() { return tlysStatus; }", cc);
        cc.addMethod(m);

        m = CtNewMethod.make( "public void tlysSetStatus(int status) { tlysStatus = $1; }", cc);
        cc.addMethod(m);

        modifyStatusMethod(cc, "setStatus", "(I)V");
        modifyStatusMethod(cc, "setStatus", "(ILjava/lang/String;)V");
        modifyStatusMethod(cc, "sendError", "(I)V");
        modifyStatusMethod(cc, "sendError", "(ILjava/lang/String;)V");

        m = cc.getMethod("reset", "()V");
        if (m.getDeclaringClass() == cc) {
            insertBefore(m, "tlysSetStatus(200);", false);
        }

        return true;
    }
    
    void modifyStatusMethod(CtClass cc, String name, String signature) {

        try {
            CtMethod m = cc.getMethod(name, signature);
            if (m.getDeclaringClass() == cc) {
                insertBefore(m, "tlysSetStatus($1);", false);
            }
        } catch(Exception ex) {
            // Errors may be normal depending on what version of the servlet API is implemented
            logger.debug("Unable to modify status method: " + name + " signature: " + signature, ex);
        }
    }
    
    private boolean isWrapper(CtClass cc)
        throws NotFoundException {
        CtClass cls = cc;

        while (cls != null) {
            if (excludedWrappers.contains(cls.getName())) {
                return false;
            }
            if (cls.getName().equals("javax.servlet.http.HttpServletResponseWrapper") || 
                cls.getName().equals("javax.servlet.http.NoBodyResponse")) { //NoBodyResponse does not implement HttpServletResponseWrapper in Servlet 2.5- see https://github.com/librato/joboe/pull/597
                return true;
            }
            cls = cls.getSuperclass();
        }

        return false;
    }

    private void applyWrapperInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        try {
            cc.getMethod("getResponse", "()Ljavax/servlet/ServletResponse;"); //check if there's a getResponse method
        } catch (NotFoundException e) { //if not, then add a getResponse method
            //make sure it has a 1 argument ctor
            try {
                CtConstructor constructor = cc.getDeclaredConstructor(new CtClass[] { classPool.getCtClass("javax.servlet.http.HttpServletResponse") }); //expect to have a 1 argument ctor
                cc.addField(CtField.make("private javax.servlet.http.HttpServletResponse tvResponse;", cc));
                insertAfter(constructor, "this.tvResponse = $1;", true, false);
                cc.addMethod(CtNewMethod.make("public javax.servlet.ServletResponse getResponse() { return tvResponse; }", cc)); 
            } catch (NotFoundException e1) {
                logger.warn("Expect a 1 argument ctor for " + className + " but failed to locate one");
                throw e1;
            }
        }
        
        CtMethod m = CtNewMethod.make( "public void tlysSetXTraceID(String tlys_str) { " + WRAPPED_RESPONSE + "tlysSetXTraceID(tlys_str); }", cc);
        cc.addMethod(m);
        
        m = CtNewMethod.make( "public String tlysGetXTraceID() { return " + WRAPPED_RESPONSE + "tlysGetXTraceID(); }", cc);
        cc.addMethod(m);

        m = CtNewMethod.make( "public int tlysReqCount() { return " + WRAPPED_RESPONSE + "tlysReqCount(); }", cc);
        cc.addMethod(m);

        m = CtNewMethod.make( "public int tlysIncReqCount() { return " + WRAPPED_RESPONSE + "tlysIncReqCount(); }", cc);
        cc.addMethod(m);

        m = CtNewMethod.make( "public int tlysDecReqCount() { return " + WRAPPED_RESPONSE + "tlysDecReqCount(); }", cc);
        cc.addMethod(m);

        m = CtNewMethod.make( "public int tlysGetStatus() { return " + WRAPPED_RESPONSE + "tlysGetStatus(); }", cc);
        cc.addMethod(m);

        m = CtNewMethod.make( "public java.util.HashMap tlysGetFrameworkCounterMap() { return " + WRAPPED_RESPONSE + "tlysGetFrameworkCounterMap(); }", cc);
        cc.addMethod(m);

        m = CtNewMethod.make( "public java.util.Stack tlysGetFrameworkLayerStack() { return " + WRAPPED_RESPONSE + "tlysGetFrameworkLayerStack(); }", cc);
        cc.addMethod(m);

        // Add our interface so we can access the modified class during layer entry/exit:
        CtClass iface = classPool.getCtClass("com.tracelytics.instrumentation.http.HttpServletResponse");
        cc.addInterface(iface);
    }

    private static String WRAPPED_RESPONSE  = "((com.tracelytics.instrumentation.http.HttpServletResponse)getResponse()).";
    private static String TLYS_XTRACE_FIELD = "tlysXTraceID";
    private static String TLYS_FRAMEWORK_LAYER_FIELD = "tlysFrameworkLayer";

}
