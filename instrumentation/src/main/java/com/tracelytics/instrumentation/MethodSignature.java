package com.tracelytics.instrumentation;

/*
 * Method signature: consists of a name, the java params/return signature, and optional instrumentation type
 * The instrumentation type is defined by the caller, so this method can be categorized. There may be
 * multiple methods that need the same instrumentation type, etc.
 */
public class MethodSignature {
    
    private String name;
    private String signature;
    private int instType;


    public MethodSignature(String name, String signature, int instType) {
        this.name = name;
        this.signature = signature;
        this.instType = instType;
    }
    
    public MethodSignature(String name, String signature) {
        this.name = name;
        this.signature = signature;
        this.instType = 0;
    }

    public String getName() {
        return name;
    }
    
    public String getSignature() {
        return signature;
    }

    public int getInstType() {
        return instType;
    }

    public String toString() {
        return "Method Name: " + name + " Signature: " + signature + " InstType: " + instType;
    }

}
