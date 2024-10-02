package com.tracelytics.test.hibernate.model;

public class IdObject {
    protected Integer id;
    
    protected IdObject() {
    }
    
    public Integer getId() {
        return id;
    }
    
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
