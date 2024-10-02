package com.tracelytics.test.hibernate.model;

public class Child extends IdObject {
    public Child() {
    }
    
    public Child(Child other) {
        this.id = other.id;
    }
    
    
}
