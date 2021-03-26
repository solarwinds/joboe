package com.tracelytics.joboe;

import com.tracelytics.joboe.EventImpl;
import com.tracelytics.joboe.rpc.RpcClientManager;
import com.tracelytics.joboe.rpc.thrift.ThriftClientManager;
import com.tracelytics.monitor.SystemMonitorController;

public class ShutdownManager {
    private ShutdownManager() {
        
    }
    
    static void register() {
        addShutdownHook();
    }
    
    
    private static void addShutdownHook() {
        Thread shutdownThread = new Thread("AppOptics-shutdown-hook") {
            @Override
            public void run() {
                SystemMonitorController.stop(); //stop system monitors, this might flush extra messages to reporters
                if (EventImpl.getEventReporter() != null) {
                    EventImpl.getEventReporter().close(); //close event reporter properly to give it chance to send out pending events in queue
                }
                RpcClientManager.closeAllManagers(); //close all rpc client, this should flush out all messages or stop immediately (if Thrift is not connected)
            }
        };
        
        shutdownThread.setContextClassLoader(null); //avoid memory leak warning
        
        Runtime.getRuntime().addShutdownHook(shutdownThread);
        
    }
}
