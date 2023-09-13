package com.tracelytics.joboe.span.impl;

import com.tracelytics.joboe.*;
import com.tracelytics.joboe.TestReporter;
import com.tracelytics.joboe.TestReporter.DeserializedEvent;
import com.tracelytics.joboe.settings.TestSettingsReader;
import com.tracelytics.joboe.span.impl.Span.SpanProperty;
import com.tracelytics.joboe.span.impl.Tracer.SpanBuilder;
import com.tracelytics.util.TestUtils;
import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests on context management for new span/legacy event-based span 
 * @author Patson
 *
 */
public class ContextTest {
    private EventReporter originalReporter;
    private static final TestReporter tracingReporter = TestUtils.initTraceReporter();
    protected static final TestSettingsReader testSettingsReader = TestUtils.initSettingsReader();
    
    
    @BeforeEach
    protected void setUp() throws Exception {
        originalReporter = EventImpl.setDefaultReporter(tracingReporter);
        testSettingsReader.reset();
        testSettingsReader.put(TestUtils.getDefaultSettings());
        Context.clearMetadata();
        ScopeManager.INSTANCE.removeAllScopes();
    }
    
    @AfterEach
    protected void tearDown() throws Exception {
        testSettingsReader.reset();
        EventImpl.setDefaultReporter(originalReporter);
        ScopeManager.INSTANCE.removeAllScopes();
        Context.clearMetadata(); //clear context
        tracingReporter.reset();
    }

    @Test
    public void testNestedSpans() throws Exception {
        Scope scope1 = startTraceScope("span1");
        Span span1 = scope1.span();
        DeserializedEvent span1EntryEvent = getLastSentEvent();
        
        span1.setTag("URL", "nested-spans");
        span1.log("test", 2);
        DeserializedEvent span1InfoEvent = getLastSentEvent();
        assertEdge(span1EntryEvent, span1InfoEvent);
        
        Scope scope2 = startScope("span2");
        Span span2 = scope2.span();
        DeserializedEvent span2EntryEvent = getLastSentEvent();
        assertEdge(span1InfoEvent, span2EntryEvent); //span 2 entry should point to the span 1 info event
        
        span2.log("test", 2);
        DeserializedEvent span2InfoEvent = getLastSentEvent();
        assertEdge(span2EntryEvent, span2InfoEvent);
        
        Scope scope3 = startScope("span3");
        Span span3 = scope3.span();
        DeserializedEvent span3EntryEvent = getLastSentEvent();
        assertEdge(span2InfoEvent, span3EntryEvent); //span 3 entry should point to the span 2 info event
        
        scope3.close();
        DeserializedEvent span3ExitEvent = getLastSentEvent();
        assertEdge(span3EntryEvent, span3ExitEvent);
        
        scope2.close();
        DeserializedEvent span2ExitEvent = getLastSentEvent();
        assertEdge(span2InfoEvent, span2ExitEvent);
        assertEdge(span3ExitEvent, span2ExitEvent);
        
        scope1.close();
        DeserializedEvent span1ExitEvent = getLastSentEvent();
        assertEdge(span1InfoEvent, span1ExitEvent);
        assertEdge(span2ExitEvent, span1ExitEvent);

        assertFalse(Context.isValid()); //should clear all context
    }

    @Test
    public void testSiblingSpans() throws Exception {
        Scope scope1 = startTraceScope("span1");
        Span span1 = scope1.span();
        DeserializedEvent span1EntryEvent = getLastSentEvent();
        
        span1.setTag("URL", "sibling-spans");
        
        Scope scope2 = startScope("span2");
        Span span2 = scope2.span();
        DeserializedEvent span2EntryEvent = getLastSentEvent();
        assertEdge(span1EntryEvent, span2EntryEvent); //span 2 entry should point to the span 1 entry event
        
        scope2.close();
        DeserializedEvent span2ExitEvent = getLastSentEvent();
        assertEdge(span2EntryEvent, span2ExitEvent);
        
        span1.log("test", 1);
        DeserializedEvent span1InfoEvent = getLastSentEvent();
        assertEdge(span1EntryEvent, span1InfoEvent);
        
        Scope scope3 = startScope("span3");
        Span span3 = scope3.span();
        DeserializedEvent span3EntryEvent = getLastSentEvent();
        assertEdge(span1InfoEvent, span3EntryEvent); //span 3 entry should point to the span 1 info event
        
        scope3.close();
        DeserializedEvent span3ExitEvent = getLastSentEvent();
        assertEdge(span3EntryEvent, span3ExitEvent);
        
        scope1.close();
        DeserializedEvent span1ExitEvent = getLastSentEvent();
        assertEdge(span3ExitEvent, span1ExitEvent);
        assertEdge(span1InfoEvent, span1ExitEvent);

        assertFalse(Context.isValid()); //should clear all context
    }


