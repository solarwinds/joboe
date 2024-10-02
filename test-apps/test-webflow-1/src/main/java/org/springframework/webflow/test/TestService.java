package org.springframework.webflow.test;


/**
 * A service interface for retrieving hotels and bookings from a backing repository. Also supports the ability to cancel
 * a booking.
 */
public interface TestService {

    TestModel method1();

    String method2();

    String method3(String input);

    boolean method4(boolean input);

    void methodTriggerException();
}
