package com.tracelytics.instrumentation.jms;

import com.tracelytics.instrumentation.MethodMatcher;

import java.util.ArrayList;
import java.util.List;

public class JakartaMessageProducerInstrumentation extends MessageProducerInstrumentation {
    private static final List<MethodMatcher<Type>> methodMatchers = new ArrayList<MethodMatcher<Type>>();
    static {
        methodMatchers.add(new MethodMatcher<Type>(
                "send",
                new String[]{"jakarta.jms.Message"},
                "void", Type.SEND, false));
        methodMatchers.add(new MethodMatcher<Type>(
                "send",
                new String[]{"jakarta.jms.Destination", "jakarta.jms.Message"},
                "void", Type.SEND_WITH_DEST, false));
    }

    @Override
    protected String getPackagePrefix() {
        return "jakarta";
    }

    @Override
    public List<MethodMatcher<Type>> getMethodMatchers() {
        return methodMatchers;
    }
}

