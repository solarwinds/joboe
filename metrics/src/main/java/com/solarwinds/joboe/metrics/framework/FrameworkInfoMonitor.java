package com.solarwinds.joboe.metrics.framework;

import com.solarwinds.joboe.core.rpc.ClientException;
import com.solarwinds.joboe.metrics.SystemMonitorWithInterval;

/**
 * {@code SystemMonitor} for Framework info
 * @author Patson Luk
 *
 */
public class FrameworkInfoMonitor extends SystemMonitorWithInterval<String, Object> {
    private static final long DEFAULT_INTERVAL = 60 * 1000; //every 1 min
    
    public FrameworkInfoMonitor() throws ClientException {
        super(DEFAULT_INTERVAL, new FrameworkInfoCollector(), new FrameworkInfoReporter());
    }

    @Override
    protected String getMonitorName() {
        return "Framework Info Monitor";
    }


}
