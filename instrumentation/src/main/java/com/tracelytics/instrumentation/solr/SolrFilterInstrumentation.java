package com.tracelytics.instrumentation.solr;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.http.FilterInstrumentation;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;

import java.util.Arrays;
import java.util.List;

/**
 * Instrumentation on SolrDispatchFilter, the controller of Solr requests.
 * @author Patson Luk
 *
 */
public class SolrFilterInstrumentation extends FilterInstrumentation {
    public static final String LAYER_NAME = "solr";

    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays
            .asList(new MethodMatcher<OpType>("execute", new String[] { "javax.servlet.http.HttpServletRequest", "org.apache.solr.request.SolrRequestHandler", "org.apache.solr.request.SolrQueryRequest", "org.apache.solr.response.SolrQueryResponse" }, "void", OpType.EXECUTE),
                    new MethodMatcher<OpType>("execute", new String[] { "javax.servlet.http.HttpServletRequest", "org.apache.solr.request.SolrRequestHandler", "org.apache.solr.request.SolrQueryRequest", "org.apache.solr.request.SolrQueryResponse" }, "void", OpType.EXECUTE));

    private enum OpType {
        EXECUTE
    }
    
    @Override
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        
        super.applyInstrumentation(cc, className, classBytes);

        String getSchemaMethodName = extractGetSchemaMethodName();
        for (CtMethod method : findMatchingMethods(cc, methodMatchers).keySet()) {
            insertBefore(method, "String tvSchemaName = null;" +
            		            "String tvCoreName = null;" +
            		            "if ($3 != null && $3.getCore() != null) {" +
            		            "    tvCoreName = $3.getCore().getName();" +
            		            (getSchemaMethodName != null ? 
            		            "    if ($3.getCore()." + getSchemaMethodName + "() != null) {" +
            		            "        tvSchemaName = $3.getCore()." + getSchemaMethodName + "().getSchemaName();" +
            		            "    }" : "") +
            		            "}" +
                                SolrFilterInstrumentation.class.getName() + ".traceCore(tvCoreName, tvSchemaName, $3 != null ? $3.getCore() : null);");
        }
        
        return true;
    }

    /**
     * Since Solr 4.4 the getSchema() method from org.apache.solr.core.SolrCore was replaced by method getLatestSchema(). In order to support older and newer version,
     * we have to check and see which method is available
     * 
     * @return the method name for Schema retrieval from SolrCore
     */
    private String extractGetSchemaMethodName() {
        CtClass solrCoreClass;
        try {
            solrCoreClass = classPool.get("org.apache.solr.core.SolrCore");
        } catch (NotFoundException e) {
            logger.warn("Cannot load the org.apache.solr.core.SolrCore CtClass");
            return null;
        }
        
        CtMethod getSchemaMethod;
        try {
            getSchemaMethod = solrCoreClass.getMethod("getSchema", "()Lorg/apache/solr/schema/IndexSchema;");
        } catch (NotFoundException e) {
            logger.debug("Cannot find the getSchema() from org.apache.solr.core.SolrCore, try the newer signature getLatestSchema instead (version 4.4+)");
            try {
                getSchemaMethod = solrCoreClass.getMethod("getLatestSchema", "()Lorg/apache/solr/schema/IndexSchema;");
            } catch (NotFoundException e1) {
                logger.warn("Tried all known signature but failed to locate method to get Scehma from org.apache.solr.core.SolrCore");
                return null;
            }
        }
        
        return getSchemaMethod.getName();
    }

    public static void traceCore(String coreName, String schemaName, Object coreObject) {
        if (coreName != null) {
            Event infoEvent = Context.createEvent();
            infoEvent.addInfo("Label", "info",
                              "Layer", LAYER_NAME,
                              "CoreName", coreName);
            
            if (coreObject instanceof SolrCloudAwareCore) { //older version of Solr has no Cloud capability
                SolrCloudAwareCore core = (SolrCloudAwareCore) coreObject;
                
                if (core.tvGetShardId() != null) {
                    infoEvent.addInfo("ShardId", core.tvGetShardId());
                }
                
                if (core.tvGetCollectionName() != null) {
                    infoEvent.addInfo("CollectionName", core.tvGetCollectionName());
                }
            } 
            
            if (schemaName != null) { 
                infoEvent.addInfo("SchemaName", schemaName);
            }
            infoEvent.report();
        }
    }
}
