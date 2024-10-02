package com.tracelytics.instrumentation.http.spray;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.Module;
import com.tracelytics.instrumentation.TvContextObjectAware;
import com.tracelytics.instrumentation.config.HideParamsConfig;
import com.tracelytics.instrumentation.http.ServletInstrumentation;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;
import com.tracelytics.joboe.Metadata;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;


/**
 * Instruments the Http request/response handled by Spray can client
 * 
 * Extent entry at the render method with HttpRequest argument and extent exit at the dispatch method with HttpResponse argument
 * 
 * @author pluk
 *
 */
public class SprayClientPipelinesInstrumentation extends ClassInstrumentation {

    private static String CLASS_NAME = SprayClientPipelinesInstrumentation.class.getName();
    private static String LAYER_NAME = "spray-can-client";

    
    private static boolean hideUrlQueryParams = ConfigManager.getConfig(ConfigProperty.AGENT_HIDE_PARAMS) != null ? ((HideParamsConfig) ConfigManager.getConfig(ConfigProperty.AGENT_HIDE_PARAMS)).shouldHideParams(Module.SPRAY_CLIENT) : false;
    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(new MethodMatcher<OpType>("spray$can$client$ClientFrontend$$anon$$anon$$render", new String[] { "spray.http.HttpRequestPart", "spray.http.HttpMessage", "scala.Option" }, "void", OpType.RENDER),
            new MethodMatcher<OpType>("spray$can$client$ClientFrontend$$anon$$anon$$dispatch", new String[] { "akka.actor.ActorRef", "java.lang.Object" }, "void", OpType.DISPATCH));
    
    private enum OpType {
        RENDER, DISPATCH
    }

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes) throws Exception {
 
        for (Entry<CtMethod, OpType> methodEntry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            CtMethod method = methodEntry.getKey();
            OpType type = methodEntry.getValue();
            if (type == OpType.DISPATCH) {
                insertBefore(method, CLASS_NAME + ".dispatch($2, this);", false);
            } else if (type == OpType.RENDER) {
                insertBefore(method, 
                        "Object requestWithTvHeader = " + CLASS_NAME + ".render(context$1 != null ? context$1.remoteAddress() : null, $1, this);"
                      + "if (requestWithTvHeader instanceof spray.http.HttpRequestPart) { $1 = (spray.http.HttpRequestPart)requestWithTvHeader; }", false);
            }
            
        }
        
        addTvContextObjectAware(cc);
        return true;
    }
    
   public static Object render(InetSocketAddress remoteAddress, Object httpRequestPartObject, Object pipelinesFunction) {
       if (httpRequestPartObject instanceof SprayHttpRequest) {
           SprayHttpRequest request = (SprayHttpRequest)httpRequestPartObject; 
           Metadata context = request.getTvContext();
           if (context == null) {
               return null;
           }
           if (context.isSampled()) {
               Metadata previousContext = Context.getMetadata(); 
               Context.setMetadata(context);
               
               String host = remoteAddress.getHostString() + ":" + remoteAddress.getPort();
               
               String path = request.tvUriPath();
               String query = request.tvUriQuery();
               String httpMethod = request.tvMethod();
               
               Event event = Context.createEvent();
               
               String urlString;
               if (request.tvGetSslEncryption()) {
                   urlString = "https://" + host + path;
               } else {
                   urlString = "http://" + host + path;
               }
                
               if (!hideUrlQueryParams && query != null && !"".equals(query)) {
                   urlString += ("?" + query);
               }
               
               event.addInfo("Layer", LAYER_NAME,
                             "Label", "entry",
                             "RemoteURL", urlString,
                             "HTTPMethod", httpMethod,
                             "Spec", "rsc",
                             "IsService", Boolean.TRUE);
                             
               ClassInstrumentation.addBackTrace(event, 2, Module.SPRAY_CLIENT);
               
               event.setAsync();
               event.report();
               
               ((TvContextObjectAware)pipelinesFunction).setTvContext(Context.getMetadata());
               Object requestWithTvHeader = request.tvWithHeader(ServletInstrumentation.XTRACE_HEADER, Context.getMetadata().toHexString());
               
               Context.setMetadata(previousContext);
               return requestWithTvHeader;
           } else if (context.isValid()) {
               return request.tvWithHeader(ServletInstrumentation.XTRACE_HEADER, context.toHexString());
           }
       }
       return null;
   }
   
   
   
   public static void dispatch(Object messageObject, Object pipelinesFunction) {
       TvContextObjectAware contextObjectAware = (TvContextObjectAware) pipelinesFunction;
       if (messageObject instanceof SprayHttpResponse && contextObjectAware.getTvContext() != null && contextObjectAware.getTvContext().isSampled()) {
           SprayHttpResponse httpResponse = (SprayHttpResponse) messageObject;
           Metadata previousContext = Context.getMetadata();
           Context.setMetadata(contextObjectAware.getTvContext());
           
           Event event = Context.createEvent();
           
           // HTTP X-Trace header is only present if remote server is instrumented
           String responseXTrace = httpResponse.tvGetResponseHeader(ServletInstrumentation.XTRACE_HEADER);
           if (responseXTrace != null) {
               event.addEdge(responseXTrace);
           }

           event.addInfo("Layer", LAYER_NAME,
                         "Label", "exit",
                         "HTTPStatus", httpResponse.tvGetStatusCode());
   
           event.report();
           
           contextObjectAware.setTvContext(null);
           
           Context.setMetadata(previousContext);
       } 
       
   }

}