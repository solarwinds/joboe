package com.solarwinds.joboe.metrics;

import com.solarwinds.joboe.config.ConfigContainer;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.core.rpc.ClientException;
import com.solarwinds.joboe.core.settings.TestSettingsReader.SettingsMockupBuilder;
import com.solarwinds.joboe.sampling.SettingsArg;
import com.solarwinds.joboe.sampling.TraceDecisionUtil;
import com.solarwinds.joboe.sampling.TracingMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class MetricsMonitorTest {
	private final ConfigContainer config = new ConfigContainer();
	{
        try {
            config.putByStringValue(ConfigProperty.MONITOR_JMX_SCOPES, "{}");
        } catch (InvalidConfigException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }		
	}
	
	@Test
	public void testSingleton() throws Exception {
		MetricsMonitor monitor1 = MetricsMonitor.buildInstance(config);
		MetricsMonitor monitor2 = MetricsMonitor.buildInstance(config);
		
		//should be same instance
        assertSame(monitor1, monitor2);
	}

	@Test
	public void testUpdateInterval() throws InvalidConfigException, ClientException {
	    long defaultInterval = MetricsMonitor.DEFAULT_TIME_UNIT.getInterval(MetricsMonitor.DEFAULT_FREQUENCY);
	    MetricsMonitor monitor = MetricsMonitor.buildInstance(config);
	    assertEquals(defaultInterval, monitor.getInterval()); //no updates
	    
	    //simulate an update on metrics flush interval
	    //every 10 secs, valid
	    TestUtils.testSettingsReader.put(new SettingsMockupBuilder().withFlags(TracingMode.ALWAYS).withSampleRate(TraceDecisionUtil.SAMPLE_RESOLUTION).withSettingsArg(SettingsArg.METRIC_FLUSH_INTERVAL, 10).build());
	    assertEquals(10 * 1000, monitor.getInterval()); //should update to new metrics flush interval in millisec
	    
	    //every 40 secs, invalid, not divisible to 60 secs nor a divisor to 60 secs
	    TestUtils.testSettingsReader.put(new SettingsMockupBuilder().withFlags(TracingMode.ALWAYS).withSampleRate(TraceDecisionUtil.SAMPLE_RESOLUTION).withSettingsArg(SettingsArg.METRIC_FLUSH_INTERVAL, 40).build());
        assertEquals(10 * 1000, monitor.getInterval()); //should ignore this value, so still show last value : 10 sec
	    
	    //every 2 mins, valid
        TestUtils.testSettingsReader.put(new SettingsMockupBuilder().withFlags(TracingMode.ALWAYS).withSampleRate(TraceDecisionUtil.SAMPLE_RESOLUTION).withSettingsArg(SettingsArg.METRIC_FLUSH_INTERVAL, 120).build());
        assertEquals(120 * 1000, monitor.getInterval()); //should update to new metrics flush interval in millisec
	    
        
	    //if the arg is not found, revert back to default
        TestUtils.testSettingsReader.put(new SettingsMockupBuilder().withFlags(TracingMode.ALWAYS).withSampleRate(TraceDecisionUtil.SAMPLE_RESOLUTION).build());
        assertEquals(defaultInterval, monitor.getInterval()); //should set back to default
	}
}
