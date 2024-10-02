package com.appoptics.test.model;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Message {
    private String message;
    
    public Message(String message) {
        super();
        this.message = message;
    }
    
    public Message() {
        // TODO Auto-generated constructor stub
    }
    
    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
