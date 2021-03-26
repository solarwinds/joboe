package com.tracelytics.monitor;

import com.tracelytics.joboe.config.ConfigContainer;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;
import com.tracelytics.monitor.framework.FrameworkInfoMonitor;
import com.tracelytics.monitor.metrics.MetricsMonitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Factory that creates the {@link SystemMonitor}. Currently it only supports building a full list of {@code SystemMonitor}s based on the configurations in 
 * {@code ConfigContainer}
 * 
 * @author Patson Luk
 *
 */

public class SystemMonitorFactoryImpl implements SystemMonitorFactory {
    protected static final Logger logger = LoggerFactory.getLogger();
        
    protected ConfigContainer configs;
    
    public SystemMonitorFactoryImpl(ConfigContainer configs) {
        this.configs = configs;
    }

    /**
     * Build a list of {@code SystemMonitor}s. To attach more {@code SystemMonitor} to the monitoring system, add the construction in this method
     * 
     * @return list of {@code SystemMonitor}s based on the configurations provided by the factory constructor
     */
    public List<SystemMonitor<?, ?>> buildMonitors() {
        List<SystemMonitor<?, ?>> monitors = new ArrayList<SystemMonitor<?, ?>>();

        //Build Framework info collector
        FrameworkInfoMonitor frameworkInfoMonitor = buildFrameworkInfoMonitor();
        if (frameworkInfoMonitor != null) {
            monitors.add(frameworkInfoMonitor);
        }

        //Build Metrics monitor
        MetricsMonitor metricsMonitor = buildMetricsMonitor();
        if (metricsMonitor != null) {
            monitors.add(metricsMonitor);
        }
        
        
        return Collections.unmodifiableList(monitors);
    }

    private FrameworkInfoMonitor buildFrameworkInfoMonitor() {
        try {
            return new FrameworkInfoMonitor(configs);
        } catch (Exception e) {
            logger.warn("Failed to build Framework Monitor! " + e.getMessage());
            return null;
        }
    }
    
    protected MetricsMonitor buildMetricsMonitor() {
        try {
            return MetricsMonitor.buildInstance(configs);
        } catch (Exception e) {
            logger.warn("Failed to build Metrics Monitor! " + e.getMessage());
            return null;
        }
    }
}
