package com.tracelytics.test.httpclient;

public abstract class Endpoint {
    private String uri;
    Endpoint(String uri) {
        this.uri = uri;
    }
    
    public String getUri() {
        return uri;
    }
}

class AbsoluteEndpoint extends Endpoint {
    public AbsoluteEndpoint(String uri) {
        super(uri);
    }   
    
    @Override
    public String toString() {
        return "Absolution endpoint [" + getUri() + "]";
    }
}

class RelativeEndpoint extends Endpoint {
    private Integer port;
    private String host;
    private String scheme;
    
    public RelativeEndpoint(String host, Integer port, String uri, String scheme) {
        super(uri);
        this.port = port;
        this.host = host;
        this.scheme = scheme;
    }   
    
    public String getHost() {
        return host;
    }
    
    public Integer getPort() {
        return port;
    }
    
    public String getScheme() {
        return scheme;
    }
    
    @Override
    public String toString() {
        return "Relative endpoint [host : " + host + " port :  " + port + " uri : " + getUri() + "]";
    }
}
