package org.springframework.webflow.test;

import java.io.Serializable;

/**
 * A Test Model
 */
public class TestModel implements Serializable {

    private String testValue;

    public TestModel() {

    }

    public TestModel(String testValue) {
        super();
        this.testValue = testValue;
    }

    @Override
    public String toString() {
        return "Test Value : " + testValue;
    }

    public String getTestValue() {
        return testValue;
    }

    public void setTestValue(String testValue) {
        this.testValue = testValue;
    }

}
