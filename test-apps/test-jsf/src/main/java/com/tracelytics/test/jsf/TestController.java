package com.tracelytics.test.jsf;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

@ManagedBean(name = "testController")
@SessionScoped
public class TestController {

    private String name;
    public String getName() { new Exception().printStackTrace(); return name; }
    public void setName(String name) { new Exception().printStackTrace(); this.name = name; }

    public String action1() {
        new Exception().printStackTrace();
        System.out.println("name: " + name);
        return "testform2";
    }

    public String action2() {
        new Exception().printStackTrace();
        System.out.println("name: " + name);
        return "testform1";
    }
}


