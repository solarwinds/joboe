package com.tracelytics.test.jsf;
 
import javax.faces.event.ValueChangeEvent;
 
public class MessageModel {
    public void printMessage(ValueChangeEvent e) {
        System.out.println("old value was: " + e.getOldValue());
        System.out.println("new value is: " + e.getNewValue());
    }


    public String action1() {
        System.out.println("action1");
        new Exception().printStackTrace();
        return "form2";
    }

    public String action2() {
        System.out.println("action2");
        new Exception().printStackTrace();
        return "form1";
    }
}

