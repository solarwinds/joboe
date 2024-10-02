package com.appoptics.test.model;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;


@XmlRootElement
public class Message {
    private String message;
    
    @JsonCreator
    public Message(@JsonProperty("message") String message) {
        super();
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
