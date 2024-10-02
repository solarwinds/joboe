package com.tracelytics.instrumentation.nosql.cassandra;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Future;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtField;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.Module;
import com.tracelytics.instrumentation.TvContextObjectAware;
import com.tracelytics.instrumentation.jdbc.SQLSanitizer;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;
import com.tracelytics.joboe.Metadata;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;

/**
 * Instrumentation on Cassandra's Session. All of the operations on Cassandra's Session are traced here
 * @author pluk
 *
 */
public class CassandraSessionInstrumentation extends CassandraBaseInstrumentation {
    public static final int CQL_MAX_LENGTH = 2048; //control the max length of the CQL string to avoid BufferOverFlowException
    private static final String CLASS_NAME = CassandraSessionInstrumentation.class.getName();
    
    private static SQLSanitizer sanitizer; //Sanitizer to remove literals from plain query string
    private static final ThreadLocal<Metadata> asyncContexts = new ThreadLocal<Metadata>();
    private static final Integer DEFAULT_SANITIZE_MODE = SQLSanitizer.ENABLED_AUTO;
    
    // Several common Instrumented method OpTypes
    private static enum OpType {
        EXECUTE, EXECUTE_ASYNC
    }

    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
         new MethodMatcher<OpType>("execute", new String[] { "com.datastax.driver.core.Query" }, "com.datastax.driver.core.ResultSet", OpType.EXECUTE), //Cassandra driver 1.x
         new MethodMatcher<OpType>("execute", new String[] { "com.datastax.driver.core.Statement" }, "com.datastax.driver.core.ResultSet", OpType.EXECUTE), //Cassandra driver 2.x
         new MethodMatcher<OpType>("executeAsync", new String[] { "com.datastax.driver.core.Query" }, "com.datastax.driver.core.ResultSetFuture", OpType.EXECUTE_ASYNC), //Cassandra driver 1.x
         new MethodMatcher<OpType>("executeAsync", new String[] { "com.datastax.driver.core.Statement" }, "com.datastax.driver.core.ResultSetFuture", OpType.EXECUTE_ASYNC) //Cassandra driver 2.x
    );
    
    static {
        Integer sanitizeFlagObject = (Integer) ConfigManager.getConfig(ConfigProperty.AGENT_SQL_SANITIZE);
        
        int sanitizeFlag = sanitizeFlagObject != null ? sanitizeFlagObject : DEFAULT_SANITIZE_MODE;
        sanitizer = SQLSanitizer.getSanitizer(sanitizeFlag, "com.datastax.driver.core.Statement"); 
    }
    

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        boolean hasGetLoggedKeyspaceMethod;
        try {
            cc.getMethod("getLoggedKeyspace", "()Ljava/lang/String;");
            hasGetLoggedKeyspaceMethod = true;
        } catch (NotFoundException e) {
            hasGetLoggedKeyspaceMethod = false;
        }
        
        
        for (Entry<CtMethod, OpType> matchingMethodEntry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            CtMethod method = matchingMethodEntry.getKey();
            if (shouldModify(cc, method)) {
                OpType opType = matchingMethodEntry.getValue();
                
                if (opType == OpType.EXECUTE) {
                    StringBuffer source = new StringBuffer(CLASS_NAME + ".layerEntry(this, $1, getCluster() != null && getCluster().getMetadata() != null ? getCluster().getMetadata().getClusterName() : null, ");
                    if (hasGetLoggedKeyspaceMethod) {
                        source.append("getLoggedKeyspace(), ");
                    } else {
                        source.append("null, ");
                    }
                    source.append("$1 != null ? $1.getConsistencyLevel() : null);");
                    
                    insertBefore(method, source.toString());
                    
                    addErrorReporting(method, "com.datastax.driver.core.exceptions.DriverException", className, classPool);
                    
                    insertAfter(method, CLASS_NAME + ".layerExit($_ != null && $_.getExecutionInfo() != null && $_.getExecutionInfo().getQueriedHost() != null ? $_.getExecutionInfo().getQueriedHost().getAddress() : null);", true);
                } else if (opType == OpType.EXECUTE_ASYNC) {
                    StringBuffer source = new StringBuffer(CLASS_NAME + ".layerEntryAsync(this, $1, getCluster() != null && getCluster().getMetadata() != null ? getCluster().getMetadata().getClusterName() : null, ");
                    if (hasGetLoggedKeyspaceMethod) {
                        source.append("getLoggedKeyspace(), ");
                    } else {
                        source.append("null, ");
                    }
                    source.append("$1 != null ? $1.getConsistencyLevel() : null);");
                    
                    insertBefore(method, source.toString());
                    insertAfter(method, CLASS_NAME + ".recordContext($_);", true);
                }
            }
        }
        
        cc.addField(CtField.make("private String tvKeyspace;", cc));
        cc.addMethod(CtNewMethod.make("public void tvSetKeyspace(String keyspace) { tvKeyspace = keyspace; }", cc)); 
        cc.addMethod(CtNewMethod.make("public String tvGetKeyspace() { return tvKeyspace; }", cc));
        
        tagInterface(cc, SessionWithKeyspace.class.getName());

        return true;
    }

    /**
     * Creates entry event for synchronous operation
     * @param session
     * @param statement
     * @param clusterName
     * @param keyspace
     * @param consistencyLevel
     */
    public static void layerEntry(Object session, Object statement, String clusterName, String keyspace, Object consistencyLevel) {
        if (shouldStartExtent()) { //check whether there are already active Cassandra extent to avoid nested instrumentation
            Event event = Context.createEvent();
            event.addInfo("Layer", LAYER_NAME,
                          "Label", "entry",
                          "Flavor", FLAVOR);
            
            if (statement instanceof StatementWithQueryString) {
                String query = ((StatementWithQueryString)statement).tvGetQueryString();
                
                if (sanitizer != null) { //then sanitizing is enabled
                    query = sanitizer.sanitizeQuery(query); 
                }
                if (query.length() > CQL_MAX_LENGTH) {
                    query = query.substring(0, CQL_MAX_LENGTH);
                    event.addInfo("QueryTruncated", true);
                    logger.debug("CQL Query trimmed as its length [" + query.length() + "] exceeds max [" + CQL_MAX_LENGTH + "]");
                }
                
                event.addInfo("Query", query);
            }
            
            if (statement instanceof StatementWithParameters) {
                StatementWithParameters statementWithParameters = (StatementWithParameters)statement;
                Map<Integer, Object> parameters = statementWithParameters.tvGetParameters();
                if (parameters != null && !parameters.isEmpty()) {
                    event.addInfo("QueryArgs", statementWithParameters.tvGetParameters().values().toArray());
                }
            }
            
            if (clusterName != null) {
                event.addInfo("ClusterName", clusterName);
            }
            
            if (keyspace != null) {
                event.addInfo("Keyspace", keyspace);
            } else if (session instanceof SessionWithKeyspace && ((SessionWithKeyspace)session).tvGetKeyspace() != null) { //try to look up the keyspace tagged with session
                event.addInfo("Keyspace", ((SessionWithKeyspace)session).tvGetKeyspace());
            }
            
            if (consistencyLevel != null) {
                event.addInfo("ConsistencyLevel", consistencyLevel.toString());
            }
            
            addBackTrace(event, 1, Module.CASSANDRA);
                          
            event.report();
        }
    }
    
    /**
     * Creates exit event for synchronous operations
     * @param queriedHostAddress
     */
    public static void layerExit(Object queriedHostAddress) {
        if (shouldEndExtent()) { //check whether there are already active Cassandra extent to avoid nested instrumentation
            Event event = Context.createEvent();
            event.addInfo("Layer", LAYER_NAME,
                          "Label", "exit");
            
            if (queriedHostAddress != null && (queriedHostAddress instanceof InetAddress || queriedHostAddress instanceof InetSocketAddress)) {
                String queriedHostAddressString = queriedHostAddress.toString();
                if (queriedHostAddressString.startsWith("/")) {
                    queriedHostAddressString = queriedHostAddressString.substring(1);
                }
                event.addInfo("RemoteHost", queriedHostAddressString);
            }
                        
            event.report();
        }
    }
    
    /**
     * Creates the entry event for asynchronous opeations. Take note that we put the asynchronous entry in a fork (instead of inline) due to rendering problem
     * @param session
     * @param statement
     * @param clusterName
     * @param keyspace
     */
    public static void layerEntryAsync(Object session, Object statement, String clusterName, String keyspace, Object consistencyLevel) {
        if (shouldStartExtent()) { //check whether there are already active Cassandra extent to avoid nested instrumentation
            
            //created a fork-off async entry as the inline one does not get rendered properly 
            Metadata previousContext = Context.getMetadata();
            Metadata forkedContext = new Metadata(previousContext);
            Context.setMetadata(forkedContext);
            
            
            Event event = Context.createEvent();
            event.addInfo("Layer", LAYER_NAME,
                          "Label", "entry",
                          "Flavor", FLAVOR);
            
            if (statement instanceof StatementWithQueryString) {
                String query = ((StatementWithQueryString)statement).tvGetQueryString();
                
                if (sanitizer != null) { //then sanitizing is enabled
                    query = sanitizer.sanitizeQuery(query); 
                }
                if (query.length() > CQL_MAX_LENGTH) {
                    query = query.substring(0, CQL_MAX_LENGTH);
                    event.addInfo("QueryTruncated", true);
                    logger.debug("CQL Query trimmed as its length [" + query.length() + "] exceeds max [" + CQL_MAX_LENGTH + "]");
                }
                
                event.addInfo("Query", query);
            }
            
            if (statement instanceof StatementWithParameters) {
                StatementWithParameters statementWithParameters  =  (StatementWithParameters)statement;
                if (!statementWithParameters.tvGetParameters().isEmpty()) {
                    event.addInfo("QueryArgs", statementWithParameters.tvGetParameters().values().toArray());
                }
            }
            
            if (clusterName != null) {
                event.addInfo("ClusterName", clusterName);
            }
            
            if (keyspace != null) {
                event.addInfo("Keyspace", keyspace);
            }
            
            if (consistencyLevel != null) {
                event.addInfo("ConsistencyLevel", consistencyLevel.toString());
            }
            
            addBackTrace(event, 1, Module.CASSANDRA);
                          
            event.report();
            
            asyncContexts.set(Context.getMetadata()); //store the async context so we can tag it to the Future object later on
            
          //restore the previous context so the new event will appear as a fork
            Context.setMetadata(previousContext);
        }
    }
    
    /**
     * Tags the context from layer entry to the future object such that upon completion of the future, we can restore the context and create exit event
     * @param future
     */
    public static void recordContext(Future<?> future) {
        Metadata asyncContext = asyncContexts.get(); //get the context stored previously from the layer entry event
        
        if (shouldEndExtent()) { //decrease the depth here, as the async end would not share the same thread as this
            if (future.isDone()) { //for super short operation that ends before it returns, we should still create exit event to avoid broken trace (very unlikely...)
                Event exitEvent = Context.createEvent();
                exitEvent.addInfo("Layer", LAYER_NAME,
                                  "Label", "exit");
                exitEvent.setAsync();
    
                exitEvent.report();
            } else {
                if (future instanceof TvContextObjectAware) {
                    ((TvContextObjectAware) future).setTvContext(asyncContext);
                }
            }
        }
        asyncContexts.remove(); //clear the threadLocal
    }
}