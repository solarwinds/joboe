package com.tracelytics.test.action;


public class MqForm {
    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_PORT = "9092";
    
    
    private String host = DEFAULT_HOST;
    private String port = DEFAULT_PORT;
    private String message;
    private String topic;
    private String messageKey;
    private String consumerGroupId;
    
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
    public String getTopic() {
        return topic;
    }
    public void setTopic(String topic) {
        this.topic = topic;
    }
    
    public String getMessageKey() {
        return messageKey;
    }
    public void setMessageKey(String messageKey) {
        this.messageKey = messageKey;
    }
    
    public String getConsumerGroupId() {
        return consumerGroupId;
    }
    public void setConsumerGroupId(String consumerGroupId) {
        this.consumerGroupId = consumerGroupId;
    }
    
}
