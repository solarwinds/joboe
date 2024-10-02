package com.tracelytics.test.hibernate.model;

public class Parent extends IdObject {
    private Child child;
    
    public Parent() {
    }
    
    public Child getChild() {
//        return new Child(child);
        return child;
    }
    
    public void setChild(Child child) {
        this.child = child;
    }
}