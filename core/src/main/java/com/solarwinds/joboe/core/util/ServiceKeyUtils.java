package com.solarwinds.joboe.core.util;

public class ServiceKeyUtils {
    public static final char SEPARATOR_CHARACTER = ':'; //char that separates customer key and service name within the service key
    public static final int SERVICE_NAME_MAX_LENGTH = 255;
    
    public static String maskServiceKey(String serviceKey) {
        if (serviceKey == null) {
            return serviceKey;
        }
        
        final int headCharacterCount = 4;
        final int tailCharacterCount = 4;
        final char maskCharacter = '*';
        
        int separatorIndex = serviceKey.indexOf(SEPARATOR_CHARACTER);
        String serviceName = getServiceName(serviceKey);
        String customerKey = separatorIndex != -1 ? serviceKey.substring(0, separatorIndex) : serviceKey;
        
        if (customerKey.length() > headCharacterCount + tailCharacterCount) {
            StringBuilder mask = new StringBuilder();
            for (int i = 0 ; i < customerKey.length() - (headCharacterCount + tailCharacterCount); i ++) {
                mask.append(maskCharacter);
            }
            
            customerKey = customerKey.substring(0, headCharacterCount) + mask + customerKey.substring(customerKey.length() - tailCharacterCount);
        }
        
        return serviceName != null ? customerKey + SEPARATOR_CHARACTER + serviceName : customerKey; 
    }
    
    public static String getServiceName(String serviceKey) {
        int separatorIndex = serviceKey.indexOf(SEPARATOR_CHARACTER);
        return separatorIndex != -1 ? serviceKey.substring(separatorIndex + 1) : null; 
    }
    
    public static String getApiKey(String serviceKey) {
        int separatorIndex = serviceKey.indexOf(SEPARATOR_CHARACTER);
        return separatorIndex != -1 ? serviceKey.substring(0, separatorIndex) : serviceKey;
    }

    public static String transformServiceKey(String serviceKey) {
        String serviceName = getServiceName(serviceKey);
        
        if (serviceName != null) {
            serviceName = serviceName.toLowerCase().replaceAll("\\s", "-").replaceAll("[^\\w.:_-]", "");
            if (serviceName.length() > SERVICE_NAME_MAX_LENGTH) {
                serviceName = serviceName.substring(0, SERVICE_NAME_MAX_LENGTH);
            }
            return getApiKey(serviceKey) + SEPARATOR_CHARACTER + serviceName;
        } else {
            return serviceKey;
        }
    }
}
