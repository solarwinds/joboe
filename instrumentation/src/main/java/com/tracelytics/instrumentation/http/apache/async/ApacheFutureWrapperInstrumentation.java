package com.tracelytics.instrumentation.http.apache.async;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtConstructor;
import com.tracelytics.ext.javassist.CtField;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.ConstructorMatcher;
import com.tracelytics.instrumentation.TvContextObjectAware;
import com.tracelytics.joboe.Metadata;

/**
 * Patches the `org.apache.http.impl.nio.client.FutureWrapper` of newer Apache async client to forward our context management call
 * to underlying Future object
 *  
 * @author pluk
 *
 */
public class ApacheFutureWrapperInstrumentation extends ClassInstrumentation {
    private static String CLASS_NAME = ApacheFutureWrapperInstrumentation.class.getName();
    
    @SuppressWarnings("unchecked")
    private static List<ConstructorMatcher<Object>> constructorMatchers = Arrays.asList(
        new ConstructorMatcher<Object>(new String[]{ "java.util.concurrent.Future" })
    );
    
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        
        cc.addField(CtField.make("private " + TvContextObjectAware.class.getName() + " tvWrappedFuture;", cc));
        
        String metadataClassName = Metadata.class.getName();
        cc.addMethod(CtNewMethod.make("public void setTvContext(" + metadataClassName + " metadata) { if (tvWrappedFuture != null) { tvWrappedFuture.setTvContext(metadata); } }", cc));
        cc.addMethod(CtNewMethod.make("public " + metadataClassName + " getTvContext() { return tvWrappedFuture != null ? tvWrappedFuture.getTvContext() : null; }", cc));
        
        cc.addMethod(CtNewMethod.make("public void setTvPreviousContext(" + metadataClassName + " metadata) { if (tvWrappedFuture != null) { tvWrappedFuture.setTvPreviousContext(metadata); } }", cc));
        cc.addMethod(CtNewMethod.make("public " + metadataClassName + " getTvPreviousContext() { return tvWrappedFuture != null ? tvWrappedFuture.getTvPreviousContext() : null; }", cc));
        
        cc.addMethod(CtNewMethod.make("public void setTvFromThreadId(long threadId) { if (tvWrappedFuture != null) { tvWrappedFuture.setTvFromThreadId(threadId); } }", cc));
        cc.addMethod(CtNewMethod.make("public long getTvFromThreadId() { return tvWrappedFuture != null ? tvWrappedFuture.getTvFromThreadId() : -1; }", cc));
        
        cc.addMethod(CtNewMethod.make("public void setTvRestored(boolean restored) { if (tvWrappedFuture != null) { tvWrappedFuture.setTvRestored(restored); } }", cc));
        cc.addMethod(CtNewMethod.make("public boolean tvRestored() { return tvWrappedFuture != null ? tvWrappedFuture.tvRestored() : false; }", cc));
        
        tagInterface(cc, TvContextObjectAware.class.getName());
        
        
        for (CtConstructor constructor : findMatchingConstructors(cc, constructorMatchers).keySet()) {
            insertAfter(constructor, "tvWrappedFuture = " + CLASS_NAME + ".getWrappedFuture($1);", true, false);
        }
        
        return true;
    }
    
    public static TvContextObjectAware getWrappedFuture(Future<?> futureObject) {
        if (futureObject instanceof TvContextObjectAware) {
            return (TvContextObjectAware) futureObject;
        } else {
            logger.warn("Wrapped future passed into " + ApacheFutureWrapperInstrumentation.CLASS_NAME + " is not tagged properly");
            return null;
        }
    }    
}