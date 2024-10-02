package com.tracelytics.instrumentation.nosql.cassandra;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.instrumentation.TvContextObjectAware;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;
import com.tracelytics.joboe.Metadata;

/**
 * Instruments the completion of asynchronous operations on <code>com.datastax.driver.core.ResultSetFuture</code>.
 * 
 * In older version class <code>com.datastax.driver.core.ResultSetFuture</code> extends <code>com.datastax.driver.core.SimpleFuture</code> while in newer version
 * <code>com.datastax.driver.core.ResultSetFuture</code> is an interface that extends <code>com.google.common.util.concurrent.ListenableFuture</code>
 * 
 * Therefore picking <code>com.datastax.driver.core.ResultSetFuture</code> as an instrumentation target would give correct coverage for both older and newer driver version
 * 
 * However with that decision, directly instrumentation on "set" and "setException" methods might not work as they are declared in the super class <code>com.datastax.driver.core.SimpleFuture</code>
 * in older version. To resolve that, we will first check whether the "set" and "setException" method are declared in the current class, if not, we will create an overriding method that simply
 * calls the super class method such that we can inject instrumentation on the created method. 
 * 
 * @author pluk
 *
 */
public class CassandraResultSetFutureInstrumentation extends CassandraBaseInstrumentation {
    private static final String CLASS_NAME = CassandraResultSetFutureInstrumentation.class.getName();
    
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        
        //tag the future object so a context can be set in it
        addTvContextObjectAware(cc);
        
        
        //instruments the 'set' method which indicates completion of Future
        CtMethod setMethod = cc.getMethod("set", "(Ljava/lang/Object;)Z");
        
        if (!cc.equals(setMethod.getDeclaringClass())) { //method not existed in current class, create one so we can inject our instrumentation later
            CtMethod overridingSetMethod = CtNewMethod.make("boolean set(Object o) { return super.set(o); }", cc);
            overridingSetMethod.setModifiers(setMethod.getModifiers()); //use the same modifier
            cc.addMethod(overridingSetMethod);
            
            setMethod = overridingSetMethod;
        }
        
        
        //inject instrumentation
        insertBefore(setMethod, 
                     "Object address = null;" +
                     "com.datastax.driver.core.ResultSet result = (com.datastax.driver.core.ResultSet)$1;" +
                     "if (result.getExecutionInfo() != null && result.getExecutionInfo().getQueriedHost() != null) {" +
                     "    address = result.getExecutionInfo().getQueriedHost().getAddress();" +
                     "}" +
                     CLASS_NAME + ".layerExit(this, address, null);", false);
        
        //instruments the 'setException' method which indicates completion with exception of Future
        CtMethod setExceptionMethod = cc.getMethod("setException", "(Ljava/lang/Throwable;)Z");
        
        if (!cc.equals(setExceptionMethod.getDeclaringClass())) { //method not existed in current class, create one so we can inject our instrumentation later
            CtMethod overridingSetExceptionMethod = CtNewMethod.make("boolean setException(Throwable throwable) { return super.setException(throwable); }", cc);
            overridingSetExceptionMethod.setModifiers(setExceptionMethod.getModifiers()); //use the same modifier
            cc.addMethod(overridingSetExceptionMethod);
            
            setExceptionMethod = overridingSetExceptionMethod;
        }
        
        //inject instrumentation
        insertBefore(setExceptionMethod, CLASS_NAME + ".layerExit(this, null, $1);", false);
        
        
        
        return true;
    }
    
    /**
     * Reports completion of the asynchronous operation
     * @param resultSetFutureObject
     * @param queriedHostAddress
     * @param exception
     */
    public static void layerExit(Object resultSetFutureObject, Object queriedHostAddress, Throwable exception) {
        TvContextObjectAware future = (TvContextObjectAware)resultSetFutureObject;
        
        if (future.getTvContext() != null) {
            Metadata existingContext = Context.getMetadata();
            
            Context.setMetadata(future.getTvContext());
            if (exception != null) {
                reportError(LAYER_NAME, exception);
            }
            
            Event event = Context.createEvent();
            event.addInfo("Label", "exit",
                          "Layer", LAYER_NAME);
            
            if (queriedHostAddress != null && (queriedHostAddress instanceof InetAddress || queriedHostAddress instanceof InetSocketAddress)) {
                String queriedHostAddressString = queriedHostAddress.toString();
                if (queriedHostAddressString.startsWith("/")) {
                    queriedHostAddressString = queriedHostAddressString.substring(1);
                }
                event.addInfo("RemoteHost", queriedHostAddressString);
            }
            
            event.setAsync();
            event.report();
            
            future.setTvContext(null); //clear the context in case if the future is reused
            
            Context.setMetadata(existingContext);
        }
    }

    
}