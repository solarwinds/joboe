package com.tracelytics.instrumentation.jms;

import com.tracelytics.instrumentation.MethodMatcher;

import java.util.ArrayList;
import java.util.List;


/**
 * The instrumentation for JMS' MessageListener interface, which is used to asynchronously process message
 * consumed by MessageConsumer (2 separate traces created - one for message consumption from the queue and another one
 * for listener processing)
 */
public class JakartaMessageListenerInstrumentation extends MessageListenerInstrumentation {
    private static final List<MethodMatcher<Object>> methodMatchers = new ArrayList<MethodMatcher<Object>>();

    static {
        methodMatchers.add(new MethodMatcher<Object>(
                "onMessage",
                new String[]{"jakarta.jms.Message"},
                "void"));
    }

    @Override
    protected String getPackagePrefix() {
        return "jakarta";
    }

    @Override
    public List<MethodMatcher<Object>> getMethodMatchers() {
        return methodMatchers;
    }
}

