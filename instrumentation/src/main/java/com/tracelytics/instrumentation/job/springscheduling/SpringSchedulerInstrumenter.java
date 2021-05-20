package com.tracelytics.instrumentation.job.springscheduling;

import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.joboe.span.impl.Scope;
import com.tracelytics.joboe.span.impl.ScopeManager;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.joboe.span.impl.Tracer;

import java.lang.reflect.Method;
import java.util.Collections;

public class SpringSchedulerInstrumenter {
    private static final String SPAN_NAME = "spring-scheduling";
    public static void runEntry(Method method) {
        Class<?> declaringClass = method.getDeclaringClass();
        String controller = declaringClass != null ? declaringClass.getName() : null;
        String action = method.getName();

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
    }

    public static void runExit() {
        Scope scope = ScopeManager.INSTANCE.active();
        if (scope != null) {
            scope.close();
        }
    }
}
