package com.tracelytics.instrumentation.jms;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.Module;
import com.tracelytics.joboe.XTraceHeader;
import com.tracelytics.joboe.span.impl.Scope;
import com.tracelytics.joboe.span.impl.Span;

import java.util.*;


/**
 * The instrumentation for JMS' MessageListener interface, which is used to asynchronously process message
 * consumed by MessageConsumer (2 separate traces created - one for message consumption from the queue and another one
 * for listener processing)
 */
public class JavaxMessageListenerInstrumentation extends MessageListenerInstrumentation {
    private static final List<MethodMatcher<Object>> methodMatchers = new ArrayList<MethodMatcher<Object>>();

    static {
        methodMatchers.add(new MethodMatcher<Object>(
                "onMessage",
                new String[]{"javax.jms.Message"},
                "void"));
    }

    @Override
    protected String getPackagePrefix() {
        return "javax";
    }

    @Override
    public List<MethodMatcher<Object>> getMethodMatchers() {
        return methodMatchers;
    }
}

