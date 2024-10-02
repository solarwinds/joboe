package com.appoptics.test.model;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@XmlRootElement
public class Message {
    private String message;
    
    @JsonCreator
    public Message(@JsonProperty("message") String message) {
        super();
        this.message = message;
    }

    private Message() {  // makes JAXB happy, will never be invoked
        this(null);
    }

    public String getMessage() {
        return message;
    }
}
