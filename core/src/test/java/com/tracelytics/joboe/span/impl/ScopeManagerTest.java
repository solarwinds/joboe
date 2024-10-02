package com.tracelytics.joboe.span.impl;

import com.tracelytics.joboe.*;
import com.tracelytics.util.TestUtils;
import junit.framework.TestCase;

import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Test the extra methods provided by ActiveSpan compared to Span
 * @author pluk
 *
 */
public class ScopeManagerTest extends TestCase {
    private static final TestReporter tracingReporter = TestUtils.initTraceReporter();
    private EventReporter originalReporter;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Context.clearMetadata();
        ScopeManager.INSTANCE.removeAllScopes();
        TraceDecisionUtil.reset();

        originalReporter = EventImpl.setDefaultReporter(tracingReporter);
    }

    @Override
    protected void tearDown() throws Exception {
        Context.clearMetadata();
        ScopeManager.INSTANCE.removeAllScopes();
        TraceDecisionUtil.reset();

        EventImpl.setDefaultReporter(originalReporter);
        tracingReporter.reset();
        super.tearDown();
    }

	public void testSnapshot() throws ExecutionException, InterruptedException {
        ExecutorService executorService = Executors.newCachedThreadPool();

	    Future<Object> future1 = executorService.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                Scope s1 = Tracer.INSTANCE.buildSpan("1-1").startActive();
                Scope s2 = Tracer.INSTANCE.buildSpan("1-2").startActive();
                TimeUnit.SECONDS.sleep(1);

                ScopeContextSnapshot snapshot = ScopeManager.INSTANCE.getSnapshot();
                ScopeManager.INSTANCE.removeAllScopes(); //remove the scope while the 2nd callable still have active scopes
                TimeUnit.SECONDS.sleep(2);

                assertEquals(null, ScopeManager.INSTANCE.active()); //no active scope since it's cleared

                snapshot.restore();

                //now assert the scopes are restored;
                assertEquals(s2, ScopeManager.INSTANCE.active());
                s2.close();
                assertEquals(s1, ScopeManager.INSTANCE.active());
                s1.close();
                assertEquals(null, ScopeManager.INSTANCE.active());

                return null;
            }
        });

        Future<Object> future2 = executorService.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                Scope s1 = Tracer.INSTANCE.buildSpan("2-1").startActive();
                Scope s2 = Tracer.INSTANCE.buildSpan("2-2").startActive();
                TimeUnit.SECONDS.sleep(2);
                //when it wakes up here, 1st callable would have called removeAllScopes
                //verify the thread local nature, 1st callable's removeAllScopes should not affect this current thread
                assertEquals(s2, ScopeManager.INSTANCE.active());
                s2.close();
                assertEquals(s1, ScopeManager.INSTANCE.active());
                s1.close();
                assertEquals(null, ScopeManager.INSTANCE.active());

                return null;
            }
        });

        executorService.shutdown();
        future1.get();
        future2.get();
    }

    public void testRemoveScope() {
        Context.clearMetadata();
	    ScopeManager.INSTANCE.removeScope();

	    assertEquals(null, ScopeManager.INSTANCE.active());
	    assert(!Context.getMetadata().isValid());

	    //has legacy context
        Context.getMetadata().randomize(true);
        Metadata legacyContext = Context.getMetadata();
        ScopeManager.INSTANCE.removeScope();
        assertEquals(null, ScopeManager.INSTANCE.active());
        assert(Context.getMetadata().isValid()); //does not affect legacy context
        assert(legacyContext == Context.getMetadata());


        Context.clearMetadata();
        Scope scope1 = ScopeManager.INSTANCE.activate(Tracer.INSTANCE.buildSpan("test-1").withReporters(TraceEventSpanReporter.REPORTER).withSpanProperty(Span.SpanProperty.TRACE_DECISION_PARAMETERS, new TraceDecisionParameters(Collections.emptyMap(), null)).start());
        Metadata span1Metadata = scope1.span().context().getMetadata();
        assertSame(Context.getMetadata(), span1Metadata);
        assert(span1Metadata.isValid());
        assertSame(ScopeManager.INSTANCE.active(), scope1);

        Scope scope2 = ScopeManager.INSTANCE.activate(Tracer.INSTANCE.buildSpan("test-2").withReporters(TraceEventSpanReporter.REPORTER).start());
        Metadata span2Metadata = scope2.span().context().getMetadata();
        assertSame(Context.getMetadata(), span2Metadata);
        assert(span2Metadata.isValid());
        assertSame(ScopeManager.INSTANCE.active(), scope2);
        assertNotSame(span1Metadata, span2Metadata);
        assertNotSame(scope1, scope2);

        ScopeManager.INSTANCE.removeScope();
        assertSame(Context.getMetadata(), span1Metadata);
        assert(span1Metadata.isValid());
        assertSame(ScopeManager.INSTANCE.active(), scope1);

        ScopeManager.INSTANCE.removeScope();
        assert(!Context.getMetadata().isValid());
        assert(ScopeManager.INSTANCE.active() == null);
    }

    public void testRemoveAllScopes() {
        Context.clearMetadata();
        ScopeManager.INSTANCE.removeAllScopes();

        assertEquals(null, ScopeManager.INSTANCE.active());
        assert(!Context.getMetadata().isValid());

        //has legacy context
        Context.getMetadata().randomize(true);
        Metadata legacyContext = Context.getMetadata();
        ScopeManager.INSTANCE.removeAllScopes();
        assertEquals(null, ScopeManager.INSTANCE.active());
        assert(Context.getMetadata().isValid()); //does not affect legacy context
        assert(legacyContext == Context.getMetadata());


        Context.clearMetadata();
        Scope scope1 = ScopeManager.INSTANCE.activate(Tracer.INSTANCE.buildSpan("test-1").withReporters(TraceEventSpanReporter.REPORTER).withSpanProperty(Span.SpanProperty.TRACE_DECISION_PARAMETERS, new TraceDecisionParameters(Collections.emptyMap(), null)).start());
        Metadata span1Metadata = scope1.span().context().getMetadata();
        assertSame(Context.getMetadata(), span1Metadata);
        assert(span1Metadata.isValid());
        assertSame(ScopeManager.INSTANCE.active(), scope1);

        Scope scope2 = ScopeManager.INSTANCE.activate(Tracer.INSTANCE.buildSpan("test-2").withReporters(TraceEventSpanReporter.REPORTER).start());
        Metadata span2Metadata = scope2.span().context().getMetadata();
        assertSame(Context.getMetadata(), span2Metadata);
        assert(span2Metadata.isValid());
        assertSame(ScopeManager.INSTANCE.active(), scope2);
        assertNotSame(span1Metadata, span2Metadata);
        assertNotSame(scope1, scope2);

        ScopeManager.INSTANCE.removeAllScopes();
        assert(!Context.getMetadata().isValid());
        assert(ScopeManager.INSTANCE.active() == null);



        Context.getMetadata().randomize(true);
        legacyContext = Context.getMetadata();

        scope1 = ScopeManager.INSTANCE.activate(Tracer.INSTANCE.buildSpan("test-1").withReporters(TraceEventSpanReporter.REPORTER).withSpanProperty(Span.SpanProperty.TRACE_DECISION_PARAMETERS, new TraceDecisionParameters(Collections.emptyMap(), null)).start());
        span1Metadata = scope1.span().context().getMetadata();
        assertSame(Context.getMetadata(), span1Metadata);
        assertEquals(span1Metadata.taskHexString(), legacyContext.taskHexString());
        assert(!span1Metadata.opHexString().equals(legacyContext.opHexString()));
        assert(span1Metadata.isValid());
        assertSame(ScopeManager.INSTANCE.active(), scope1);

        scope2 = ScopeManager.INSTANCE.activate(Tracer.INSTANCE.buildSpan("test-2").withReporters(TraceEventSpanReporter.REPORTER).start());
        span2Metadata = scope2.span().context().getMetadata();
        assertSame(Context.getMetadata(), span2Metadata);
        assertEquals(span2Metadata.taskHexString(), legacyContext.taskHexString());
        assert(!span1Metadata.opHexString().equals(legacyContext.opHexString()));
        assert(!span1Metadata.opHexString().equals(span2Metadata.opHexString()));
        assert(span2Metadata.isValid());
        assertSame(ScopeManager.INSTANCE.active(), scope2);
        assertNotSame(span1Metadata, span2Metadata);
        assertNotSame(scope1, scope2);

        ScopeManager.INSTANCE.removeAllScopes();
        assert(Context.getMetadata().isValid());
        assertSame(legacyContext, Context.getMetadata());
        assert(ScopeManager.INSTANCE.active() == null);
    }


}