    @Test
    public void testLegacySpanLegacy() throws Exception {
        Context.getMetadata().randomize(); //create valid metadata
        Event legacyRootEntry = Context.createEventWithContext(Context.getMetadata(), true); //a root span with legacy event-based approach
        legacyRootEntry.addInfo("Layer", "legacy-span1",
                "Label", "entry",
                "URL", "legacy-span-legacy");
        legacyRootEntry.report();
        DeserializedEvent span1EntryEvent = getLastSentEvent();
        
        Scope scope = startScope("span2"); //a span nested below a legacy span
        Span span = scope.span();
        DeserializedEvent span2EntryEvent = getLastSentEvent();
        assertEdge(span1EntryEvent, span2EntryEvent); //span 2 entry should point to the span 1 entry event
        
        Event legacyEntry = Context.createEvent();
        legacyEntry.addInfo(
                "Layer", "legacy-span3",
                "Label", "entry");
        legacyEntry.report();
        DeserializedEvent span3EntryEvent = getLastSentEvent();
        assertEdge(span2EntryEvent, span3EntryEvent); //legacy event should "inline" into current active span, which is span 2
        
        Event legacyExit = Context.createEvent();
        legacyExit.addInfo(
                "Layer", "legacy-span3",
                "Label", "exit");
        legacyExit.report();
        DeserializedEvent span3ExitEvent = getLastSentEvent();
        assertEdge(span3EntryEvent, span3ExitEvent);
        
        scope.close();
        DeserializedEvent span2ExitEvent = getLastSentEvent();
        assertEdge(span3ExitEvent, span2ExitEvent); //legacy event should "inline" into current active span, which is span 2
        
        Event legacyRootExit = Context.createEvent(); //a root span with legacy event-based approach
        legacyRootExit.addInfo("Layer", "legacy-span1",
                "Label", "exit");
        legacyRootExit.report();
        DeserializedEvent span1ExitEvent = getLastSentEvent();
        assertEdge(span1EntryEvent, span1ExitEvent);
    }

    @Test
    public void testSpanLegacySpan() throws Exception {
        Scope scope1 = startTraceScope("span1");
        Span span1 = scope1.span();
        DeserializedEvent span1EntryEvent = getLastSentEvent();
        
        span1.setTag("URL", "span-legacy-span");
        span1.log("test", 2);
        DeserializedEvent span1InfoEvent = getLastSentEvent();
        assertEdge(span1EntryEvent, span1InfoEvent);
        
        Event legacyEntry = Context.createEvent();
        legacyEntry.addInfo(
                "Layer", "legacy-span2",
                "Label", "entry");
        legacyEntry.report();
        DeserializedEvent span2EntryEvent = getLastSentEvent();
        assertEdge(span1InfoEvent, span2EntryEvent); //span 2 entry should "inline" into active span 1, hence pointing at span 1's info event
        
        Scope scope3 = startTraceScope("span3");
        Span span3 = scope3.span();
        DeserializedEvent span3EntryEvent = getLastSentEvent();
        assertEdge(span2EntryEvent, span3EntryEvent);
        
        scope3.close();
        DeserializedEvent span3ExitEvent = getLastSentEvent();
        assertEdge(span3EntryEvent, span3ExitEvent);
        
        Event legacyExit = Context.createEvent();
        legacyExit.addInfo(
                "Layer", "legacy-span2",
                "Label", "exit");
        legacyExit.report();
        DeserializedEvent span2ExitEvent = getLastSentEvent();
        assertEdge(span2EntryEvent, span2ExitEvent); //legacy span 2 exit should point directly back to its entry instead of span 3, as span 3 is a "branch"
        
        scope1.close();
        DeserializedEvent span1ExitEvent = getLastSentEvent();
        assertEdge(span2ExitEvent, span1ExitEvent); //span 1 exit should point to legacy span 2 exit, as span 2 is "inline" into span 1
        assertEdge(span3ExitEvent, span1ExitEvent); //span 1 exit should point to span 3 exit as span 3 is a child of span 1


        assertFalse(Context.isValid()); //should clear all context
    }

