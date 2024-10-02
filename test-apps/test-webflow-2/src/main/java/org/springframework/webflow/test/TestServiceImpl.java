package org.springframework.webflow.test;

import org.springframework.stereotype.Service;

@Service("testService")
public class TestServiceImpl implements TestService {

    public TestModel method1() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return new TestModel("testing");
    }

    public String method2() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return "next";
    }

    public String method3(String input) {
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return input;
    }

    public boolean method4(boolean input) {
        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return input;
    }

    public void methodTriggerException() {
        throw new RuntimeException("Exception created for testing purpose");
    }

}
