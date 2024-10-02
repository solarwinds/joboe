package com.tracelytics.instrumentation.http.ws;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtField;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.http.ServletInstrumentation;


/**
 * Modifies org.apache.cxf.jaxrs.client.AbstractClient class to trace the URI, Http Method and Http response header (x-trace ID)
 * @author Patson Luk
 *
 */
public class CxfRsClientInstrumentation extends ClassInstrumentation {
    private static final String URI_FIELD_NAME = "tvUri";
    private static final String HTTP_METHOD_FIELD_NAME = "tvHttpMethod";
    private static final String X_TRACE_ID_FIELD_NAME = "tvXTraceId";

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        addFieldsAndAccessors(cc);
                        
        modifyCreateMessageMethod(cc);
        
        modifyReadBodyMethod(cc);
        
        // Add our interface so we can access the modified class in callbacks
        CtClass iface = classPool.getCtClass(CxfRsClient.class.getName());

        for(CtClass i : cc.getInterfaces()) {
            if (i.equals(iface)) {
                return true; // already tagged
            }
        }
        
        cc.addInterface(iface);
        
        
        return true;
    }

    private void modifyCreateMessageMethod(CtClass cc) throws NotFoundException, CannotCompileException {
        CtMethod createMessageMethod = cc.getDeclaredMethod("createMessage");
        
        if (shouldModify(cc, createMessageMethod)) {
            if (createMessageMethod.getReturnType() != null &&
                createMessageMethod.getReturnType().equals(classPool.get("org.apache.cxf.message.Message"))) {
                insertAfter(createMessageMethod, "if ($_ != null) {" +
                                                   URI_FIELD_NAME + " = (String)$_.get(org.apache.cxf.message.Message.ENDPOINT_ADDRESS); " +
                                                   HTTP_METHOD_FIELD_NAME + " = (String)$_.get(org.apache.cxf.message.Message.HTTP_REQUEST_METHOD); " +
                                               "}", true, false); //not subject to context check
            } else {
                logger.warn("Unexpected return type of CXF client createMessage method, expected [org.apache.cxf.message.Message] but found [" + createMessageMethod.getReturnType() + "]");
            }
        }
    }

    private void modifyReadBodyMethod(CtClass cc)
        throws NotFoundException, CannotCompileException {
        CtMethod readBodyMethod = cc.getDeclaredMethod("readBody");
        if (shouldModify(cc, readBodyMethod)) {
            if (readBodyMethod.getParameterTypes().length > 0 && readBodyMethod.getParameterTypes()[0].equals(classPool.get("javax.ws.rs.core.Response"))) {
                insertAfter(readBodyMethod, "if ($1 != null && $1.getMetadata() != null) {" +
                                                "java.util.List xTraceId = (java.util.List)$1.getMetadata().get(\"" + ServletInstrumentation.XTRACE_HEADER + "\");" +
                                                "if (xTraceId != null && !xTraceId.isEmpty()) { " + X_TRACE_ID_FIELD_NAME + ".set((String)xTraceId.get(0)); }" +
                                            "}", true, false); //not subject to context check
            } else {
                logger.warn("Unexpected param type of CXF client readBody method, expected first parameter to be [javax.ws.rs.core.Response] but actual signature is [" + readBodyMethod.getSignature() + "]"); 
            }
        }
    }

    private void addFieldsAndAccessors(CtClass cc)
        throws CannotCompileException {
        CtField uriField = CtField.make("private String " + URI_FIELD_NAME + " = null;", cc);
        cc.addField(uriField);
        
        CtField httpMethodField = CtField.make("private String " + HTTP_METHOD_FIELD_NAME + " = null;", cc);
        cc.addField(httpMethodField);
        
        CtField xTraceIdField = CtField.make("private ThreadLocal " + X_TRACE_ID_FIELD_NAME + " = new ThreadLocal();", cc);
        cc.addField(xTraceIdField);
        
        CtMethod getUriMethod = CtNewMethod.make("public String getUri() { return " + URI_FIELD_NAME + ";}", cc);
        cc.addMethod(getUriMethod);
        
        CtMethod getHttpMethodMethod = CtNewMethod.make("public String getHttpMethod() { return " + HTTP_METHOD_FIELD_NAME + ";}", cc);
        cc.addMethod(getHttpMethodMethod);
        
        CtMethod getXTraceIdMethod = CtNewMethod.make("public String getXTraceId() { return (String)" + X_TRACE_ID_FIELD_NAME + ".get();}", cc);
        cc.addMethod(getXTraceIdMethod);
    }
    
}