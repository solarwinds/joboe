package com.tracelytics.util;

import junit.framework.TestCase;

public class HttpUtilsTest extends TestCase {
    public void testTrimQueryParameters() {
        assertEquals("index.html", HttpUtils.trimQueryParameters("index.html"));
        assertEquals("index.html", HttpUtils.trimQueryParameters("index.html?"));
        assertEquals("index.html", HttpUtils.trimQueryParameters("index.html?a=1&b=2"));
        assertEquals(null, HttpUtils.trimQueryParameters(null));
    }
    
    
    public void testErrorStatusCode() {
        assertEquals(true, HttpUtils.isServerErrorStatusCode(500));
        assertEquals(false, HttpUtils.isServerErrorStatusCode(404));
        assertEquals(false, HttpUtils.isServerErrorStatusCode(200));
        
        assertEquals(false, HttpUtils.isClientErrorStatusCode(500));
        assertEquals(true, HttpUtils.isClientErrorStatusCode(404));
        assertEquals(false, HttpUtils.isClientErrorStatusCode(200));
        
        assertEquals(true, HttpUtils.isErrorStatusCode(500));
        assertEquals(true, HttpUtils.isErrorStatusCode(404));
        assertEquals(false, HttpUtils.isErrorStatusCode(200));
    }
}
