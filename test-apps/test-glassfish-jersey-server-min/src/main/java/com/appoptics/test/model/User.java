package com.appoptics.test.model;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

@XmlRootElement
public class User {
    private String username;
    
    @JsonCreator
    public User(@JsonProperty("username") String username) {
        super();
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
