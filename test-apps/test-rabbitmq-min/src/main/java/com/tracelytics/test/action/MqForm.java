package com.tracelytics.test.action;


public class MqForm {
    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_PORT = "";
    
    
    private String host = DEFAULT_HOST;
    private String port = DEFAULT_PORT;
    private String message;
    private String routingKey;
    
    public String getHost() {
        return host;
    }
    public void setHost(String host) {
        this.host = host;
    }
    public String getPort() {
        return port;
    }
    public void setPort(String port) {
        this.port = port;
    }
    
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    public String getRoutingKey() {
        return routingKey;
    }
    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }
    
}
