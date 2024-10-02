package com.tracelytics.monitor.metrics;

import java.util.Collections;

import com.tracelytics.agent.Agent;
import com.tracelytics.joboe.JoboeTest;
import com.tracelytics.joboe.TracingMode;
import com.tracelytics.joboe.config.ConfigContainer;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.joboe.config.InvalidConfigException;
import com.tracelytics.joboe.rpc.ClientException;
import com.tracelytics.joboe.settings.SettingsArg;
import com.tracelytics.joboe.settings.SettingsManager;
import com.tracelytics.joboe.settings.SimpleSettingsFetcher;
import com.tracelytics.joboe.settings.TestSettingsReader.SettingsMockup;
import com.tracelytics.joboe.settings.TestSettingsReader.SettingsMockupBuilder;

public class MetricsMonitorTest extends JoboeTest {
	private ConfigContainer config = new ConfigContainer();
	{
        try {
            config.putByStringValue(ConfigProperty.MONITOR_JMX_SCOPES, "{}");
        } catch (InvalidConfigException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }		
	}
	
	
	public void testSingleton() throws Exception {
		MetricsMonitor monitor1 = MetricsMonitor.buildInstance(config);
		MetricsMonitor monitor2 = MetricsMonitor.buildInstance(config);
		
		//should be same instance
		assertTrue(monitor1 == monitor2);
	}
	
	public void testUpdateInterval() throws InvalidConfigException, ClientException {
	    long defaultInterval = MetricsMonitor.DEFAULT_TIME_UNIT.getInterval(MetricsMonitor.DEFAULT_FREQUENCY);
	    MetricsMonitor monitor = MetricsMonitor.buildInstance(config);
	    assertEquals(defaultInterval, monitor.getInterval()); //no updates
	    
	    //simulate an update on metrics flush interval
	    SimpleSettingsFetcher fetcher = (SimpleSettingsFetcher) SettingsManager.getFetcher();
	    //every 10 secs, valid
	    testSettingsReader.put(new SettingsMockupBuilder().withFlags(TracingMode.ALWAYS).withSampleRate(Agent.SAMPLE_RESOLUTION).withSettingsArg(SettingsArg.METRIC_FLUSH_INTERVAL, 10).build());
	    assertEquals(10 * 1000, monitor.getInterval()); //should update to new metrics flush interval in millisec
	    
	    //every 40 secs, invalid, not divisible to 60 secs nor a divisor to 60 secs
	    testSettingsReader.put(new SettingsMockupBuilder().withFlags(TracingMode.ALWAYS).withSampleRate(Agent.SAMPLE_RESOLUTION).withSettingsArg(SettingsArg.METRIC_FLUSH_INTERVAL, 40).build());
        assertEquals(10 * 1000, monitor.getInterval()); //should ignore this value, so still show last value : 10 sec
	    
	    //every 2 mins, valid
        testSettingsReader.put(new SettingsMockupBuilder().withFlags(TracingMode.ALWAYS).withSampleRate(Agent.SAMPLE_RESOLUTION).withSettingsArg(SettingsArg.METRIC_FLUSH_INTERVAL, 120).build());
        assertEquals(120 * 1000, monitor.getInterval()); //should update to new metrics flush interval in millisec
	    
        
	    //if the arg is not found, revert back to default
        testSettingsReader.put(new SettingsMockupBuilder().withFlags(TracingMode.ALWAYS).withSampleRate(Agent.SAMPLE_RESOLUTION).build());
        assertEquals(defaultInterval, monitor.getInterval()); //should set back to default
	}
}
