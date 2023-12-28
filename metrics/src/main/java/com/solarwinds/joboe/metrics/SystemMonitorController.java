package com.solarwinds.joboe.metrics;

import com.solarwinds.joboe.core.config.ConfigContainer;
import com.solarwinds.joboe.core.config.ConfigGroup;
import com.solarwinds.joboe.core.config.ConfigManager;
import com.solarwinds.joboe.core.logging.Logger;
import com.solarwinds.joboe.core.logging.LoggerFactory;
import com.solarwinds.joboe.core.util.DaemonThreadFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *  The controller of the System Monitor module.
 *  <p>
 *  Manages a list of {@code SystemMonitor} for system monitoring. Take note that this controller can only be started once per JVM
 *  <p>
 *  Each {@code SystemMonitor} is a self contained Runnable with it's own interval, the behavior is implemented in {@link SystemMonitor#run()}.
 *  The {@code SystemMonitor} defines {@link SystemCollector} and {@link SystemReporter} in order to collect and report data. For detailed information,
 *  please refer to documentation in {@link SystemMonitor}.
 *
 *
 *  The construction of the monitors is implemented in {@link SystemMonitorFactoryImpl}. Therefore to attach extra {@code SystemMonitor}, add the code logic in
 *  that factory.
 *
 * @see SystemMonitor
 *
 * @author Patson Luk
 *
 */
public class SystemMonitorController {
    protected static final Logger logger = LoggerFactory.getLogger();

    private static volatile ExecutorService executor;
    private static final List<SystemMonitor<?, ?>> monitors = new ArrayList<SystemMonitor<?, ?>>();
    private static final ConfigContainer configs;

    static {
        configs = ConfigManager.getConfigs(ConfigGroup.MONITOR);
    }


    /**
     * Starts the System monitoring. Should only be called once. If more than once has been called, the other calls would be ignored
     */
    public static synchronized void start() {
        startWithBuilder(() -> {
            if (configs == null) {
                logger.error("Cannot start the System Monitors! The property/config is not found!");
                return null;
            }
            return new SystemMonitorFactoryImpl(configs).buildMonitors();
        });
    }

    public static synchronized void startWithBuilder(SystemMonitorBuilder builder) {
        if (executor == null) {
            logger.debug("Start creating metrics collectors");

            List<SystemMonitor<?, ?>> monitors = builder.build();
            if (monitors == null) {
                logger.warn("Failed to build monitors. System monitors are not starting");
                return;
            }

            executor = Executors.newCachedThreadPool(DaemonThreadFactory.newInstance("system-monitor"));

            for (SystemMonitor<?, ?> monitor : monitors) {
                executor.execute(monitor);
            }

            SystemMonitorController.monitors.addAll(monitors);

            logger.debug("Finished creating System monitors");
        } else {
            logger.debug("System Monitor has already been started!");
        }
    }

    public static synchronized void stop() {
        for (SystemMonitor<?, ?> monitor : monitors) {
            monitor.close();
        }

        if (executor != null) {
            executor.shutdownNow();
        }
        executor = null;
    }
}
