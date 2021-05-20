package com.tracelytics.instrumentation.job.springscheduling;

import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.joboe.span.impl.Scope;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.joboe.span.impl.Tracer;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;

import java.util.Collections;

/**
 * Wraps the Runnable passed to the constructor of `org.springframework.scheduling.config.Task`.
 *
 * Such that we can report span when `run` is invoked on the `Runnable`.
 *
 */
public class RunnableWrapper implements Runnable {
    private final Runnable runnable;
    private static final String SPAN_NAME = "spring-scheduling";
    private static Logger logger = LoggerFactory.getLogger();
    RunnableWrapper(Runnable runnable) {
        this.runnable = runnable;
    }

    @Override
    public void run() {
        RuntimeException applicationException = null;
        Scope scope = null;
        try {
            scope = runEntry(runnable);
        } catch (Throwable e) {
            printAgentException(e);

        }

        try {
            try {
                runnable.run();
            } catch (RuntimeException e) {
                applicationException = e;
                if (scope != null) {
                    ClassInstrumentation.reportError(scope.span(), e);
                    scope.span().setTracePropertyValue(Span.TraceProperty.HAS_ERROR, true);
                }

            } finally {
                if (scope != null) {
                    scope.close();
                }
            }
        } catch (Throwable e) {
            printAgentException(e);
        }

        if (applicationException != null) { //throw this last
            throw applicationException;
        }
    }

    private static void printAgentException(Throwable throwable) {
        //Do not use logger to avoid further exception
        System.err.println("[" + Logger.APPOPTICS_TAG + "] Caught exception as below. Please take note that existing code flow should not be affected, this might only impact the instrumentation of current trace");
        System.err.println(throwable.getMessage());
        throwable.printStackTrace();
    }


    public static Scope runEntry(Runnable runnable) {
        String controller = null;
        String action = null;
        if (runnable instanceof SpringScheduledMethodRunnable) {
            Class<?> declaringClass = ((SpringScheduledMethodRunnable) runnable).getMethod().getDeclaringClass();
            controller = declaringClass != null ? declaringClass.getName() : null;
            action = ((SpringScheduledMethodRunnable) runnable).getMethod().getName();
        } else {
            controller = runnable.getClass().getName();
        }

        String resource;
        if (controller != null && action != null) {
            resource = controller + "." + action;
        } else {
            resource = (controller != null ? controller : "") + (action != null ? action : "");
        }


        Tracer.SpanBuilder builder = ClassInstrumentation.getStartTraceSpanBuilder(SPAN_NAME, Collections.EMPTY_MAP, resource, true);

        Scope scope = builder.startActive();

        if (controller != null) {
            scope.span().setTracePropertyValue(Span.TraceProperty.CONTROLLER, controller);
        }
        if (action != null) {
            scope.span().setTracePropertyValue(Span.TraceProperty.ACTION, action);
        }

        return scope;
    }
}
