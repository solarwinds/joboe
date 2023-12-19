package com.solarwinds.monitor.framework;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.solarwinds.monitor.SystemCollector;
import com.solarwinds.monitor.framework.FrameworkRecorder.FrameworkInfo;

/**
 * Collects Framework information extracted and recorded by {@link FrameworkRecorder}
 * @author Patson Luk
 *
 */
class FrameworkInfoCollector extends SystemCollector<String, Object> {
    @Override
    /**
     * Goes through the pending frameworks, build the KVs for reporting
     */
    protected Map<String, Object> collect() {
        Set<FrameworkInfo> pendingFrameworks = FrameworkRecorder.consumePendingFrameworks();
        Map<String, Object> map = new HashMap<String, Object>();

        for (FrameworkInfo info : pendingFrameworks) {
            String frameworkPhrase = info.getId();
            frameworkPhrase = frameworkPhrase.trim().toLowerCase().replaceAll("[\\W]", "_");
            String key = "Java." + frameworkPhrase + ".Version";
            String version = info.getVersion();
            
            map.put(key, version != null ? version : "");
        }
        
        return map;
    }
  
}