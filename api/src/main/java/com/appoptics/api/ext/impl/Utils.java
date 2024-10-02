package com.appoptics.api.ext.impl;

import java.util.HashMap;
import java.util.Map;

import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;

public class Utils {
    private final static Logger logger = LoggerFactory.getLogger();
    
    private Utils() {
        
    }
    
    public static Map<String, Object> keyValuePairsToMap(Object... keyValuePairs) {
        Map<String, Object> map = new HashMap<String, Object>();
        if (keyValuePairs.length % 2 == 1) {
            logger.warn("Expect even number of arugments but found " + keyValuePairs.length + " arguments");
            return map;
        }

        for(int i = 0; i < keyValuePairs.length / 2; i++) {
            if (!(keyValuePairs[i * 2] instanceof String)) {
                logger.warn("Expect String argument at position " + (i * 2 + 1) + " but found " + keyValuePairs[i * 2]);
                continue;
            }
            map.put((String) keyValuePairs[i * 2], keyValuePairs[i * 2 + 1]);
        }
        
        return map;
    }
}
