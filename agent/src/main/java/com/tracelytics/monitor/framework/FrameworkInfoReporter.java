package com.tracelytics.monitor.framework;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.joboe.rpc.Client;
import com.tracelytics.joboe.rpc.Result;
import com.tracelytics.joboe.rpc.RpcClientManager;
import com.tracelytics.joboe.rpc.RpcClientManager.OperationType;
import com.tracelytics.joboe.rpc.ClientException;
import com.tracelytics.joboe.rpc.ClientLoggingCallback;
import com.tracelytics.monitor.SystemReporter;
import com.tracelytics.monitor.SystemReporterException;

/**
 * {@code SystemReporter} for Framework info
 * @author Patson Luk
 *
 */
class FrameworkInfoReporter extends SystemReporter<String, Object> {
    private Map<String, Object> frameworkKvs;
    private final Client rpcClient;
    
    private ClientLoggingCallback<Result> loggingCallback = new ClientLoggingCallback<Result>("framework init");
    
    public FrameworkInfoReporter() throws ClientException {
        this(RpcClientManager.getClient(OperationType.METRICS));
    }
    
    FrameworkInfoReporter(Client rpcClient) {
        this.rpcClient = rpcClient;
    }
    
    @Override
    public void preReportData() {
        frameworkKvs = new HashMap<String, Object>(); //reset
    }
    
    @Override
    public void postReportData() {
        if (!frameworkKvs.isEmpty()) {
            String layerName = (String) ConfigManager.getConfig(ConfigProperty.AGENT_LAYER);
            
            Map<String, Object> frameworkInfo = new HashMap<String, Object>();
            frameworkInfo.put("__Init", true);
            frameworkInfo.put("Layer", layerName);
            
            frameworkInfo.putAll(frameworkKvs);
            
            try {
                rpcClient.postStatus(Collections.singletonList(frameworkInfo), loggingCallback);
            } catch (ClientException e) {
                logger.debug("Failed reporting Framework Info : " + e.getMessage());
            }
    
            logger.debug(getClass().getSimpleName() + " reported jar with manifest info of size [" + frameworkKvs.size() + "]");
        }
    }
  

    @Override
    public void reportData(Map<String, Object> collectedData, long interval) {
        frameworkKvs.putAll(collectedData); //buffer up the framework to send them in one call for better performance
    }

}
