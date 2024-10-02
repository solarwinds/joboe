package org.springframework.webflow.test;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * A Test Model
 */
@Entity
public class TestModel implements Serializable {

    private String testValue;
    private Long id;

    public TestModel() {

    }

    public TestModel(String testValue) {
        super();
        this.testValue = testValue;
    }

    @Id
    @GeneratedValue
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
