package com.tracelytics.instrumentation.http.spray;

import java.util.Arrays;
import java.util.List;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtConstructor;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.ConstructorMatcher;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.TvContextObjectAware;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;
import com.tracelytics.joboe.Metadata;

/**
 * Instruments the "receive" function declared in the <code>HttpUrlConnection</code> which is an actor
 * 
 * The connection extent starts when this function is instantiated (triggered by creation of HttpUrlConnection actor)
 * The extent ends when a message is received and handled by this function
 * 
 * @author pluk
 *
 */
public class SprayClientConnectionReceiveInstrumentation extends ClassInstrumentation {

    private static String CLASS_NAME = SprayClientConnectionReceiveInstrumentation.class.getName();
    private static String LAYER_NAME = "spray-can-client-connect";

    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
            new MethodMatcher<OpType>("applyOrElse", new String[] { "java.lang.Object", "scala.Function1" }, "java.lang.Object", OpType.APPLY_OR_ELSE));
    
    @SuppressWarnings("unchecked")
    private static List<ConstructorMatcher<OpType>> contructorMatchers = Arrays.asList(
            new ConstructorMatcher<OpType>(new String[] { "spray.can.client.HttpClientConnection" }, OpType.CTOR));
    
    
    private enum OpType {
        APPLY_OR_ELSE, CTOR
    }

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes) throws Exception {
        for (CtConstructor constructor : findMatchingConstructors(cc, contructorMatchers).keySet()) {
            insertAfter(constructor, 
                    "if ($1 != null) {"
                  +     CLASS_NAME + ".startConnect($1, this);"
                  + "}", true);
        }

        for (CtMethod method : findMatchingMethods(cc, methodMatchers).keySet()) {
            insertBefore(method, CLASS_NAME + ".endConnect(this, $1);", false);
        }

        addTvContextObjectAware(cc);
        return true;
   }
    
   
    public static void startConnect(Object connectionObject, Object receiveFunctionObject) {
        SprayHttpClientConnection connection = (SprayHttpClientConnection) connectionObject;
        if (connection.tvGetRemoteAddress() != null) {
            Event event = Context.createEvent();
            event.addInfo("Layer", LAYER_NAME,
                          "Label", "entry",
                          "RemoteHost", connection.tvGetRemoteAddress().getHostString() + ":" + connection.tvGetRemoteAddress().getPort(),
                          "SSLEncrpytion", connection.tvGetSslEncryption());
            event.setAsync();
            event.report();
            
            ((TvContextObjectAware)receiveFunctionObject).setTvContext(new Metadata(Context.getMetadata()));
        }
    }
    
    
    
    public static void endConnect(Object receiveFunctionObject, Object resultMessageObject) {
        TvContextObjectAware receiveFunction = (TvContextObjectAware) receiveFunctionObject;
        if (receiveFunction.getTvContext() != null && receiveFunction.getTvContext().isSampled()) {
            Metadata previousContext = Context.getMetadata();
            Context.setMetadata(receiveFunction.getTvContext());
            
            Event event = Context.createEvent();
            
            event.addInfo("Layer", LAYER_NAME,
                          "Label", "exit",
                          "ConnectResult", resultMessageObject.toString());
                          
    
            event.report();
            
            receiveFunction.setTvContext(null); //remove the context, only exit on the first received event
            Context.setMetadata(previousContext);
        } 
    }
}