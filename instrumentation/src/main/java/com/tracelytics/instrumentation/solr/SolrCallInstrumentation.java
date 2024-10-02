package com.tracelytics.instrumentation.solr;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.joboe.span.impl.ScopeManager;
import com.tracelytics.joboe.span.impl.Span;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Instrumentation on <code>org.apache.solr.servlet.HttpSolrCalls</code> to capture collection and core information
 * @author Patson Luk
 *
 */
public class SolrCallInstrumentation extends ClassInstrumentation {
    public static final String LAYER_NAME = "solr";
    private static final String CLASS_NAME =  SolrCallInstrumentation.class.getName();

    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays
            .asList(new MethodMatcher<OpType>("call",  new String[] {}, "org.apache.solr.servlet.SolrDispatchFilter$Action", OpType.CALL));

    private enum OpType {
        CALL
    }
    
    @Override
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {

        for (Map.Entry<CtMethod, OpType> entry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            CtMethod method = entry.getKey();
            insertAfter(method, "if (getCore() != null) { " + CLASS_NAME + ".traceCore(getCore().getName(), getCore().getLatestSchema().getSchemaName(), getCore()); }", true);
        }

        return true;
    }

    public static void traceCore(String coreName, String schemaName, Object coreObject) {
        Span span = ScopeManager.INSTANCE.activeSpan();
        if (span != null) {
            span.setTag("CoreName", coreName);
            if (coreObject instanceof SolrCloudAwareCore) { //older version of Solr has no Cloud capability
                SolrCloudAwareCore core = (SolrCloudAwareCore) coreObject;

                if (core.tvGetShardId() != null) {
                    span.setTag("ShardId", core.tvGetShardId());
                }

                if (core.tvGetCollectionName() != null) {
                    span.setTag("CollectionName", core.tvGetCollectionName());
                }
            }
        }

        if (schemaName != null) {
            span.setTag("SchemaName", schemaName);
        }
    }
}
