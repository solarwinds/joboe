package com.tracelytics.monitor.framework;

import com.tracelytics.joboe.config.ConfigContainer;
import com.tracelytics.joboe.config.InvalidConfigException;
import com.tracelytics.joboe.rpc.ClientException;
import com.tracelytics.monitor.SystemMonitorWithInterval;

/**
 * {@code SystemMonitor} for Framework info
 * @author Patson Luk
 *
 */
public class FrameworkInfoMonitor extends SystemMonitorWithInterval<String, Object> {
    private static final long DEFAULT_INTERVAL = 60 * 1000; //every 1 min
    
    public FrameworkInfoMonitor(ConfigContainer configs) throws ClientException {
        super(DEFAULT_INTERVAL, new FrameworkInfoCollector(), new FrameworkInfoReporter());
    }

    @Override
    protected String getMonitorName() {
        return "Framework Info Monitor";
    }


}
