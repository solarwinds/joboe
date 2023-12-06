package com.solarwinds.joboe;

@Deprecated
/**
 * A deprecated class that performs no op and gives null for RumID
 * 
 * This is needed to maintain backward compatibility with legacy Appneta API
 * 
 * https://mvnrepository.com/artifact/com.appneta.agent.java/appneta-api
 * 
 * @author pluk
 *
 */
public class RUM {
    public static String getRumId() {
        return null;
    }
}