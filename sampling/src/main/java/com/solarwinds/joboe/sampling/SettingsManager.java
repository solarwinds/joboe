package com.solarwinds.joboe.sampling;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import lombok.Getter;


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
@Getter
public final class SettingsManager {
    private static SettingsFetcher fetcher;
    private static final Map<SettingsArg<?>, Set<SettingsArgChangeListener<?>>> listeners = new ConcurrentHashMap<SettingsArg<?>, Set<SettingsArgChangeListener<?>>>();
    private static final Logger logger = LoggerFactory.getLogger();

    @Getter
    private static SamplingConfiguration samplingConfiguration = SamplingConfiguration.builder().build();

    public static CountDownLatch initialize(SettingsFetcher fetcher, SamplingConfiguration samplingConfiguration)  {
        initializeFetcher(fetcher);
        Metadata.setup(samplingConfiguration);
        SettingsManager.samplingConfiguration = samplingConfiguration;

        return fetcher.isSettingsAvailableLatch();
    }
            
    /**
     * Initializes this manager with a provided {@link SettingsFetcher}. Direct call to this is only for internal tests
     *
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
        Set<SettingsArgChangeListener<?>> listenersOfThisType = listeners.computeIfAbsent(listener.getType(), k -> new HashSet<SettingsArgChangeListener<?>>());
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
