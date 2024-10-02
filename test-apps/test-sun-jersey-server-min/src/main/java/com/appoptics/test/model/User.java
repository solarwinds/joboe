package com.appoptics.test.model;

import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement
public class User {
    private String username;
    
    public User(String username) {
        super();
        this.username = username;
    }
    
    public User() {
        // TODO Auto-generated constructor stub
    }

    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getUsername() {
        return username;
    }
}
