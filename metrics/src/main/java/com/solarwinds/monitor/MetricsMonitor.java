package com.solarwinds.monitor;

import java.util.List;

import com.solarwinds.joboe.config.ConfigContainer;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.rpc.ClientException;
import com.solarwinds.joboe.rpc.RpcClientManager;
import com.solarwinds.joboe.rpc.RpcClientManager.OperationType;
import com.solarwinds.joboe.settings.SettingsArg;
import com.solarwinds.joboe.settings.SettingsArgChangeListener;
import com.solarwinds.joboe.settings.SettingsManager;
import com.solarwinds.metrics.MetricsEntry;

/**
 * {@code SystemMonitor} for various metrics. The the list of supported metrics, please refer to {@link MetricsCollector} 
 * @author Patson Luk
 *
 */
public class MetricsMonitor extends SystemMonitorWithFrequency<MetricsCategory, List<? extends MetricsEntry<?>>> {
    private static MetricsMonitor singleton;
    
    static final int DEFAULT_FREQUENCY = 1;
    static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.PER_MINUTE;

    public MetricsMonitor(ConfigContainer configs, MetricsCollector metricsCollector) throws InvalidConfigException, ClientException {
        super(DEFAULT_TIME_UNIT, DEFAULT_FREQUENCY, metricsCollector, new MetricsReporter(RpcClientManager.getClient(OperationType.METRICS)));
        
        if (configs.containsProperty(ConfigProperty.MONITOR_METRICS_FLUSH_INTERVAL)) {
            setInterval(((Long) configs.get(ConfigProperty.MONITOR_METRICS_FLUSH_INTERVAL)) * 1000);
        }
        
        SettingsManager.registerListener(new SettingsArgChangeListener<Integer>(SettingsArg.METRIC_FLUSH_INTERVAL) {
            @Override
            public void onChange(Integer newValue) {
                try {
                    if (newValue != null) {
                        setInterval((long) newValue * 1000);
                    } else { //reset back to default
                        setFrequency(DEFAULT_TIME_UNIT, DEFAULT_FREQUENCY);
                    }
                    logger.debug("Changed metrics report interval to " + newValue);
                } catch (InvalidConfigException e) {
                    logger.warn("Cannot set interval to [" + newValue + "] : " + e.getMessage());
                }
            }
        });
    }

    public static synchronized MetricsMonitor buildInstance(ConfigContainer configs) throws InvalidConfigException, ClientException {
        return buildInstance(configs, null);
    }
    public static synchronized MetricsMonitor buildInstance(ConfigContainer configs, MetricsCollector explicitCollector) throws InvalidConfigException, ClientException {
        if (singleton == null) {
            singleton = new MetricsMonitor(configs, explicitCollector != null ? explicitCollector : new MetricsCollector(configs));
        }
        return singleton;
    }
    
    @Override
    protected String getMonitorName() {
        return "Metrics Monitor";
    }
    
    @Override
    public void close() {
        logger.debug("Flushing metrics before closing " + getMonitorName());
        executeCycle();
        
        super.close();
    }
    
}
