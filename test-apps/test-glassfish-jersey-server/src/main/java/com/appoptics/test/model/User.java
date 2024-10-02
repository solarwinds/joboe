package com.appoptics.test.model;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@XmlRootElement
public class User {
    private String username;
    
    @JsonCreator
    public User(@JsonProperty("username") String username) {
        super();
        this.username = username;
    }

    private User() {  // makes JAXB happy, will never be invoked
        this(null);
    }

    public String getUsername() {
        return username;
    }
}
