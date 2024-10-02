package com.tracelytics.instrumentation.http.ws;

import java.util.List;

import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.Module;
import com.tracelytics.instrumentation.TvContextObjectAware;
import com.tracelytics.instrumentation.config.HideParamsConfig;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;
import com.tracelytics.joboe.Metadata;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.util.HttpUtils;

/**
 * Base class for web service instrumentation. Provides basic methods for creating entry and exit events for both SOAP and REST
 * 
 * @author Patson Luk
 */
public abstract class BaseWsClientInstrumentation extends ClassInstrumentation {
    //Flag for whether hide query parameters as a part of the URL or not. By default false 
    protected static boolean hideUrlQuery = ConfigManager.getConfig(ConfigProperty.AGENT_HIDE_PARAMS) != null ? ((HideParamsConfig) ConfigManager.getConfig(ConfigProperty.AGENT_HIDE_PARAMS)).shouldHideParams(Module.WEB_SERVICE) : false;
  
    /**
     * Create an entry event of SOAP client following the Service Spec
     * @param soapAction
     * @param endpointAddress
     * @return x-trace id of the entry event reported
     * @see <a href="https://github.com/tracelytics/launchpad/wiki/Service-spec">Service spec</a>
     */
    
    public static Metadata layerEntrySoap(String soapAction, String endpointAddress, String layerName) {
        return layerEntrySoap(soapAction, endpointAddress, layerName, false);
    }
    
    /**
     * Create an entry event of SOAP client following the Service Spec
     * @param soapAction
     * @param endpointAddress
     * @param layerName
     * @param createFork        whether to create this entry event as a fork or not. This should be set to true for entry event of async extent, where the entry event itself is NOT reported on a separate thread - 
     * a fork is required to avoid inline async entry event
     * @return x-trace id of the entry event reported
     * @see <a href="https://github.com/tracelytics/launchpad/wiki/Service-spec">Service spec</a>
     */
    public static Metadata layerEntrySoap(String soapAction, String endpointAddress, String layerName, boolean createFork) {
        if (Context.getMetadata().isSampled()) {
            Metadata existingContext = null;
            if (createFork) { //store the existingContext
                existingContext = Context.getMetadata();
                Context.setMetadata(new Metadata(existingContext)); //create a fork
            }
            
            Event event = Context.createEvent();
            event.addInfo("Layer", layerName,
                          "Label", "entry");
            
            if (soapAction != null) {
                event.addInfo("SoapAction", soapAction);
            }
            
            if (endpointAddress != null) {
                event.addInfo("RemoteURL", hideUrlQuery ? HttpUtils.trimQueryParameters(endpointAddress) : endpointAddress);
            }
            
            event.addInfo("RemoteController", "SOAP",
                          "IsService", true,
                          "Spec", "rsc");
            
            event.report();
            
            Metadata entryMetadata = event.getMetadata();
            
            if (createFork && existingContext != null) { //restore the existing Context 
                Context.setMetadata(existingContext);
            }
            
            return entryMetadata;
        } else if (Context.getMetadata().isValid()) {
            return Context.getMetadata();
        } else {
            return null;
        } 
    }
    
    public static void layerExitSoap(String layerName) {
        layerExitSoap(layerName, (String)null);
    }
    
    public static void layerExitSoap(String layerName, List<String> responseXTraceId) {
        layerExitSoap(layerName, (responseXTraceId != null && !responseXTraceId.isEmpty()) ? responseXTraceId.get(0) : null);
    }
    
    public static void layerExitSoap(String layerName, List<String> responseXTraceId, boolean isAsync) {
        layerExitSoap(layerName, (responseXTraceId != null && !responseXTraceId.isEmpty()) ? responseXTraceId.get(0) : null, isAsync);
    }
    
    public static void layerExitSoap(String layerName, String responseXTraceId) {
        layerExitSoap(layerName, responseXTraceId, false);
    }
    
