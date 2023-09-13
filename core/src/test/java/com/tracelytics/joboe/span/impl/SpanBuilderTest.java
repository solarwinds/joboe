package com.tracelytics.joboe.span.impl;

import com.tracelytics.joboe.*;
import com.tracelytics.joboe.settings.*;
import com.tracelytics.joboe.settings.TestSettingsReader.SettingsMockupBuilder;
import com.tracelytics.joboe.span.impl.Span.SpanProperty;
import com.tracelytics.joboe.span.impl.Tracer.SpanBuilder;
import com.tracelytics.util.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class SpanBuilderTest {
    protected static final TestSettingsReader testSettingsReader = TestUtils.initSettingsReader();
    
    private static final Settings SAMPLED_SETTINGS =  new SettingsMockupBuilder().withFlags(true, false, true, true, false).withSampleRate(1000000).withSettingsArg(SettingsArg.BUCKET_CAPACITY, 16.0).withSettingsArg(SettingsArg.BUCKET_RATE, 8.0).build(); //ALWAYS sample rate = 100%
    private static final Settings NOT_SAMPLED_SETTINGS =  new SettingsMockupBuilder().withFlags(true, false, true, true, false).withSampleRate(0).build(); //Sampling enabled but sample rate = 0 . Metrics only then
    private static final Settings NOT_TRACED_SETTINGS = new SettingsMockupBuilder().withFlags(false, false, false, false, false).withSampleRate(0).build(); //all tracing (sampling/metrics) disabled
    
    private static final TraceDecisionParameters TRACE_DECISION_PARAMS =  new TraceDecisionParameters(Collections.EMPTY_MAP, null);

    private static final TestReporter tracingReporter = TestUtils.initTraceReporter();
    private static EventReporter originalReporter;

    @BeforeEach
    protected void setUp() throws Exception {
        testSettingsReader.reset();
        testSettingsReader.put(TestUtils.getDefaultSettings());
        Context.clearMetadata();
        ScopeManager.INSTANCE.removeAllScopes();
        TraceDecisionUtil.reset();

        originalReporter = EventImpl.setDefaultReporter(tracingReporter);
    }
    
    @AfterEach
    protected void tearDown() throws Exception {
        testSettingsReader.reset();
        Context.clearMetadata();
        ScopeManager.INSTANCE.removeAllScopes();
        TraceDecisionUtil.reset();

        EventImpl.setDefaultReporter(originalReporter);
        tracingReporter.reset();
    }

    @Test
    public void testNewSpanManual() {
        SpanBuilder builder = Tracer.INSTANCE.new SpanBuilder("new-span").withTag("tag1", 1).withTag("tag2", 2.0).withFlags((byte) 123);
        Span span = builder.start();
        assertNotNull(span);
        
        assertEquals("new-span", span.getOperationName());
        assertNotSame(Context.getMetadata(), span.context().getMetadata()); //not same instance as it's startManual
        assertNull(span.context().getParentId()); //no parent
        
        assertEquals((byte) 123, span.context().getFlags());
        assertEquals(1, span.getTags().get("tag1"));
        assertEquals(2.0, span.getTags().get("tag2"));
        
        //assert that it's not in current context
        assertNull(ScopeManager.INSTANCE.activeSpan());
    }

    @Test
    public void testAsChildManual() {
        //test ctor that takes a parent context to ensure baggage propagated and metadata cloned
        Metadata metadata = new Metadata();
        metadata.randomize();
        
        Span parentSpan = Tracer.INSTANCE.new SpanBuilder("parent-span").start();
        parentSpan.setBaggageItem("baggage1", "1");
        parentSpan.setBaggageItem("baggage2", "2");
        SpanContext parentContext = parentSpan.context();
        
        Span childSpan =  Tracer.INSTANCE.new SpanBuilder("parent-span").asChildOf(parentSpan).start();
        SpanContext childContext = childSpan.context();
        
        assertSameTraceContext(parentContext, childContext);
        
        //to ensure the child context's metadata is a clone
        childContext.getMetadata().randomizeOpID();
        
        assertTrue(parentContext.getMetadata().isTaskEqual(childContext.getMetadata())); //parent should still have same task ID as child
        assertFalse(parentContext.getMetadata().isOpEqual(childContext.getMetadata())); //parent should not have same op ID as child
    }

    @Test
    public void testNewSpanActive() {
        SpanBuilder builder = Tracer.INSTANCE.new SpanBuilder("new-span").withFlags((byte) 123).withTag("tag1", 1).withTag("tag2", 2.0);
        Span span = builder.startActive().span();
        assertNotNull(span);
        
        assertEquals("new-span", span.getOperationName());
        assertSame(Context.getMetadata(), span.context().getMetadata()); //has to be the same instance
        assertNull(span.context().getParentId()); //no parent
        
        assertEquals(123, span.context().getFlags());
        assertEquals(1, span.getTags().get("tag1"));
        assertEquals(2.0, span.getTags().get("tag2"));
        
        //assert that it is in current context
        assertEquals(ScopeManager.INSTANCE.activeSpan(), span);
    }

    @Test
    public void testAsChildActive() {
        //test ctor that takes a parent context to ensure baggage propagated and metadata cloned
        Metadata metadata = new Metadata();
        metadata.randomize();
        
        Span parentSpan = Tracer.INSTANCE.new SpanBuilder("parent-span").startActive().span();
        parentSpan.setBaggageItem("baggage1", "1");
        parentSpan.setBaggageItem("baggage2", "2");
        SpanContext parentContext = parentSpan.context();
        
        Span childSpan =  Tracer.INSTANCE.new SpanBuilder("parent-span").asChildOf(parentSpan).startActive().span();
        SpanContext childContext = childSpan.context();
        
        assertSameTraceContext(parentContext, childContext);
        
        //to ensure the child context's metadata is a clone
        childContext.getMetadata().randomizeOpID();
        
        assertTrue(parentContext.getMetadata().isTaskEqual(childContext.getMetadata())); //parent should still have same task ID as child
        assertFalse(parentContext.getMetadata().isOpEqual(childContext.getMetadata())); //parent should not have same op ID as child
        
        //assert context
        assertEquals(ScopeManager.INSTANCE.removeScope().span(), childSpan); //child span first
        assertEquals(ScopeManager.INSTANCE.removeScope().span(), parentSpan); //then parent span
    }
    
    /**
     * Test inferred context as described in {@link com.tracelytics.joboe.span.Tracer.SpanBuilder#addReference(String, com.tracelytics.joboe.span.SpanContext)}
     * 
     */
    @Test
    public void testInferredContext() {
        //create an active context
        Tracer tracer = Tracer.INSTANCE;
        Scope parentScope = tracer.buildSpan("parent").startActive();
        
        Scope childActiveScope = tracer.buildSpan("active-child").startActive(); //it should infer the current active span as parent
        assertSameTraceContext(parentScope.span().context(), childActiveScope.span().context());
        childActiveScope.close();
                
        Span childSpan = tracer.buildSpan("non-active-child").startManual(); //it should infer the current active span as parent
        assertSameTraceContext(parentScope.span().context(), childSpan.context());
        childSpan.finish();
        
        childActiveScope = tracer.buildSpan("active-child").ignoreActiveSpan().startActive(); //it should NOT infer the current active span as parent
        assertNotSame(parentScope.span().context().getTraceId(), childActiveScope.span().context().getTraceId());
        childActiveScope.close();
        
        childSpan = tracer.buildSpan("non-active-child").ignoreActiveSpan().start(); //it should NOT infer the current active span as parent
        assertNotSame(parentScope.span().context().getTraceId(), childSpan.context().getTraceId());
        childSpan.finish();
        
        Span detachedSpan = tracer.buildSpan("detached").ignoreActiveSpan().start();
        
        childActiveScope = tracer.buildSpan("active-child").asChildOf(detachedSpan).startActive(); //it should NOT infer the current active span as parent as parent is explicitly set
        assertNotSame(parentScope.span().context().getTraceId(), childActiveScope.span().context().getTraceId());
        assertSameTraceContext(detachedSpan.context(), childActiveScope.span().context());
        childActiveScope.close();
        
        childSpan = tracer.buildSpan("non-active-child").asChildOf(detachedSpan).start(); //it should NOT infer the current active span as parent as parent is explicitly set
        assertNotSame(parentScope.span().context().getTraceId(), childSpan.context().getTraceId());
        assertSameTraceContext(detachedSpan.context(), childSpan.context());
        childSpan.finish();
    }

    @Test
    public void testContext() {
        Tracer tracer = Tracer.INSTANCE;
        Metadata spanMetadata;
        
        Scope scope1 = tracer.buildSpan("span1").withReporters(TraceEventSpanReporter.REPORTER).withSpanProperty(SpanProperty.TRACE_DECISION_PARAMETERS, TRACE_DECISION_PARAMS).startActive();
        Span span1 = scope1.span();
        spanMetadata = span1.context().getMetadata();
        assertTrue(spanMetadata.isValid()); 
        assertTrue(spanMetadata.isSampled());
        assertTrue(spanMetadata.isReportMetrics());
        assertSame(spanMetadata, Context.getMetadata()); //should have updated the TLS context to use the active span
        
        Scope scope2 = tracer.buildSpan("span2").withReporters(TraceEventSpanReporter.REPORTER).startActive();
        Span span2 = scope2.span();
        spanMetadata = span2.context().getMetadata();
        assertTrue(spanMetadata.isValid()); 
        assertTrue(spanMetadata.isSampled());
        assertTrue(spanMetadata.isReportMetrics());
        assertSame(spanMetadata, Context.getMetadata()); //should have updated the TLS context to use the active span
        assertNotEquals(span1.context().getMetadata().opHexString(), span2.context().getMetadata().opHexString()); //different op ID after entry event is reported
        assertSameTraceContext(span1.context(), span2.context());
        
        scope2.close();
        assertFalse(span2.context().getMetadata().isValid()); //should be invalidated
        assertSame(span1.context().getMetadata(), Context.getMetadata()); //TLS reverts back to span1 context
        assertTrue(Context.getMetadata().isValid()); //TLS context should still be valid
        
        scope1.close();
        assertFalse(span1.context().getMetadata().isValid()); //should be invalidated
        assertFalse(Context.getMetadata().isValid()); //TLS context should also be cleared
        
        
        Span span3 = tracer.buildSpan("span3").withReporters(TraceEventSpanReporter.REPORTER).withSpanProperty(SpanProperty.TRACE_DECISION_PARAMETERS, TRACE_DECISION_PARAMS).startManual();
        spanMetadata = span3.context().getMetadata();
        assertTrue(spanMetadata.isValid()); 
        assertTrue(spanMetadata.isSampled());
        assertTrue(spanMetadata.isReportMetrics());
        assertNotSame(spanMetadata, Context.getMetadata()); //should NOT update TLS as it's not sampled
        assertFalse(Context.getMetadata().isValid()); //TLS context should still be invalid
        
        //now when span3 is still around, let's create span4 which is an active span
        Scope scope4 = tracer.buildSpan("span4").withReporters(TraceEventSpanReporter.REPORTER).withSpanProperty(SpanProperty.TRACE_DECISION_PARAMETERS, TRACE_DECISION_PARAMS).startActive();
        Span span4= scope4.span();
        spanMetadata = span4.context().getMetadata();
        assertTrue(spanMetadata.isValid()); 
        assertTrue(spanMetadata.isSampled());
        assertTrue(spanMetadata.isReportMetrics());
        assertSame(spanMetadata, Context.getMetadata()); //should have updated the TLS context to use the active span
        
        //span3 and span4 should have completely different metadata, even though they are on the same thread
        assertNotEquals(span3.context().getMetadata().taskHexString(), span4.context().getMetadata().taskHexString());
        
        //now span3 finish while span4 still active, should NOT affect span4's context nor TlS
        span3.finish(); 
        spanMetadata = span3.context().getMetadata();
        assertFalse(spanMetadata.isValid());
        spanMetadata = span4.context().getMetadata();
        assertTrue(spanMetadata.isValid()); //span 4 should still be valid
        assertTrue(Context.getMetadata().isValid()); //so is the TLS metadata
        
        scope4.close();
        spanMetadata = span4.context().getMetadata();
        assertFalse(spanMetadata.isValid()); //both span 4 and TLS should be cleared now
        assertFalse(Context.getMetadata().isValid()); //so is the TLS metadata
    }
    
    /**
     * Tests if traces are started by legacy way (ie entry event individually)
     */
    @Test
    public void testLegacyContext() {
        testSettingsReader.put(SAMPLED_SETTINGS);
        Metadata existingContext = Context.getMetadata();
        existingContext.randomize(true); //simulate existing context set by legacy event but there's no active span
                
        Tracer tracer = Tracer.INSTANCE;
        Metadata spanMetadata;

        try (Scope scope = Tracer.INSTANCE.activateSpan(tracer.buildSpan("span1").withReporters(TraceEventSpanReporter.REPORTER).start())) {
            Span span1 = scope.span();
            spanMetadata = span1.context().getMetadata();
            assertTrue(spanMetadata.isValid());
            assertTrue(spanMetadata.isSampled());
            assertFalse(spanMetadata.isReportMetrics()); //legacy context (trace started by legacy start event) would not have metrics
            assertEquals(existingContext.taskHexString(), spanMetadata.taskHexString()); //same task as the legacy context
            assertNotEquals(existingContext.opHexString(), spanMetadata.opHexString()); //but different op
            assertSame(spanMetadata, Context.getMetadata()); //should have updated the TLS context to use the active span
            span1.finish();
        }

        //now it should revert back to the legacy context
        assertSame(existingContext, Context.getMetadata());
        assertTrue(existingContext.isValid());
        assertTrue(existingContext.isSampled());
        assertFalse(existingContext.isReportMetrics());
    }

    @Test
    public void testSampled() {
        testSettingsReader.put(SAMPLED_SETTINGS);
        Map<String, Object> tags = Collections.singletonMap("test-tag", "test-value");
        
        Tracer tracer = Tracer.INSTANCE;
        Span span = tracer.buildSpan("test-layer").withTags(tags).withReporters(TraceEventSpanReporter.REPORTER).withSpanProperty(SpanProperty.TRACE_DECISION_PARAMETERS, TRACE_DECISION_PARAMS).startActive().span();
        
        //assert TLS context is set properly
        assertTrue(Context.getMetadata().isValid());
        assertTrue(Context.getMetadata().isSampled());
        assertTrue(Context.getMetadata().isReportMetrics());
        
        
        //assert the span has correct properties
        assertEquals("test-layer", span.getOperationName());
        assertTrue(span.getTags().keySet().containsAll(tags.keySet()));
        assertNotNull(span.getSpanPropertyValue(SpanProperty.TRACE_DECISION).getTraceConfig());
        assertSame(Context.getMetadata(), span.context().getMetadata()); //confirm that metadata is synchronized
    }

    @Test
    public void testNotSampled() {
        testSettingsReader.put(NOT_SAMPLED_SETTINGS);
        Map<String, Object> tags = Collections.singletonMap("test-tag", "test-value");
        
        Tracer tracer = Tracer.INSTANCE;
        Span span = tracer.buildSpan("test-layer").withTags(tags).withReporters(TraceEventSpanReporter.REPORTER).withSpanProperty(SpanProperty.TRACE_DECISION_PARAMETERS, TRACE_DECISION_PARAMS).startActive().span();
        
        //assert TLS context is set properly
        assertTrue(Context.getMetadata().isValid());
        assertFalse(Context.getMetadata().isSampled());
        assertTrue(Context.getMetadata().isReportMetrics());
        
        
        //assert the span has correct properties
        assertEquals("test-layer", span.getOperationName());
        assertTrue(span.getTags().keySet().containsAll(tags.keySet()));
        List<String> entryServiceKvs = new ArrayList<String>(Arrays.asList("SampleRate", "SampleSource", "BucketCapacity", "BucketRate"));
        entryServiceKvs.retainAll(span.getTags().keySet());
        assertTrue(entryServiceKvs.isEmpty()); //should NOT contain any of the entry service keys, not sampled

        assertSame(Context.getMetadata(), span.context().getMetadata()); //confirm that metadata is synchronized
    }

    @Test
    public void testNotTraced() {
        testSettingsReader.put(NOT_TRACED_SETTINGS);
        Map<String, Object> tags = Collections.singletonMap("test-tag", "test-value");
        
        Tracer tracer = Tracer.INSTANCE;
        Span span = tracer.buildSpan("test-layer").withTags(tags).withReporters(TraceEventSpanReporter.REPORTER).withSpanProperty(SpanProperty.TRACE_DECISION_PARAMETERS, TRACE_DECISION_PARAMS).startActive().span();
        
        //assert TLS context is set properly
        assertTrue(Context.getMetadata().isValid()); //even when it's completely not traced, we still want a valid context (isSampled set to false)
        assertFalse(Context.getMetadata().isSampled());
        assertFalse(Context.getMetadata().isReportMetrics());
        
        
        //assert the span has correct properties
        assertEquals("test-layer", span.getOperationName());
        assertTrue(span.getTags().keySet().containsAll(tags.keySet()));

        assertSame(Context.getMetadata(), span.context().getMetadata()); //confirm that metadata is synchronized
    }

    @Test
    public void testNonEntryPoint() {
        testSettingsReader.put(SAMPLED_SETTINGS);
        Map<String, Object> tags = Collections.singletonMap("test-tag", "test-value");
        
        Tracer tracer = Tracer.INSTANCE;
        Span span = tracer.buildSpan("test-layer").withTags(tags).withReporters(TraceEventSpanReporter.REPORTER).startActive().span(); //no SpanProperty.TRACE_DECISION_PARAMETERS indicates that this is not a trace entry point
        
        //assert TLS context is set properly
        assertFalse(Context.getMetadata().isValid());
        assertFalse(Context.getMetadata().isSampled());
        assertFalse(Context.getMetadata().isReportMetrics());
        
        
        //assert the span has correct properties
        assertEquals("test-layer", span.getOperationName());
        assertTrue(span.getTags().keySet().containsAll(tags.keySet()));
        assertSame(Context.getMetadata(), span.context().getMetadata()); //confirm that metadata is synchronized
    }

    @Test
    public void testSpanSampledContinue() {
        testSettingsReader.put(SAMPLED_SETTINGS);
        Map<String, Object> tags = Collections.singletonMap("test-tag", "test-value");
        Metadata incomingMetadata = new Metadata();
        incomingMetadata.randomize(true);
        String xTraceId = incomingMetadata.toHexString();
        
        Tracer tracer = Tracer.INSTANCE;
        Span span = tracer.buildSpan("test-layer").withTags(tags).withSpanProperty(SpanProperty.TRACE_DECISION_PARAMETERS, new TraceDecisionParameters(Collections.singletonMap(XTraceHeader.TRACE_ID, xTraceId), null)).withReporters(TraceEventSpanReporter.REPORTER).startActive().span();
        
        //assert TLS context is set properly
        assertTrue(Context.getMetadata().isValid());
        assertTrue(Context.getMetadata().isSampled());
        assertTrue(Context.getMetadata().isReportMetrics());
        assertEquals(incomingMetadata.taskHexString(), Context.getMetadata().taskHexString()); //same task
        assertNotEquals(incomingMetadata.opHexString(), Context.getMetadata().opHexString()); //not the same op as the entry event of this span is reported
        
        
        //assert the span has correct properties
        assertEquals("test-layer", span.getOperationName());
        assertTrue(span.getTags().keySet().containsAll(tags.keySet()));
        List<String> entryServiceKvs = new ArrayList<String>(Arrays.asList("SampleRate", "SampleSource", "BucketCapacity", "BucketRate"));
        entryServiceKvs.retainAll(span.getTags().keySet());
        assertTrue(entryServiceKvs.isEmpty()); //should NOT contain any of the entry service keys, as this is NOT the entry service
        assertSame(Context.getMetadata(), span.context().getMetadata()); //should be the same instance as this is an active span
    }


    @Test
    public void testNotTracedContinue() {
        testSettingsReader.put(NOT_TRACED_SETTINGS);
        Map<String, Object> tags = Collections.singletonMap("test-tag", "test-value");
        Metadata incomingMetadata = new Metadata();
        incomingMetadata.randomize(true);
        String xTraceId = incomingMetadata.toHexString();
        
        Tracer tracer = Tracer.INSTANCE;
        Span span = tracer.buildSpan("test-layer").withTags(tags).withSpanProperty(SpanProperty.TRACE_DECISION_PARAMETERS, new TraceDecisionParameters(Collections.singletonMap(XTraceHeader.TRACE_ID, xTraceId), null)).withReporters(TraceEventSpanReporter.REPORTER).startActive().span();
        
        //assert TLS context is set properly
        assertTrue(Context.getMetadata().isValid());
        assertFalse(Context.getMetadata().isSampled()); //should no longer be sampled/traced as never mode flips the decision
        assertFalse(Context.getMetadata().isReportMetrics()); //should no longer be sampled/traced as never mode flips the decision
        assertEquals(incomingMetadata.taskHexString(), Context.getMetadata().taskHexString());
        assertEquals(incomingMetadata.opHexString(), Context.getMetadata().opHexString()); //same op as no entry event is generated
        assertTrue(incomingMetadata.isSampled() != Context.getMetadata().isSampled());
        
        
        
        //assert the span has correct properties
        assertEquals("test-layer", span.getOperationName());
        assertTrue(span.getTags().keySet().containsAll(tags.keySet()));
        List<String> entryServiceKvs = new ArrayList<String>(Arrays.asList("SampleRate", "SampleSource", "BucketCapacity", "BucketRate"));
        entryServiceKvs.retainAll(span.getTags().keySet());
        assertTrue(entryServiceKvs.isEmpty()); //should NOT contain any of the entry service keys, as this is NOT the entry service
        assertSame(Context.getMetadata(), span.context().getMetadata()); //should be the same instance as this is an active span
    }


    @Test
    public void testContextTtl() throws InterruptedException {
        int newTtl = 2;
        SimpleSettingsFetcher fetcher = (SimpleSettingsFetcher) SettingsManager.getFetcher();
        //set to 2 secs
        testSettingsReader.put(new SettingsMockupBuilder().withFlags(TracingMode.ALWAYS).withSampleRate(TraceDecisionUtil.SAMPLE_RESOLUTION).withSettingsArg(SettingsArg.MAX_CONTEXT_AGE, newTtl).build());

        Tracer tracer = Tracer.INSTANCE;
        Scope parentScope = tracer.buildSpan("parent-span").withReporters(TraceEventSpanReporter.REPORTER).withSpanProperty(SpanProperty.TRACE_DECISION_PARAMETERS, TRACE_DECISION_PARAMS).startActive();
        Span parentSpan = parentScope.span();

        //assert TLS context is set properly
        assertTrue(Context.getMetadata().isValid());
        assertTrue(Context.getMetadata().isSampled());
        assertTrue(Context.getMetadata().isReportMetrics());

        assertTrue(parentSpan.context().getMetadata().isValid());
        assertTrue(parentSpan.context().getMetadata().isSampled());

        Span childSpan = tracer.buildSpan("child-span-1").withReporters(TraceEventSpanReporter.REPORTER).start();
        assertTrue(childSpan.context().getMetadata().isValid());
        assertTrue(childSpan.context().getMetadata().isSampled());
        childSpan.finish();

        TimeUnit.SECONDS.sleep(newTtl + 1);

        childSpan = tracer.buildSpan("child-span-2").withReporters(TraceEventSpanReporter.REPORTER).start();
        assertFalse(childSpan.context().getMetadata().isValid()); //expired
        assertFalse(childSpan.context().getMetadata().isSampled()); //expired
        childSpan.finish();

        assertFalse(parentSpan.context().getMetadata().isValid()); //expired
        assertFalse(parentSpan.context().getMetadata().isSampled()); //expired

        parentScope.close();
        assertFalse(Context.getMetadata().isValid());

        testSettingsReader.reset();
        tracingReporter.reset();
    }

    private void assertSameTraceContext(SpanContext parentContext, SpanContext childContext) {
        assertTrue(parentContext.getMetadata().isTaskEqual(childContext.getMetadata())); //share the same task
        assertEquals(parentContext.baggageItems(), childContext.baggageItems()); //baggage items should be propagated
        assertEquals(parentContext.getFlags(), childContext.getFlags()); //so are flags
        assertEquals(parentContext.getTraceId(), childContext.getTraceId()); //share same trace ID
        assertEquals(parentContext.getSpanId(), childContext.getParentId().longValue()); //child should have parentId as parent's span ID
        assertSame(parentContext.getMetadata(), childContext.getPreviousMetadata());
    }
}