    @Test
    public void testAsyncSpans() throws Throwable {
        final TestReporter threadLocalReporter = ReporterFactory.getInstance().buildTestReporter(true);
        Field field = EventImpl.class.getDeclaredField("DEFAULT_REPORTER");
        field.setAccessible(true);
        
        TestReporter existingReporter = (TestReporter) field.get(null);
        
        field.set(null, threadLocalReporter);
        
        
        Scope scope1 = startTraceScope("span1");
        Span span1 = scope1.span();
        DeserializedEvent span1EntryEvent = getLastSentEvent(threadLocalReporter);
        
        span1.setTag("URL", "async-spans");
        span1.log("test", 2);
        final DeserializedEvent span1InfoEvent = getLastSentEvent(threadLocalReporter);
        assertEdge(span1EntryEvent, span1InfoEvent);
        
        final List<Throwable> asyncErrors = Collections.synchronizedList(new ArrayList<Throwable>());
        
        
        Thread thread1 = new Thread() {
            @Override
            public void run() {
                try {
                    Scope scope2 = startScope("async-span2", true);
                    Span span2 = scope2.span();
                    DeserializedEvent span2EntryEvent = getLastSentEvent(threadLocalReporter);
                    assertEdge(span1InfoEvent, span2EntryEvent);
                    
                    span2.log("test", 2);
                    DeserializedEvent span2InfoEvent = getLastSentEvent(threadLocalReporter);
                    assertEdge(span2EntryEvent, span2InfoEvent);
                    
                    Scope scope4 = startScope("span4");
                    Span span4 = scope4.span();
                    DeserializedEvent span4EntryEvent = getLastSentEvent(threadLocalReporter);
                    assertEdge(span2InfoEvent, span4EntryEvent);
                    
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    scope4.close();
                    DeserializedEvent span4ExitEvent = getLastSentEvent(threadLocalReporter);
                    assertEdge(span4EntryEvent, span4ExitEvent);
                    
                    scope2.close();
                    DeserializedEvent span2ExitEvent = getLastSentEvent(threadLocalReporter);
                    assertEdge(span4ExitEvent, span2ExitEvent);
                    assertEdge(span2InfoEvent, span2ExitEvent);
                } catch (Throwable e) {
                    asyncErrors.add(e);
                }
            }
        };
        thread1.start();
        
        Thread thread2 = new Thread() {
            @Override
            public void run() {
                try {
                    Scope scope3 = startScope("async-span3", true);
                    Span span3 = scope3.span();
                    DeserializedEvent span3EntryEvent = getLastSentEvent(threadLocalReporter);
                    assertEdge(span1InfoEvent, span3EntryEvent);
                    
                    span3.log("test", 3);
                    DeserializedEvent span3InfoEvent = getLastSentEvent(threadLocalReporter);
                    assertEdge(span3EntryEvent, span3InfoEvent);
                    
                    Scope scope5 = startScope("span5");
                    Span span5 = scope5.span();
                    DeserializedEvent span5EntryEvent = getLastSentEvent(threadLocalReporter);
                    assertEdge(span3InfoEvent, span5EntryEvent);
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    scope5.close();
                    DeserializedEvent span5ExitEvent = getLastSentEvent(threadLocalReporter);
                    assertEdge(span5EntryEvent, span5ExitEvent);
                    
                    scope3.close();
                    DeserializedEvent span3ExitEvent = getLastSentEvent(threadLocalReporter);
                    assertEdge(span5ExitEvent, span3ExitEvent);
                    assertEdge(span3InfoEvent, span3ExitEvent);
                } catch (Throwable e) {
                    asyncErrors.add(e);
                }
            }
        };
        
        thread2.start();
        
        scope1.close();
        DeserializedEvent span1ExitEvent = getLastSentEvent(threadLocalReporter);
        assertEdge(span1InfoEvent, span1ExitEvent);
        assertFalse(Context.isValid()); //should clear all context
        
        thread1.join();
        thread2.join();
        
        field.set(null, existingReporter); //revert back to the original reporter
        
        for (Throwable error : asyncErrors) { //re-throw any async errors
            throw error;
        }
    }
    
