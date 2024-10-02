package com.tracelytics.joboe;

import com.tracelytics.agent.Agent;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.joboe.config.ProfilerSetting;
import com.tracelytics.joboe.rpc.ClientException;
import com.tracelytics.joboe.rpc.RpcClientManager;
import com.tracelytics.joboe.settings.SettingsManager;
import com.tracelytics.joboe.settings.SimpleSettingsFetcher;
import com.tracelytics.joboe.settings.TestSettingsReader;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;
import com.tracelytics.monitor.SystemMonitorController;
import com.tracelytics.profiler.Profiler;
import com.tracelytics.util.BackTraceCache;
import com.tracelytics.util.DaemonThreadFactory;
import com.tracelytics.util.HostInfoUtils;
import com.tracelytics.util.HostInfoUtils.NetworkAddressInfo;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

/**
 * A manager that maintains the startup status of the app in this current JVM. Take note that external code logic will have to flag a startup completion by calling method <code>flagStartupCompleted</code>
 *  
 * This manager also executes a set of tasks in <code>executeAfterStartup</code> after the startup completion was flagged the first time 
 * 
 * This is necessary as some operations could cause initialization problem (usually class loading) if performed too early in the app startup stage.
 * 
 * Currently, the startup completion is flagged by static initializer of <code>com.tracelytics.joboe.Context</code>
 * 
 * @author pluk
 *
 */
public class StartupManager {
    private static boolean startupCompleted = false;
    private static final Logger logger = LoggerFactory.getLogger();
    private static Future<?> backgroundInitializationFuture;
    private static boolean testingMode;

    /**
     * Returns whether the startup of the app has been completed
     * @return
     */
    public static boolean isStartupCompleted() {
        return startupCompleted;
    }


    /**
     * Flags startup of the app has been completed and it's safe to perform various instrumentation tasks
     */
    public static synchronized void flagSystemStartupCompleted() {
        flagSystemStartupCompleted(null);
    }

