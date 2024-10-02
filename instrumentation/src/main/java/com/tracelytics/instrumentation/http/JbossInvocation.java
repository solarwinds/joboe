package com.tracelytics.instrumentation.http;

import java.lang.reflect.Method;

/**
 * Subset of org.jboss.invocation.Invocation
 *
 * We can't reference the actual class because we don't want to bundle JBoss classes with our app,
 * and if we did it probably would be incompatible with the version the customer was running. We look for the real
 * class and tag it with this interface at load time.
 */
public interface JbossInvocation {
    public Method getMethod();
}
