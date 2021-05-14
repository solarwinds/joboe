package com.tracelytics.instrumentation.http.ws;

import com.tracelytics.instrumentation.MethodMatcher;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.http.ServletInstrumentation;

import java.util.Arrays;
import java.util.List;

/**
 * Instrumentation on plain SOAP requests made using the java.xml.soap package
 * 
 * @see <a href="http://docs.oracle.com/javaee/5/api/javax/xml/soap/SOAPMessage.html">java.xml.soap API</a>
 * @author Patson Luk
 *
 */
public class SOAPConnectionInstrumentation extends BaseWsClientInstrumentation {

    private static String LAYER_NAME = "soap_client";
    private static String CLASS_NAME = SOAPConnectionInstrumentation.class.getName();

    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
            new MethodMatcher<OpType>("call", new String[] { "javax.xml.soap.SOAPMessage", "java.lang.Object" }, "javax.xml.soap.SOAPMessage", OpType.CALL, true),
            new MethodMatcher<OpType>("call", new String[] { "jakarta.xml.soap.SOAPMessage", "java.lang.Object" }, "jakarta.xml.soap.SOAPMessage", OpType.CALL, true)
    );

    private enum OpType {CALL}

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {

        for (CtMethod callMethod : findMatchingMethods(cc, methodMatchers).keySet()) {
            insertBefore(callMethod, CLASS_NAME + ".beforeCall($2, $1 != null ? $1.getSOAPBody() : null);");
            insertAfter(callMethod, CLASS_NAME + ".afterCall(($_ != null && $_.getMimeHeaders() != null) ? $_.getMimeHeaders().getHeader(\"" + ServletInstrumentation.XTRACE_HEADER + "\") : (String[]) null, $_, this);", true);
        }

        return true;
    }

    public static void beforeCall(Object endPoint, Element element) {
        String soapAction = null;
        if (element != null && element.getFirstChild() != null) {
            Node soapActionNode = element.getFirstChild();
            if (soapActionNode != null) {
                if (soapActionNode.getNamespaceURI() != null) {
                    String namespaceURI = soapActionNode.getNamespaceURI();
                    if (!namespaceURI.endsWith("/")) { //append a ending slash to the namespace URI, most of them should have ended in '/' already
                        namespaceURI += "/";
                    }
                    soapAction = namespaceURI + soapActionNode.getLocalName();
                } else {
                    soapAction = soapActionNode.getNodeName();
                }
            }
        }
        
        layerEntrySoap(soapAction, endPoint != null ? endPoint.toString() : null, LAYER_NAME);
    }

    public static void afterCall(String[] responseXTraceIdArray, Object ret, Object conn) {
        if (responseXTraceIdArray != null && responseXTraceIdArray.length > 0) {
            layerExitSoap(LAYER_NAME, responseXTraceIdArray[0]);
        } else {
            layerExitSoap(LAYER_NAME);
        }
    }

    
}