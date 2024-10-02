package com.tracelytics.test.action;

class Person {
    private int age;
    private String firstName;
    private String lastName;
    
    public Person(int age, String firstName, String lastName) {
        super();
        this.age = age;
        this.firstName = firstName;
        this.lastName = lastName;
    }
    
    public Integer getAge() {
        return age;
    }
    public String getFirstName() {
        return firstName;
    }
    public String getLastName() {
        return lastName;
    }

    @Override
    public String toString() {
        return "Person [age=" + age + ", firstName=" + firstName + ", lastName=" + lastName + "]";
    }
    
    
}
