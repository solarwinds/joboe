package com.solarwinds.monitor;

import com.solarwinds.metrics.measurement.SimpleMeasurementMetricsEntry;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.ArrayList;
import java.util.List;

/**
 * Sub metrics collector that collectors metrics on system level, such as CPU load and memory usage
 * @author pluk
 *
 */
class SystemMetricsCollector extends AbstractMetricsEntryCollector {
    @Override
    List<SimpleMeasurementMetricsEntry> collectMetricsEntries() throws Exception {
        List<SimpleMeasurementMetricsEntry> info = new ArrayList<SimpleMeasurementMetricsEntry>();
        
        OperatingSystemMXBean operatingMXBean = ManagementFactory.getOperatingSystemMXBean();
        
        try {
            if (operatingMXBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunMXBean = (com.sun.management.OperatingSystemMXBean) operatingMXBean;
                
                info.add(new SimpleMeasurementMetricsEntry("TotalRAM", sunMXBean.getTotalPhysicalMemorySize()));
                info.add(new SimpleMeasurementMetricsEntry("FreeRAM", sunMXBean.getFreePhysicalMemorySize()));
                
                try {
                    double loadAverage = operatingMXBean.getSystemLoadAverage();
                    if (loadAverage >= 0) {
                        info.add(new SimpleMeasurementMetricsEntry("Load1", loadAverage));
                    } else {
                        loadAverage = sunMXBean.getSystemCpuLoad();
                        if (loadAverage >= 0) { 
                            info.add(new SimpleMeasurementMetricsEntry("Load1", loadAverage));
                        }
                    }
                } catch (NoSuchMethodError e) { //jdk 1.6
                    logger.debug("Heartbeat not tracking system load average, probably running JDK 1.6 or earlier");
                }
                
                
                try {
                    double processLoad = sunMXBean.getProcessCpuLoad();
                    if (processLoad >= 0) {
                        info.add(new SimpleMeasurementMetricsEntry("ProcessLoad", processLoad));
                    }
                } catch (NoSuchMethodError e) { //jdk 1.6
                    logger.debug("Not tracking process load average, probably running JDK 1.6 or earlier");
                }
                
            }
        } catch (NoClassDefFoundError e) {
            logger.debug("com.sun.management.OperatingSystemMXBean is not found. A non Oracle/Sun JVM");
        }
        
        Runtime runtime = Runtime.getRuntime();
        info.add(new SimpleMeasurementMetricsEntry("ProcessRAM", runtime.totalMemory()));
                
        logger.debug("Collected Heartbeat: " + info);
        
        return info;
    }
}
