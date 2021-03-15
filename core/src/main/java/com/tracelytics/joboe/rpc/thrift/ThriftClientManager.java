package com.tracelytics.joboe.rpc.thrift;

import com.tracelytics.joboe.rpc.Client;
import com.tracelytics.joboe.rpc.RpcClient;
import com.tracelytics.joboe.rpc.RpcClientManager;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;

/**
 * Manages creation of {@link ThriftClient}. Currently, only 2 Thrift client instances are allowed per JVM process -
 * one for Tracing operation and another one for all others
 * 
 * @author pluk
 *
 */
public class ThriftClientManager extends RpcClientManager {
    private static final Logger logger = LoggerFactory.getLogger();
    private static Client tracingRpcClient; //one instance for tracing purpose
    private static boolean clientsInitialized = false;
    private static Client nonTracingRpcClient; //one instance for everything else

    public ThriftClientManager() {
    }

    @Override
    protected Client getClientImpl(OperationType operationType, String serviceKey) {
        synchronized(this) {
            if (!clientsInitialized) {
                initializeClients(serviceKey);
                clientsInitialized = true;
            }
        }
        
        if (operationType == OperationType.TRACING || operationType == OperationType.PROFILING) {
            return tracingRpcClient;
        } else { 
            return nonTracingRpcClient;
        }
    }
    
    private void initializeClients(String serviceKey) {
        try {
            tracingRpcClient = new RpcClient(collectorHost, collectorPort, serviceKey, new ThriftClient.ThriftProtocolClientFactory(collectorCertLocation, collectorHost), RpcClient.TaskType.POST_EVENTS);
        } catch (Exception e) {
            logger.warn("Failed to initialize rpc tracing client: " + e.getMessage(), e);
        }
        
        try {
            nonTracingRpcClient = new RpcClient(collectorHost, collectorPort, serviceKey, new ThriftClient.ThriftProtocolClientFactory(collectorCertLocation, collectorHost), RpcClient.TaskType.GET_SETTINGS, RpcClient.TaskType.POST_METRICS, RpcClient.TaskType.POST_STATUS);
        } catch (Exception e) {
            logger.warn("Failed to initialize rpc non-tracing client: " + e.getMessage(), e);
        }
    }
    
    public static void closeAll() {
        if (tracingRpcClient != null) {
            tracingRpcClient.close();
        }
        
        if (nonTracingRpcClient != null) {
            nonTracingRpcClient.close();
        }
    }
}
