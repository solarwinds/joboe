package com.tracelytics.test.ejb3;

import javax.ejb.*;
 
@Remote
public interface TestEJB3Remote {
    public String testOp(Integer a);
    public String anotherOp(Integer a);
    public String testException();
}
