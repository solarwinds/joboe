package com.tracelytics.instrumentation.http.ws;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.TvContextObjectAware;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Metadata;

/**
 * Instrumentation for axis 2 clients for both SOAP and REST requests. Take note that axis reuses it's SOAP client for REST requests. There are two options in the client -
 * org.apache.axis2.Constants.Configuration.ENABLE_REST and org.apache.axis2.Constants.Configuration.HTTP_METHOD that enable sending REST requests. However, the REST 
 * support for axis 2 is minimal, it only allows GET and POST but not DELETE and PUT.
 * 
 * 
 * @author Patson Luk
 * @see <a href="http://axis.apache.org/axis2/java/core/docs/userguide-creatingclients.html#createclients">Apache Axis 2 SOAP client</a>
 * @see <a href="http://axis.apache.org/axis2/java/core/docs/rest-ws.html#sample">Apache Axis 2 REST client</a>
 *
 */
public class AxisWsClientInstrumentation extends BaseWsClientInstrumentation {

    private static String LAYER_NAME_SOAP = "soap_client_axis2";
    private static String LAYER_NAME_REST = "rest_client_axis2";
    private static String CLASS_NAME = AxisWsClientInstrumentation.class.getName();

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {

        CtMethod sendMethod = cc.getMethod("send", "(Lorg/apache/axis2/context/MessageContext;)V");
        if (shouldModify(cc, sendMethod)) {
            insertBefore(sendMethod, "if ($1.getOptions() != null " +
            		                " && org.apache.axis2.Constants.VALUE_TRUE.equals($1.getOptions().getProperty(org.apache.axis2.Constants.Configuration.ENABLE_REST))) {" +
            		                "   String httpMethod = (String)$1.getOptions().getProperty(org.apache.axis2.Constants.Configuration.HTTP_METHOD);" +
            		                //take note that axis 2 uses POST by default (null)
                                        CLASS_NAME + ".layerEntryRest(httpMethod != null ? httpMethod : \"POST\", $1.getTo() == null ? null : $1.getTo().getAddress(), \"" + LAYER_NAME_REST + "\");" +
                                    "} else {" +
                                        CLASS_NAME + ".layerEntrySoap($1.getSoapAction(), $1.getTo() == null ? null : $1.getTo().getAddress(), \"" + LAYER_NAME_SOAP + "\");" +
                                    "}");
            
            insertBefore(sendMethod, CLASS_NAME + ".restoreContextIfAsync($1);", false); //always execute this even if the current context is not valid
                                      
            insertAfter(sendMethod, "boolean isAsync = " + CLASS_NAME + ".isAsync($1);" +
                                    "if ($1.getOptions() != null && org.apache.axis2.Constants.VALUE_TRUE.equals($1.getOptions().getProperty(org.apache.axis2.Constants.Configuration.ENABLE_REST))) {" +
                                        CLASS_NAME + ".layerExitRest(\"" + LAYER_NAME_REST + "\", null, isAsync);" +
                                    "} else {" +
                                        CLASS_NAME + ".layerExitSoap(\"" + LAYER_NAME_SOAP + "\", null, isAsync);" +
                                    "}", true);
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Restore the context tagged to the messageContext if it is an async operation. Take note that the messageContext will have context tagged to it ONLY if it is an async operation -
     * tagged in <code>org.apache.axis2.description.OutInAxisOperationClient$NonBlockingInvocationWorker</code>
     * @param messageContextObject
     */
    public static void restoreContextIfAsync(Object messageContextObject) {
        if (messageContextObject instanceof TvContextObjectAware) {
            TvContextObjectAware messageContext = (TvContextObjectAware) messageContextObject;
            if (messageContext.getTvContext() != null) {
                Context.setMetadata(new Metadata(messageContext.getTvContext())); //create a fork
            }
        }
    }
    
    /**
     * Checks whether the messageContext represents an asynchronous operation. Take note that the messageContext will have context tagged to it ONLY if it is an async operation -
     * tagged in <code>org.apache.axis2.description.OutInAxisOperationClient$NonBlockingInvocationWorker</code>
     * @param messageContextObject
     * @return
     */
    public static boolean isAsync(Object messageContextObject) {
        return (messageContextObject instanceof TvContextObjectAware) && ((TvContextObjectAware)messageContextObject).getTvContext() != null;
    }
}