    /**
     * Flags startup of the app has been completed and it's safe to perform various instrumentation tasks
     *
     * @param blockingTimeInMillisec if non-null, block and wait for up to the supplied amount of millisec for the post startup tasks to finish.
     *                               No blocking if null
     */
    public static synchronized void flagSystemStartupCompleted(Long blockingTimeInMillisec) {
        if (Agent.getStatus() == Agent.AgentStatus.DISABLED) {
            return;
        }
        
        if (!startupCompleted) { //only execute these tasks once on first flag
            if (!testingMode) {
                backgroundInitializationFuture = executeAfterSystemStartup();
            } else {
                backgroundInitializationFuture = executeAfterSystemStartupInTestingMode();
            }
            
            ShutdownManager.register(); //register shutdown manager to perform cleanup tasks on shutdown
        }
        if (blockingTimeInMillisec != null) {
            try {
                backgroundInitializationFuture.get(blockingTimeInMillisec, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                logger.warn("Failed to block on flagSystemStartupCompleted : " + e.getMessage(), e);
            } catch (ExecutionException e) {
                logger.warn("Failed to block on flagSystemStartupCompleted : " + e.getMessage(), e);
            } catch (TimeoutException e) {
                logger.warn("Waited for [" + blockingTimeInMillisec + "] milliseconds but the post startup operations are still not completed");
            }
        }

        startupCompleted = true;
    }            
    
    /**
     * Flag the agent as testing mode. No background initialization will be done if flagged
     */
    public static void flagTestingMode() {
        testingMode = true;
    }
        
    private static Future<?> executeAfterSystemStartup() {
        ExecutorService service = Executors.newSingleThreadExecutor(DaemonThreadFactory.newInstance("post-startup-tasks"));
        
        Future<?> future = service.submit(new Runnable() {
            public void run() {
                try {
                    // trigger the re-transformation here, We have to wait until this point, otherwise it will trigger  https://bugs.openjdk.java.net/browse/JDK-8074299
                    Agent.checkRetransformation(); //check if any classes should be re-transformed if they were skipped during the init process
                    
                    // trigger init message reporting here. Issue due to JBoss server startup documented in https://github.com/librato/joboe/issues/542
                    Agent.reportInit();

                    // trigger init on the Settings reader
                    CountDownLatch settingsLatch = null;

                    HostInfoUtils.init(AgentHostInfoReader.INSTANCE);
                    try {
                        NetworkAddressInfo networkAddressInfo = HostInfoUtils.getNetworkAddressInfo();
                        List<String> ipAddresses = networkAddressInfo != null ? networkAddressInfo.getIpAddresses() : Collections.<String>emptyList();
                        
                        logger.debug("Detected host id: " + HostInfoUtils.getHostId() + " ip addresses: " + ipAddresses);
                        
                        settingsLatch = SettingsManager.initialize();
                    } catch (ClientException e) {
                        logger.warn("Failed to initialize RpcSettingsReader : " + e.getMessage());
                    }

                    EventImpl.setDefaultReporter(RpcEventReporter.buildReporter(RpcClientManager.OperationType.TRACING));


                    //we hook the JMX metric collection at this point to ensure it is within the context of App Class loader
                    //Doing this in the agent initialization is problematic as some app server (JBoss) assume no other code would access java core classes before them, 
                    //such a false assumption causes various classloading problems explained in
                    //https://github.com/tracelytics/joboe/pull/85 
                    //https://github.com/tracelytics/joboe/pull/79
                    //Take note that System monitor might still start in premain with SystemMonitorController.conditionalStart(), for details please refer to https://github.com/librato/joboe/pull/853
                    SystemMonitorController.start();

                    BackTraceCache.enable();
                    
                    ProfilerSetting profilerSetting = (ProfilerSetting) ConfigManager.getConfig(ConfigProperty.PROFILER);
                    if (profilerSetting != null && profilerSetting.isEnabled()) {
                        logger.debug("Profiler is enabled, local settings : " + profilerSetting);
                        Profiler.initialize(profilerSetting, RpcEventReporter.buildReporter(RpcClientManager.OperationType.PROFILING));
                    } else {
                        logger.debug("Profiler is disabled, local settings : " + profilerSetting);
                    }
                    
                    //now wait for all the latches (for now there's only one for settings)
                    try {
                        if (settingsLatch != null) {
                            settingsLatch.await();
                        }
                    } catch (InterruptedException e) {
                        logger.warn("Failed to wait for settings from RpcSettingsReader : " + e.getMessage());
                    }
                } catch (Throwable e) {
                    logger.warn("Failed post system startup operations due to : " + e.getMessage(), e);
                }
            }
        });
        
        service.shutdown();
        
        return future;
    }
    
    private static Future<TestingEnv> executeAfterSystemStartupInTestingMode() {
        ExecutorService service = Executors.newSingleThreadExecutor(DaemonThreadFactory.newInstance("post-startup-tasks"));
        
        Future<TestingEnv> future = service.submit(new Callable<TestingEnv>() {
            public TestingEnv call() {
                TestSettingsReader reader = new TestSettingsReader();
                SettingsManager.initializeFetcher(new SimpleSettingsFetcher(reader));
                HostInfoUtils.init(AgentHostInfoReader.INSTANCE);
                //replace event reporter with the testing one
                try {
                    TestReporter testTracingReporter = ReporterFactory.getInstance().buildTestReporter();

                    EventImpl.setDefaultReporter(testTracingReporter);
                    
                    TestReporter testProfilingReporter = ReporterFactory.getInstance().buildTestReporter();
                    ProfilerSetting profilerSetting = (ProfilerSetting) ConfigManager.getConfig(ConfigProperty.PROFILER);
                    if (profilerSetting != null && profilerSetting.isEnabled()) {
                        logger.debug("Profiler is enabled, local settings : " + profilerSetting);
                        Profiler.initialize(profilerSetting, testProfilingReporter);
                    } else {
                        Profiler.initialize(new ProfilerSetting(true, ProfilerSetting.DEFAULT_INTERVAL), testProfilingReporter);
                    }
                    
                    return new TestingEnv(testTracingReporter, testProfilingReporter, reader);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
                
                
                return null;
            }
        });
        
        service.shutdown();
        
        return future;
    }
    
    /**
     * Gives a Future object that completes once the agent initialization is completed
     */
    public static Future<?> isAgentReady() {
        flagSystemStartupCompleted();
        
        return backgroundInitializationFuture;
    }
}
