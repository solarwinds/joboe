package com.tracelytics.instrumentation.http.ws;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtField;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.http.ServletInstrumentation;
import com.tracelytics.joboe.Metadata;

/**
 * Instrumentation of SOAP clients created by Apache CXF. Since all CXF generated clients eventually call org.apache.cxf.endpoint.Client's invoke() method, we 
 * will inject our instrumentation there.
 * 
 * All instrumented clients can be tested via the test-cxf project with uri /test-cxf/weather.do. For more details refer to the WeatherServlet of that project
 * 
 * @see <a href="http://cxf.apache.org/docs/how-do-i-develop-a-client.html">Building CXF clients</a>
 * @author Patson Luk
 *
 */
public class CxfEndpointClientInstrumentation extends BaseWsClientInstrumentation {

    private static final String CONTEXT_FIELD_NAME = "tvContext";
    private static String LAYER_NAME = "soap_client_cxf";
    private static String CLASS_NAME = CxfEndpointClientInstrumentation.class.getName();
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        CtMethod invokeMethod = cc.getMethod("invoke", "(Lorg/apache/cxf/service/model/BindingOperationInfo;[Ljava/lang/Object;Ljava/util/Map;Lorg/apache/cxf/message/Exchange;)[Ljava/lang/Object;");
        
        if (shouldModify(cc, invokeMethod)) {
            modifySynchronousInvoke(invokeMethod, cc);
            
        }
        
        //Trace the Asynchronous invoke method of CXF, Take note that we can only create the entry event here. The exit event will be traced by the ClientCallback
        CtMethod invokeAsyncMethod;
        
