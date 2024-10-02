package com.tracelytics.instrumentation.solr;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;

import java.util.Arrays;
import java.util.List;

/**
 * Instrumentation on the base class RequestHandlerBase of all Solr handlers. Solr applies handler to handle each query request and admin operation
 * @author Patson Luk
 *
 */
public class SolrResponseWriterInstrumentation extends ClassInstrumentation {
    private static final String LAYER_NAME = "solr-write-response";

    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays
            .asList(new MethodMatcher<OpType>("write", new String[] { "java.io.Writer", "org.apache.solr.request.SolrQueryRequest", "org.apache.solr.response.SolrQueryResponse" }, "void", OpType.WRITE), //solr 3 +
                    new MethodMatcher<OpType>("write", new String[] { "java.io.OutputStream", "org.apache.solr.request.SolrQueryRequest", "org.apache.solr.response.SolrQueryResponse" }, "void", OpType.WRITE), //solr 3 +
                    new MethodMatcher<OpType>("write", new String[] { "java.io.Writer", "org.apache.solr.request.SolrQueryRequest", "org.apache.solr.request.SolrQueryResponse" }, "void", OpType.WRITE)); //older signature

    private enum OpType {
        WRITE
    }
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {

        for (CtMethod writeMethod : findMatchingMethods(cc, methodMatchers).keySet()) {
            insertBefore(writeMethod, getClass().getName() + ".profileEntry(this);");
            insertAfter(writeMethod, getClass().getName() + ".profileExit();", true);
        }
        return true;

    }

    public static void profileEntry(Object responseWriterObject) {
        Event event = Context.createEvent();

        event.addInfo("Label", "entry",
                      "Layer", LAYER_NAME,
                      "ResponseWriterClass", responseWriterObject.getClass().getName());

        event.report();
    }

    public static void profileExit() {
        Event event = Context.createEvent();

        event.addInfo("Label", "exit",
                "Layer", LAYER_NAME);

        event.report();
    }
}