    /**
     * Creates an exit event. Take note that the x-trace id should be taken care of by the ApacheHttpClient (a bit tightly coupled now though)
     */
    public static void layerExitSoap(String layerName, String responseXTraceId, boolean isAsync) {
        Event event = Context.createEvent();

        event.addInfo("Layer", layerName,
                      "Label", "exit");
        
        if (responseXTraceId != null) {
            event.addEdge(responseXTraceId);
        }
        
        if (isAsync) {
            event.setAsync();
        }

        event.report();
    }
    
    /**
     * Create an entry event of REST client following the Service Spec
     * @param httpMethod
     * @param endpointAddress
     * @param layerName
     * @return x-trace id of the entry event reported 
     * @see <a href="https://github.com/tracelytics/launchpad/wiki/Service-spec">Service spec</a>
     */
    public static Metadata layerEntryRest(String httpMethod, String endpointAddress, String layerName) {
        return layerEntryRest(httpMethod, endpointAddress, layerName, false);
    }
    
    /**
     * Create an entry event of REST client following the Service Spec
     * @param httpMethod
     * @param endpointAddress
     * @param layerName
     * @param createFork    whether to create this entry event as a fork or not. This should be set to true for entry event of async extent, where the entry event itself is NOT reported on a separate thread - 
     * a fork is required to avoid inline async entry event
     * @return x-trace id of the entry event reported 
     * @see <a href="https://github.com/tracelytics/launchpad/wiki/Service-spec">Service spec</a> 
     */
    public static Metadata layerEntryRest(String httpMethod, String endpointAddress, String layerName, boolean createFork) {
        if (Context.getMetadata().isSampled()) {
            Metadata existingContext = null;
            if (createFork) { //store the existingContext
                existingContext = Context.getMetadata();
                Context.setMetadata(new Metadata(existingContext)); //create a fork
            }
            
            Event event = Context.createEvent();
            event.addInfo("Layer", layerName,
                          "Label", "entry");
            
            if (httpMethod != null) {
                event.addInfo("HTTPMethod", httpMethod);
            }
                   
            if (endpointAddress != null) {
                event.addInfo("RemoteURL", hideUrlQuery ? HttpUtils.trimQueryParameters(endpointAddress) : endpointAddress);
                event.addInfo("RemoteController", "REST",
                              "Spec", "rsc",
                              "IsService", true);
            }
            
            event.report();
            
            Metadata entryMetadata = event.getMetadata();
            
            if (createFork && existingContext != null) { //restore the existing Context 
                Context.setMetadata(existingContext);
            }
            
            return entryMetadata;
        } else if (Context.getMetadata().isValid()) {
            return Context.getMetadata();
        } else {
            return null;
        }
    }
    
    public static void layerExitRest(String layerName) {
        layerExitRest(layerName, (String)null);
    }
    
    public static void layerExitRest(String layerName, List<String> responseXTraceId) {
        layerExitRest(layerName, (responseXTraceId != null && !responseXTraceId.isEmpty()) ? responseXTraceId.get(0) : null);
    }
    
    public static void layerExitRest(String layerName, String responseXTraceId) {
        layerExitRest(layerName, responseXTraceId, false);
    }
    
    
    /**
     * Creates an exit event. Take note that the x-trace id should be taken care of by the ApacheHttpClient (a bit tightly coupled now though)
     */
    public static void layerExitRest(String layerName, String responseXTraceId, boolean isAsync) {
        if (Context.getMetadata().isSampled()) {
            Event event = Context.createEvent();
    
            event.addInfo("Layer", layerName,
                          "Label", "exit");
    
            if (responseXTraceId != null) {
                event.addEdge(responseXTraceId);
            }
            
            if (isAsync) {
                event.setAsync();
            }
                           
            event.report();
        }
    }
    
    public static void storeContext(Object clientCallback, Metadata metadata) {
        if (clientCallback == null) {
            logger.warn("Attempted to store context on the Soap/rest asynchronous call back but it is null. Unexpected");
        } else if (!(clientCallback instanceof TvContextObjectAware)) {
            logger.warn("Attempted to store context on the Soap/rest asynchronous call back but it is not instance of " + TvContextObjectAware.class.getName() + ". Unexpected");
        } else {
            if (metadata != null) {
                ((TvContextObjectAware) clientCallback).setTvContext(metadata);
            }
        }
    }
}
