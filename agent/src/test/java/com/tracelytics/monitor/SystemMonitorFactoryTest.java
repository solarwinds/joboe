package com.tracelytics.monitor;

import java.util.List;

import com.tracelytics.joboe.JoboeTest;
import com.tracelytics.joboe.config.ConfigContainer;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.joboe.config.InvalidConfigException;

public class SystemMonitorFactoryTest extends JoboeTest {
    
    /**
     * Test with empty config
     * @throws InvalidConfigException 
     */
    public void testBuildCollectors1() throws InvalidConfigException {
        ConfigContainer configs = new ConfigContainer();
        
        configs.putByStringValue(ConfigProperty.MONITOR_JMX_MAX_ENTRY, "200");
        configs.putByStringValue(ConfigProperty.MONITOR_JMX_SCOPES, "{\"java.lang:type=MemoryPool,*\":[\"Usage\"],\"java.lang:type=Memory\":[\"HeapMemoryUsage\",\"NonHeapMemoryUsage\"]}");
        configs.putByStringValue(ConfigProperty.MONITOR_JMX_ENABLE, "true");
        
        SystemMonitorFactory factory = new SystemMonitorFactoryImpl(configs);
        
        List<? extends SystemMonitor<?, ?>> monitors = factory.buildMonitors();

        //FrameworkInfoMonitor and MetricsMonitor
        assertEquals(2, monitors.size());
    }
    
    /**
     * Test with config that disables the JMX monitoring
     * @throws InvalidConfigException 
     */
    public void testBuildCollectors2() throws InvalidConfigException {
        ConfigContainer configs = new ConfigContainer();
        
        configs.putByStringValue(ConfigProperty.MONITOR_JMX_MAX_ENTRY, "200");
        configs.putByStringValue(ConfigProperty.MONITOR_JMX_SCOPES, "{\"java.lang:type=MemoryPool,*\":[\"Usage\"],\"java.lang:type=Memory\":[\"HeapMemoryUsage\",\"NonHeapMemoryUsage\"]}");
        configs.putByStringValue(ConfigProperty.MONITOR_JMX_ENABLE, "false");
        
        SystemMonitorFactory factory = new SystemMonitorFactoryImpl(configs);
        
        List<? extends SystemMonitor> monitors = factory.buildMonitors();
        
        //build 2 monitors. As Metric monitor is still required for other metrics
        assertEquals(2, monitors.size());
    }
}
