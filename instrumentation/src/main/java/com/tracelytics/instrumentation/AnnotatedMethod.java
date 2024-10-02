package com.tracelytics.instrumentation;

import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.bytecode.annotation.Annotation;

public class AnnotatedMethod {
    private final CtMethod method;
    private final Annotation annotation;
    
    AnnotatedMethod(CtMethod method, Annotation annotation) {
        super();
        this.method = method;
        this.annotation = annotation;
    }

    public Annotation getAnnotation() {
        return annotation;
    }
    
    public CtMethod getMethod() {
        return method;
    }

    @Override
    public String toString() {
        return "AnnotatedMethod [method=" + method + ", annotation=" + annotation + "]";
    }
}
