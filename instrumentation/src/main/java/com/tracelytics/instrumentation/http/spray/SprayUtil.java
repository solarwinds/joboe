package com.tracelytics.instrumentation.http.spray;

import com.tracelytics.instrumentation.Module;
import com.tracelytics.instrumentation.config.HideParamsConfig;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;

class SprayUtil {
    
    //Flag for whether hide query parameters as a part of the URL or not. By default false
    private static boolean hideUrlQueryParams = ConfigManager.getConfig(ConfigProperty.AGENT_HIDE_PARAMS) != null ? ((HideParamsConfig) ConfigManager.getConfig(ConfigProperty.AGENT_HIDE_PARAMS)).shouldHideParams(Module.SPRAY) : false;
    
    private SprayUtil() {
    }
    
    static String getUrl(String path, String query) {
        if (hideUrlQueryParams || query.isEmpty()) {
            return path;
        } else {
            return path + "?" + query; 
        } 
    }
}
