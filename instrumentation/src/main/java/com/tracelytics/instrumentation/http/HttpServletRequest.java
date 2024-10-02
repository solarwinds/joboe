package com.tracelytics.instrumentation.http;

/**
 * Subsets of the javax.servlet.http.HttpServletRequest
 * See http://docs.oracle.com/javaee/6/api/javax/servlet/http/HttpServletRequest.html
 * We don't use the actual interfaces because we don't know what version to use, and if we included one, it would probably conflict
 * with the app server's class loader.
 */
public interface HttpServletRequest {
    String getHeader(String header);
    String getMethod();
    String getRequestURI();
    StringBuffer getRequestURL();
    String getQueryString();
    String getRemoteHost();
    
    void setAttribute(String name, Object o);
    Object getAttribute(String name);
    
    boolean tvIsAsyncDispatch();
    
    void tvSetExtraHeader(String name, String value);
    void tvRemoveExtraHeader(String name);
}