        try {
            invokeAsyncMethod = cc.getMethod("invoke", "(Lorg/apache/cxf/endpoint/ClientCallback;Lorg/apache/cxf/service/model/BindingOperationInfo;[Ljava/lang/Object;Ljava/util/Map;Lorg/apache/cxf/message/Exchange;)V");
        } catch (NotFoundException e) {
            logger.debug("Cannot find the signature of (Lorg/apache/cxf/endpoint/ClientCallback;Lorg/apache/cxf/service/model/BindingOperationInfo;[Ljava/lang/Object;Ljava/util/Map;Lorg/apache/cxf/message/Exchange;)V for method invoke. Try an older signature");
            invokeAsyncMethod = cc.getMethod("invoke", "(Lorg/apache/cxf/endpoint/ClientCallback;Lorg/apache/cxf/service/model/BindingOperationInfo;[Ljava/lang/Object;)V");
        }
        if (shouldModify(cc, invokeAsyncMethod)) {
            cc.addField(CtField.make("private " + Metadata.class.getName() + " " + CONTEXT_FIELD_NAME + ";", cc));
            modifyAsynchronousInvoke(invokeAsyncMethod, cc);
        }
        return true;
    }
    
    private void modifySynchronousInvoke(CtMethod invokeMethod, CtClass cc) throws CannotCompileException {
        boolean hasSoapBindingAccess;
        
        //check whether the SoapBinding is accessible, in certain setup the package might not be included, see https://github.com/tracelytics/joboe/issues/319
        try {
            classPool.get("org.apache.cxf.binding.soap.SoapBindingConstants");
            hasSoapBindingAccess = true;
        } catch (NotFoundException e) {
            logger.debug("Using CXF without access to org.apache.cxf.binding.soap.SoapBindingConstants. Probably using only cxf-bundle-jaxrs or cxf-rt-rs-client");
            hasSoapBindingAccess = false;
        }
        
        insertBefore(invokeMethod,
                                  "String endpointAddress = null;" +
                                  "String soapAction = null;" +
                                  "if ($3 != null && $3.get(org.apache.cxf.endpoint.Client.REQUEST_CONTEXT) != null) {" +
                                  "    soapAction = (String)((java.util.Map)$3.get(org.apache.cxf.endpoint.Client.REQUEST_CONTEXT)).get(" + (hasSoapBindingAccess ? "org.apache.cxf.binding.soap.SoapBindingConstants.SOAP_ACTION" : "\"SOAPAction\"") + ");" +
                                  "}" +
                                  "if ($1 != null) {" +
                                  "    if (soapAction == null && $1.getName() != null) {" + //if cannot obtain name from request context, then try the bindingOperationInfo
                                  "        soapAction = $1.getName().getNamespaceURI() != null ? ($1.getName().getNamespaceURI() + '/' + $1.getName().getLocalPart()) : $1.getName().getLocalPart();" +
                                  "    }" +
                                  "}" +
                                  "if (getEndpoint() != null && getEndpoint().getEndpointInfo() != null) {" +
                                  "    endpointAddress = getEndpoint().getEndpointInfo().getAddress();" +
                                  "}"+
                                  CLASS_NAME + ".layerEntrySoap(soapAction, endpointAddress, \"" + LAYER_NAME + "\");");
                                  
        insertAfter(invokeMethod, 
                                 "java.util.List responseXtraceId = null;" +
                                 "if ($3.get(org.apache.cxf.endpoint.Client.RESPONSE_CONTEXT) != null) {" +
                                 "     java.util.Map tvProtocolHeaders = (java.util.Map)((java.util.Map)$3.get(org.apache.cxf.endpoint.Client.RESPONSE_CONTEXT)).get(org.apache.cxf.message.Message.PROTOCOL_HEADERS);" +
                                 "     if (tvProtocolHeaders != null) {" +
                                 "         java.util.Map tvCaseInsensitiveProtocolHeaders = new java.util.TreeMap(String.CASE_INSENSITIVE_ORDER);" +
                                 "         tvCaseInsensitiveProtocolHeaders.putAll(tvProtocolHeaders);" +
                                 "         responseXtraceId = (java.util.List)tvCaseInsensitiveProtocolHeaders.get(" + ServletInstrumentation.CLASS_NAME + ".XTRACE_HEADER);" +
                                 "     }" +
                                 "}" +
                                 CLASS_NAME + ".layerExitSoap(\"" + LAYER_NAME + "\", responseXtraceId);", true);
    }

    private void modifyAsynchronousInvoke(CtMethod invokeAsyncMethod, CtClass cc) throws CannotCompileException, NotFoundException {
        CtClass mapClass = classPool.get("java.util.Map");
        boolean hasContextParam = invokeAsyncMethod.getParameterTypes().length >= 4 && invokeAsyncMethod.getParameterTypes()[3].equals(mapClass);
        boolean hasSoapBindingAccess;
        try {
            classPool.get("org.apache.cxf.binding.soap.SoapBindingConstants");
            hasSoapBindingAccess = true;
        } catch (NotFoundException e) {
            logger.debug("Using CXF without access to org.apache.cxf.binding.soap.SoapBindingConstants. Probably using only cxf-bundle-jaxrs or cxf-rt-rs-client");
            hasSoapBindingAccess = false;
        }
        
        insertBefore(invokeAsyncMethod,
                                       "String endpointAddress = null;" +
                                       "String soapAction = null;" +
                                       (hasContextParam ?         
                                       "if ($4 != null && $4.get(org.apache.cxf.endpoint.Client.REQUEST_CONTEXT) != null) {" +
                                       "    soapAction = (String)((java.util.Map)$4.get(org.apache.cxf.endpoint.Client.REQUEST_CONTEXT)).get(" + (hasSoapBindingAccess ? "org.apache.cxf.binding.soap.SoapBindingConstants.SOAP_ACTION" : "\"SOAPAction\"") + ");" +
                                       "}"
                                       : "")
                                       +
                                       "if ($2 != null) {" +
                                       "    if (soapAction == null && $2.getName() != null) {" + //if cannot obtain name from request context, then try the bindingOperationInfo
                                       "        soapAction = $2.getName().getNamespaceURI() != null ? $2.getName().getNamespaceURI() + $2.getName().getLocalPart() : $2.getName().getLocalPart();" +
                                       "    }" +
                                       "}" +
                                       "if (getEndpoint() != null && getEndpoint().getEndpointInfo() != null) {" +
                                       "    endpointAddress = getEndpoint().getEndpointInfo().getAddress();" +
                                       "}"+
                                       CONTEXT_FIELD_NAME + " = " + CLASS_NAME + ".layerEntrySoap(soapAction, endpointAddress, \"" + LAYER_NAME + "\", true);" +
                                       //add xtrace id to the request header. Take note that Async soap calls uses AsyncHTTPConduit that might not call HttpUrlConnection
                                       //therefore we add the xtrace ID here
                                       "if (" + CONTEXT_FIELD_NAME + " != null && getRequestContext() != null) {" +
                                       "     java.util.Map tvProtocolHeaders = (java.util.Map)getRequestContext().get(org.apache.cxf.message.Message.PROTOCOL_HEADERS);" +
                                       "     if (tvProtocolHeaders == null) {" +
                                       "        tvProtocolHeaders = new java.util.HashMap();" +
                                       "        getRequestContext().put(org.apache.cxf.message.Message.PROTOCOL_HEADERS, tvProtocolHeaders);" +
                                       "     }" +
                                       "     java.util.List tvXTraceId = (java.util.List)tvProtocolHeaders.get(" + ServletInstrumentation.CLASS_NAME + ".XTRACE_HEADER);" +
                                       "     if (tvXTraceId == null) {" +
                                       "        tvXTraceId = new java.util.ArrayList();" +
                                       "        tvProtocolHeaders.put(" + ServletInstrumentation.CLASS_NAME + ".XTRACE_HEADER, tvXTraceId);" +
                                       "     }" +
                                       "     tvXTraceId.add(" + CONTEXT_FIELD_NAME + ".toHexString());" +
                                       "}" +
                                       "if ($1 instanceof " + CxfClientCallback.class.getName() + ") {" +
                                       "    ((" + CxfClientCallback.class.getName() + ")$1).tvSetLayer(\"" + LAYER_NAME + "\");" +
                                       "}");
        
         insertAfter(invokeAsyncMethod, CLASS_NAME + ".storeContext($1, " + CONTEXT_FIELD_NAME + ");" +
                 "     java.util.Map tvProtocolHeaders = (java.util.Map)getRequestContext().get(org.apache.cxf.message.Message.PROTOCOL_HEADERS);" + //clean up the x-trace id otherwise same client might keep all the x-trace ID
                 "     if (tvProtocolHeaders != null) {" +
                 "         java.util.List tvXTraceId = (java.util.List)tvProtocolHeaders.get(" + ServletInstrumentation.CLASS_NAME + ".XTRACE_HEADER);" +
                 "         if (tvXTraceId != null) {" +
                 "             tvXTraceId.clear();" + 
                 "         }" +
                 "     }" +
                 CONTEXT_FIELD_NAME + " = null; ", true);
    }
    

}