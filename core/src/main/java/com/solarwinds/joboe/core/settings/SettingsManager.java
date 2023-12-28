package com.solarwinds.joboe.core.settings;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.solarwinds.joboe.core.rpc.ClientException;
import com.solarwinds.joboe.core.rpc.RpcClientManager;
import com.solarwinds.joboe.core.rpc.RpcClientManager.OperationType;
import com.solarwinds.joboe.core.logging.Logger;
import com.solarwinds.joboe.core.logging.LoggerFactory;
import lombok.Getter;

import static com.solarwinds.joboe.core.util.HostTypeDetector.isLambda;

/**
 * Manages {@link Settings} of per jvm process. All <code>Settings</code> should be retrieved via this manager.
 * 
 * <code>initialize</code> or <code>initializeFetcher</code> must be invoked before this manager returns any valid <code>Settings</code>
 * 
 * Take note that there are 2 ways to inquire about {@link SettingsArg} of <code>Settings</code> from this manager
 * <ol>
 *  <li>Direct inquiry from <code>getSettings</code> and extract <code>SettingsArg</code> from the result</li>
 *  <li>Subscribe a {@link SettingsArgChangeListener} to this manager</li> 
 * </ol> 
 * 
 * @author pluk
 *
 */
public class SettingsManager {
    @Getter
    private static SettingsFetcher fetcher;
    private static final Map<SettingsArg<?>, Set<SettingsArgChangeListener<?>>> listeners = new ConcurrentHashMap<SettingsArg<?>, Set<SettingsArgChangeListener<?>>>();
    private static final Logger logger = LoggerFactory.getLogger();

    public static final Settings DEFAULT_SETTINGS = new com.solarwinds.joboe.core.rpc.Settings(
            (short) 0,
            "SAMPLE_START,SAMPLE_THROUGH_ALWAYS",
            System.currentTimeMillis(),
            1_000_000,
            0,
            "",
            new HashMap<String, ByteBuffer>() {{
                ByteBuffer bucketCap = ByteBuffer.allocate(Double.BYTES).order(ByteOrder.LITTLE_ENDIAN).putDouble(8);
                bucketCap.rewind();
                put("BucketCapacity", bucketCap);

                ByteBuffer bucketRate = ByteBuffer.allocate(Double.BYTES).order(ByteOrder.LITTLE_ENDIAN).putDouble(0.17);
                bucketRate.rewind();
                put("BucketRate", bucketRate);

                ByteBuffer metricFlush = ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN).putInt(60);
                metricFlush.rewind();
                put("MetricsFlushInterval", metricFlush);

                ByteBuffer triggerStrictRate = ByteBuffer.allocate(Double.BYTES).order(ByteOrder.LITTLE_ENDIAN).putDouble(0.1);
                triggerStrictRate.rewind();
                put("TriggerStrictBucketRate", triggerStrictRate);

                ByteBuffer triggerStrictCap = ByteBuffer.allocate(Double.BYTES).order(ByteOrder.LITTLE_ENDIAN).putDouble(6);
                triggerStrictCap.rewind();
                put("TriggerStrictBucketCapacity", triggerStrictCap);

                ByteBuffer triggerRelaxedCap = ByteBuffer.allocate(Double.BYTES).order(ByteOrder.LITTLE_ENDIAN).putDouble(20);
                triggerRelaxedCap.rewind();
                put("TriggerRelaxedBucketCapacity", triggerRelaxedCap);

                ByteBuffer triggerRelaxedRate = ByteBuffer.allocate(Double.BYTES).order(ByteOrder.LITTLE_ENDIAN).putDouble(1);
                triggerRelaxedRate.rewind();
                put("TriggerRelaxedBucketRate", triggerRelaxedRate);
            }});


    public static CountDownLatch initialize() throws ClientException {
        return initialize("/tmp/solarwinds-apm-settings-raw");
    }

    public static CountDownLatch initialize(String filePath) throws ClientException {
        if (isLambda()) {
            fetcher = new AwsLambdaSettingsFetcher(new FileSettingsReader(filePath), DEFAULT_SETTINGS);
        } else {
            fetcher = new PollingSettingsFetcher(new RpcSettingsReader(RpcClientManager.getClient(OperationType.SETTINGS)));
        }

        initializeFetcher(fetcher);
        return fetcher.isSettingsAvailableLatch();
    }
            
    /**
     * Initializes this manager with a provided {@link SettingsFetcher}. Direct call to this is only for internal tests
     * 
     * @param fetcher
     */
    public static void initializeFetcher(SettingsFetcher fetcher) {
        SettingsManager.fetcher = fetcher;
        fetcher.registerListener(newSettings -> {
            //figure out the difference and notify listeners
            for (Entry<SettingsArg<?>, Set<SettingsArgChangeListener<?>>> entry : listeners.entrySet()) {
                SettingsArg<?> listenedToArg = entry.getKey();
                Object newValue = newSettings != null ? newSettings.getArgValue(listenedToArg) : null;

                for (SettingsArgChangeListener<?> listener : entry.getValue()) {
                    notifyValue(listener, newValue);
                }
            }
        });
    }
    
    private static <T> void notifyValue(SettingsArgChangeListener<?> listener, Object value) {
        ((SettingsArgChangeListener<T>) listener).onValue((T) value);
    }
    
    
    /**
     * Registers a {@link SettingsArgChangeListener} to this manager to listen to changes on {@link SettingsArg}. 
     * 
     * The caller will get notified immediately once on the initial value upon calling this method
     * @param listener
     */
    public static void registerListener(SettingsArgChangeListener<?> listener) {
        Set<SettingsArgChangeListener<?>> listenersOfThisType = listeners.get(listener.getType());
        if (listenersOfThisType == null) {
            listenersOfThisType = new HashSet<SettingsArgChangeListener<?>>();
            listeners.put(listener.getType(), listenersOfThisType);
        }
        listenersOfThisType.add(listener);
        
        Settings currentSettings = getSettings();
        if (currentSettings != null) {
            notifyValue(listener, currentSettings.getArgValue(listener.getType()));
        }
    }
    
    public static void removeListener(SettingsArgChangeListener<?> listener) {
        Set<SettingsArgChangeListener<?>> listenersOfThisType = listeners.get(listener.getType());
        if (listenersOfThisType != null) {
            listenersOfThisType.remove(listener);
        }
    }
    
    /**
     * Gets the <code>Settings</code> of this current process. Might return null if no <code>Settings</code> is available yet
     * @return
     */
    public static Settings getSettings() {
        return getSettings(0, null);
    }
    
    /**
     * Gets the <code>Settings</code> of this current process. If a <code>Settings</code> is not yet available, this method will
     * block either until <code>Settings</code> is available or the timeout elapses
     *  
     * @param timeout
     * @param unit
     * @return
     */
    public static Settings getSettings(long timeout, TimeUnit unit) {
        if (fetcher != null) {
            if (timeout > 0) {
                try {
                    if (!fetcher.isSettingsAvailableLatch().await(timeout, unit)) {
                        logger.warn("Settings are not avaialable after waiting for " + timeout + " " + unit);
                        return null;
                    }
                } catch (InterruptedException e) {
                    logger.warn("Settings are not avaialable as latch await is interrupted");
                    return null;
                }
            }
            return fetcher.getSettings();
        } else {
            logger.debug("Settings are not yet available as initialization has not been completed yet");
            return null;
        }
    }

}
