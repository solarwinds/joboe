package com.tracelytics.instrumentation;

import java.util.Collection;

import com.tracelytics.ext.javassist.CtBehavior;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Metadata;
import com.tracelytics.joboe.span.impl.Scope;
import com.tracelytics.joboe.span.impl.ScopeManager;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.joboe.span.impl.Span.SpanProperty;

/**
 * Patches the target class such that it can capture and store the context at the method/constructor defined in "getCatpureContextBehaviors", and when the object is executed by the method defined in "getRestoreContextMethod", the tagged context is restored as a fork.
 * 
 * A good example for target class are Tasks and Runnable objects that are executed in asynchronous manner
 * 
 * Take note that this approach only works if:
 * <ol>
 * <li>the operation creation/submission method is called within the main thread and that the target class's instance is not reused OR if reused, can be captured</li>
 * <li>the unit of "execution" (on context restoration) only process operation(s) submitted from a single thread. (ie, not a worker that get jobs from multiple threads, and execution them all within a single unit of "execution")</li>
 * </ol>
 * 
 * @author pluk
 *
 */
public abstract class ContextPropagationPatcher extends ClassInstrumentation {

    private static String CLASS_NAME = ContextPropagationPatcher.class.getName();

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes) throws Exception {
        logger.debug("Patching class [" + className + "] for context propagation");

        addTvContextObjectAware(cc);
        addSpanAware(cc);

        for (CtBehavior behavior : getCaptureContextBehaviors(cc)) {
            insertAfter(behavior, CLASS_NAME + ".captureContext(this);", true, false);
        }

        for (CtMethod method : getRestoreContextMethods(cc)) {
            insertBefore(method, CLASS_NAME + ".restoreContext(this, " + isAlwaysAsync() + ", " + isSupportMultipleRestore() + ");", false);
            insertAfter(method, CLASS_NAME + ".resetContext(this);", true, false);
        }

        return true;
    }

    /**
     * Override this to true if the span should be considered as asynchronous even if it's run on the same thread as when the methods/ctors declared in "getCaptureContextBehaviors" is invoked
     * 
     * For example event loop handling should always be considered as asynchronous even if it's running on the main thread
     * 
     * @return
     */
    protected boolean isAlwaysAsync() {
        return false;
    }

    /**
     * Override this to true if the same context object is expected to be restored multiple times.
     * 
     * By default this returns false to avoid context lingering after the first restore operation
     * 
     * @return
     */
    protected boolean isSupportMultipleRestore() {
        return false;
    }

    /**
     * Supplies the list of methods/constructors that the context should be captured at (so it can be restored in the execution thread). For example the ctor of a Runnable object
     * 
     * @param cc
     * @return
     */
    protected abstract Collection<? extends CtBehavior> getCaptureContextBehaviors(CtClass cc);

    /**
     * Supplies a list of methods that the context should be restored, this usually is the beginning of the actual execution in a different thread. For example the run method of a Runnable object
     * 
     * @param cc
     * @return
     */
    protected abstract Collection<CtMethod> getRestoreContextMethods(CtClass cc);

    /**
     * Captures current context and tags it to the Context propagation object
     * 
     * @param contextAwareObject
     */
    public static void captureContext(Object contextAwareObject) {

        if (contextAwareObject instanceof TvContextObjectAware && Context.getMetadata().isValid()) {
            ((TvContextObjectAware) contextAwareObject).setTvContext(Context.getMetadata());
            ((TvContextObjectAware) contextAwareObject).setTvFromThreadId(Thread.currentThread().getId());
            ((TvContextObjectAware) contextAwareObject).setTvRestored(false);
        }

        Span span = ScopeManager.INSTANCE.activeSpan();
        if (contextAwareObject instanceof SpanAware && span != null) {
            if (span.getSpanPropertyValue(SpanProperty.IS_SDK)) { // only propagate SDK span for now
                ((SpanAware) contextAwareObject).tvSetSpan(span);
            }
        }
    }

    public static boolean restoreContext(Object contextAwareObject) {
        return restoreContext(contextAwareObject, false, false);
    }

    /**
     * Sets the current context by restoring the context value tagged to the context aware object from its capturing method.
     * 
     * Take note that a fork event will be created if the current thread is different from the thread the runnable object was created (usually the case)
     * 
     * @param contextAwareObject
     * @param isAlwaysAsync
     *            whether the restored context should be marked as async always regardless of running thread. This is useful for handling such as event loop that it should always be considered as asynchronous even if all operations are on the same thread
     * @param supportMultipleRestore
     *            whether the same contextAwareObject can be used to restore context more than once.
     * 
     *            return whether the context is restored to this current thread
     */
    public static boolean restoreContext(Object contextAwareObject, boolean isAlwaysAsync, boolean supportMultipleRestore) {
        if (!(contextAwareObject instanceof TvContextObjectAware)) {
            return false;
        }
        // keep a reference of existing context value before setting it. Upon completion of this run method, we should reset it back to this existing context value
        // this should happen before the span propagation below as span propagation sets span context into current context
        Metadata currentContext = Context.getMetadata();
        TvContextObjectAware contextAware = (TvContextObjectAware) contextAwareObject;

        //Context propagation using the newer OpenTracing span/scope model
        if (contextAwareObject instanceof SpanAware) {
            Span restoringSpan = ((SpanAware) contextAwareObject).tvGetSpan();

            if (restoringSpan != null && restoringSpan.getSpanPropertyValue(SpanProperty.IS_SDK)) { // only propagate SDK span for now
                //check if the scope is created off a span from a different thread - ie all the child span creates off this scope should be flagged as async
                boolean isAsyncPropagation = isAlwaysAsync || Thread.currentThread().getId() != contextAware.getTvFromThreadId();
                ScopeManager.INSTANCE.activate(restoringSpan, false, isAsyncPropagation); //activate it as the current active scope/span
            }
        }

        //Context propagation using the legacy Metadata
        contextAware.setTvPreviousContext(currentContext.isValid() ? currentContext : null);
        Metadata restoringContext = contextAware.getTvContext(); // get the context captured previously and try to restore it here
        if (restoringContext != null && restoringContext.isValid()) {

            // if the context is sampled, then we might need to create a clone as a fork
            if (restoringContext.isSampled()) {
                // if the propagation should always be considered async or if the current thread is different from the context aware object creation thread,
                // then create a cloned context (fork) if there has not been one yet
                if (isAlwaysAsync || Thread.currentThread().getId() != contextAware.getTvFromThreadId()) {
                    Metadata clonedContext = contextAware.tvGetClonedContext();
                    if (clonedContext == null) {
                        clonedContext = new Metadata(restoringContext);
                        clonedContext.setIsAsync(true); // set the metadata to asynchronous such that all events reported under that context will have the async flag set automatically
                    }
                    if (supportMultipleRestore) {
                        contextAware.tvSetClonedContext(clonedContext); // set it back to the context aware object, such that if it's restored again (supportMultipleRestore == true), then the same cloned instance is used
                    }
                    restoringContext = clonedContext; // use the cloned context
                }
            }

            Context.setMetadata(restoringContext);

            contextAware.setTvRestored(true); // flag that this context as restored that it's overwritten an existing context; such that upon exit of the run method, it should reset back to the existing context

            if (!supportMultipleRestore) {
                contextAware.setTvContext(null); // Clear the context if this contextAwareObject instance should only be used once
            }
        }
        return true;
    }

    public static void resetContext(Object contextAwareObject) {
        resetContext(contextAwareObject, true);
    }

    /**
     * 
     * @param contextAwareObject
     * @param resetToPreviousContext
     *            if true, reset it back to the previous context after finishing this runnable; if false, clear all context afterwards
     */
    public static void resetContext(Object contextAwareObject, boolean resetToPreviousContext) {
        // if a SDK span was previously restored, then pop it now
        if (contextAwareObject instanceof SpanAware) {
            Span restoringSpan = ((SpanAware) contextAwareObject).tvGetSpan();

            // only pop if it's the span that restored
            if (restoringSpan != null && restoringSpan.getSpanPropertyValue(SpanProperty.IS_SDK) && // only propagate SDK span for now
                    ScopeManager.INSTANCE.activeSpan() == restoringSpan) {
                Scope scope = ScopeManager.INSTANCE.active();

                scope.close(); // closing this scope should NOT finish the span as it's activated with finishOnClose as `false`
            }
        }

        // if the context was previously restored, then reset it
        if (contextAwareObject instanceof TvContextObjectAware && ((TvContextObjectAware) contextAwareObject).tvRestored()) {
            if (resetToPreviousContext) {
                Metadata previousContext = ((TvContextObjectAware) contextAwareObject).getTvPreviousContext();
                if (previousContext == null) {
                    Context.clearMetadata();
                } else {
                    Context.setMetadata(previousContext);
                }
            } else {
                Context.clearMetadata();
            }

            ((TvContextObjectAware) contextAwareObject).setTvPreviousContext(null);
            ((TvContextObjectAware) contextAwareObject).setTvRestored(false);
        }
    }

    // TODO comment out span capture/restore/reset for now as having both legacy context/metadata and the new Span context is rather complicated
    // public static boolean captureSpan(SpanAware spanAwareObject) {
    // ActiveSpan currentSpan = ContextManager.getCurrentSpan();
    // if (currentSpan != null) {
    // spanAwareObject.tvSetSpan(currentSpan);
    // return true;
    // } else {
    // return false;
    // }
    // }
    //
    // public static boolean restoreSpan(SpanAware spanAwareObject) {
    // return restoreSpan(spanAwareObject, false);
    // }
    //
    // /**
    // * "non-span" instrumentation relies on com.tracelytics.joboe.Context (ThreadLocal) for x-trace ID tracking
    // * "span" instrumentation relies on com.tracelytics.joboe.context.ContextManager to get current Span and Span.context().getMetadata() to get the x-trace ID of it
    // *
    // * "non-span" instrumentation never creates forks unless explicitly told to do so (therefore it always updates the current context)
    // * "span" instrumentation always creates forks for child(new) spans
    // *
    // * Now consider the effects we want when a span is "restored" on a different thread:
    // * 1. "non-span" instrumentation: Any new events should appear as forks of the restored span
    // * 2. "span" instrumentation : child(new) span created on this thread should have the restored span as parent
    // *
    // * To achieve 1., we would need to create a clone of the metadata(x-trace ID) and set it to com.tracelytics.joboe.Context
    // * To achieve 2., we can simply set the span to the current ContextManager
    // *
    // * Take note that the clone of metadata is created and set solely for "non-span" instrumentation, this clone of metadata should not be set back to the "span" as
    // * the "span" instance is shared by multiple thread and the metadata is immutable once span is created (makes sense as every events within the span should be linear).
    // *
    // * @param spanAwareObject
    // * @param flagAsnyc flag explicitly that this restore operation should be treated as asynchronous against where it was captured
    // * @return
    // *
    // */
    // public static boolean restoreSpan(SpanAware spanAwareObject, boolean flagAsnyc) {
    // BaseSpan<?> span = spanAwareObject.tvGetSpan();
    // ActiveSpan restoringSpan;
    // if (span instanceof Span) {
    // restoringSpan = Tracer.INSTANCE.makeActive((Span) span); //make active automatically add to context
    // } else {
    // restoringSpan = (ActiveSpan) spanAwareObject.tvGetSpan();
    // ContextManager.addSpan(restoringSpan);
    // }
    //
    // if (restoringSpan != null) {
    // boolean isAsync;
    //
    // if (flagAsnyc) {
    // isAsync = true;
    // } else {
    // if (Thread.currentThread().getId() != spanAwareObject.tvGetFromThreadId()) {
    // isAsync = true;
    // } else {
    // isAsync = false;
    // }
    // }
    //
    // if (isAsync) {
    // restoringSpan.setTag("isAsync", true);
    // }
    //
    // Metadata propagatingMetadata = restoringSpan.context().getMetadata();
    // if (propagatingMetadata != null) {
    // if (isAsync) {
    // propagatingMetadata = new Metadata(propagatingMetadata); //create a clone so it appears as a fork for all "non-span" instrumentation (all "span" instrumentation appear as fork always anyway)
    // propagatingMetadata.setIsAsync(true);
    // }
    // Context.setMetadata(propagatingMetadata); //ok if the metadata within the span is different instance from the cloned metadata, since javadoc of method
    // }
    //
    // return true;
    // } else {
    // return false;
    // }
    // }
    //
    // public static void resetSpan(SpanAware spanAwareObject) {
    // if (spanAwareObject.tvGetSpan() != null) {
    // spanAwareObject.tvSetSpan(null); //Clear the context. This instance should only be used once (see assumptions in this class javadoc), but in cases that it's reused, we do not want to propagate context more than once
    // ContextManager.removeSpan();
    // }
    // }
}