    private DeserializedEvent getLastSentEvent(TestReporter reporter) {
        List<DeserializedEvent> sentEvents = reporter.getSentEvents();
        
        return sentEvents.isEmpty() ? null : sentEvents.get(sentEvents.size() - 1);
    }

    @Test
    public void testCreateEventWithGeneratedId() {
        Metadata contextMetadata = Context.getMetadata();
        contextMetadata.randomize(true);
        
        String startEdge = contextMetadata.opHexString();
        
        Metadata metadata = new Metadata(contextMetadata);
        metadata.randomizeOpID();
        
        try {
            Event event = Context.createEventWithID(metadata.toHexString());
            event.report();
            
            DeserializedEvent sentEvent = getLastSentEvent();
            
            assertEquals(startEdge, sentEvent.getSentEntries().get(Constants.XTR_EDGE_KEY));
            assertEquals(metadata.toHexString(), sentEvent.getSentEntries().get(Constants.XTR_METADATA_KEY));
        } catch (OboeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        contextMetadata = new Metadata();
        contextMetadata.randomize(true);
        
        startEdge = contextMetadata.opHexString();
        
        metadata = new Metadata(contextMetadata);
        metadata.randomizeOpID();
        
        try {
            Event event = Context.createEventWithIDAndContext(metadata.toHexString(), contextMetadata);
            event.report(contextMetadata);
            
            DeserializedEvent sentEvent = getLastSentEvent();
            
            assertEquals(startEdge, sentEvent.getSentEntries().get(Constants.XTR_EDGE_KEY));
            assertEquals(metadata.toHexString(), sentEvent.getSentEntries().get(Constants.XTR_METADATA_KEY));
        } catch (OboeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    protected static Scope startTraceScope(String layerName) {
        return Tracer.INSTANCE
                .buildSpan(layerName)
                .withReporters(TraceEventSpanReporter.REPORTER)
                .withSpanProperty(SpanProperty.TRACE_DECISION_PARAMETERS, new TraceDecisionParameters(Collections.emptyMap(), "testUrl"))
                .startActive();  
    }
    
    protected static Scope startScope(String layerName) {
        return Tracer.INSTANCE.buildSpan(layerName).withReporters(TraceEventSpanReporter.REPORTER).startActive();
    }
    
    protected static Scope startScope(String layerName, boolean isAsync) {
        SpanBuilder spanBuilder = Tracer.INSTANCE.buildSpan(layerName).withReporters(TraceEventSpanReporter.REPORTER);
        if (isAsync) {
            spanBuilder = spanBuilder.withSpanProperty(SpanProperty.IS_ASYNC, true);
        }
        
        return spanBuilder.startActive();
    }
    
    
    
    private static void assertEdge(DeserializedEvent toEvent, DeserializedEvent fromEvent) throws Exception {
        Metadata parentMetadata = new Metadata((String) toEvent.getSentEntries().get(Constants.XTR_METADATA_KEY));
        Metadata childMetadata = new Metadata((String) fromEvent.getSentEntries().get(Constants.XTR_METADATA_KEY));
        
        assertEquals(parentMetadata.taskHexString(), childMetadata.taskHexString());
        
        Object edgeObject = fromEvent.getSentEntries().get(Constants.XTR_EDGE_KEY);
        Collection<String> edges;
        if (edgeObject instanceof String) {
            edges = new ArrayList<String>();
            edges.add((String) edgeObject);
        } else if (edgeObject instanceof Collection) {
            edges = (Collection<String>) edgeObject;
        } else {
            fail("KV edges is not String nor Collection : " + edgeObject.getClass().getName());
            edges = null;
        }
        
        assertTrue(edges.contains(parentMetadata.opHexString()));
    }
    
    private DeserializedEvent getLastSentEvent() {
        List<DeserializedEvent> sentEvents = tracingReporter.getSentEvents();
        
        return sentEvents.isEmpty() ? null : sentEvents.get(sentEvents.size() - 1);
    }
}
 