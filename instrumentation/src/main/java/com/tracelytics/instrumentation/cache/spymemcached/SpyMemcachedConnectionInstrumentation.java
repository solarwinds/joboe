package com.tracelytics.instrumentation.cache.spymemcached;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.TvContextObjectAware;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;
import com.tracelytics.joboe.Metadata;

/**
 * Locate adding operation to the Connection queue to mark it as the start of asynchronous extend. Also set the context to the operation callback so it can create
 * the exit event as asynchronous operation completion.
 * 
 * @author Patson Luk
 *
 */
public class SpyMemcachedConnectionInstrumentation extends ClassInstrumentation {
    private static final String CLASS_NAME = SpyMemcachedConnectionInstrumentation.class.getName();
    private static final String LAYER_NAME = "spymemcached";
    
            
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        CtMethod addOperationMethod = cc.getMethod("addOperation", "(Lnet/spy/memcached/MemcachedNode;Lnet/spy/memcached/ops/Operation;)V");
        if (shouldModify(cc, addOperationMethod)) {
            modifyAddOperationMethod(addOperationMethod);
        }
        
        CtMethod addOperationsMethod = cc.getMethod("addOperations", "(Ljava/util/Map;)V");
        
        if (shouldModify(cc, addOperationsMethod)) {
            modifyAddOperationsMethod(addOperationsMethod);
        }
        
        return true;
    }

    //for all methods except getBulk() which could issue multiple callbacks
    private void modifyAddOperationMethod(CtMethod addOperationMethod)
        throws CannotCompileException {
        insertBefore(addOperationMethod, CLASS_NAME + ".layerAsyncEntry($2, ($1 != null && $1.getSocketAddress() != null) ? $1.getSocketAddress().toString() : null, $2.getCallback());");
    }

  //for getBulk() which could issue multiple callbacks
    private void modifyAddOperationsMethod(CtMethod addOperationsMethod) throws CannotCompileException {
        String insertedCode = 
                "if ($1 != null) {" +
                "    java.util.Iterator iter= $1.entrySet().iterator();" +
        		"    while (iter.hasNext()) {" +
                "        Object entry = iter.next();" +
        		"        Object key = ((java.util.Map.Entry)entry).getKey();" +
                "        Object value = ((java.util.Map.Entry)entry).getValue();" +        		
        		"        if (key != null && key instanceof net.spy.memcached.MemcachedNode && value != null && value instanceof net.spy.memcached.ops.Operation) { " +
        		"            Object callback = ((net.spy.memcached.ops.Operation)value).getCallback();" +
        		"            net.spy.memcached.MemcachedNode node = (net.spy.memcached.MemcachedNode)key;" +
        		"            if (callback instanceof com.tracelytics.instrumentation.TvContextObjectAware) { " +
        		                 CLASS_NAME + ".layerAsyncEntry(value, node.getSocketAddress() != null ? node.getSocketAddress().toString() : null, callback);" +
        		"            }" +
        		"        }" +
        		"    }" +
        		"}";
        
        insertBefore(addOperationsMethod, insertedCode);
    }

    public static void layerAsyncEntry(Object asyncOperation, String nodeSocketAddress, Object callback) {
        //Check if the operation has already triggered an entry event, if so, do not create entry event again
        if (asyncOperation instanceof TvContextObjectAware && ((TvContextObjectAware)asyncOperation).getTvContext() != null) {
            return;
        }
        
        if (nodeSocketAddress != null) { 
            int separatorIndex = nodeSocketAddress.indexOf("/");
            if (separatorIndex != -1 && separatorIndex < nodeSocketAddress.length() - 1) {
                nodeSocketAddress = nodeSocketAddress.substring(separatorIndex + 1);
            }
        
            //report an info event such that the parent operation extent would have the host info too. This allows users of easier host info extraction w/o having
            //to expand asynchronous events
            Event event = Context.createEvent();
            event.addInfo("Layer", LAYER_NAME,
                          "Label", "info",
                          "RemoteHost", nodeSocketAddress);
            event.report();
        }
        
        Metadata currentContext = Context.getMetadata();
        
        Metadata forkedContext = new Metadata(currentContext);
        Context.setMetadata(forkedContext);
        
        Event event = Context.createEvent();
        event.addInfo("Layer", LAYER_NAME,
                      "Label", "entry",
                      "AsyncOperationClass", asyncOperation.getClass().getSimpleName());
        
        if (nodeSocketAddress != null) {
            event.addInfo("RemoteHost", nodeSocketAddress);
        }
        
        event.report();
        
        //set the current context back as we do not want other unrelated events pointing at the async Entry context by accident
        Context.setMetadata(currentContext);
        
        //set the context to the callback so the complete() method can point back to the start event
        ((TvContextObjectAware)callback).setTvContext(forkedContext);
        
        //also set the context to the operation (if it is tagged) as it might need the context if the callback is shared among different operations
        if (asyncOperation instanceof TvContextObjectAware) {
            ((TvContextObjectAware)asyncOperation).setTvContext(forkedContext);
        }
    }
        
}