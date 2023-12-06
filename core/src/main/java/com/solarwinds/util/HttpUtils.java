package com.solarwinds.util;

public final class HttpUtils {
    private HttpUtils() {} //no instantiation on this class is allowed
 
    public static String trimQueryParameters(String uri) {
        if (uri == null) {
            return null;
        }
        
        int querySeparatorPosition = uri.indexOf('?'); 
        if (querySeparatorPosition == -1) {
            return uri;
        } else {
            return uri.substring(0, querySeparatorPosition);
        }
    }
    
    public static boolean isServerErrorStatusCode(int statusCode) {
        return statusCode / 100 == 5; //all 5xx
    }
    
    public static boolean isClientErrorStatusCode(int statusCode) {
        return statusCode / 100 == 4; //all 4xx
    }
    
    public static boolean isErrorStatusCode(int statusCode) {
        return isServerErrorStatusCode(statusCode) || isClientErrorStatusCode(statusCode);
    }
    
}
