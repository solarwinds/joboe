package com.solarwinds.joboe.core.span.impl;

import com.solarwinds.joboe.core.Constants;
import com.solarwinds.joboe.core.Context;
import com.solarwinds.joboe.core.Event;
import com.solarwinds.joboe.core.ReporterFactory;
import com.solarwinds.joboe.core.TestReporter;
import com.solarwinds.joboe.core.TestReporter.DeserializedEvent;
import com.solarwinds.joboe.core.settings.TestSettingsReader;
import com.solarwinds.joboe.core.span.impl.Span.SpanProperty;
import com.solarwinds.joboe.core.span.impl.Tracer.SpanBuilder;
import com.solarwinds.joboe.core.util.TestUtils;
import com.solarwinds.joboe.sampling.Metadata;
import com.solarwinds.joboe.sampling.SamplingException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests on context management for new span/legacy event-based span 
 * @author Patson
 *
 */
public class ContextTest {

    protected static final TestSettingsReader testSettingsReader = TestUtils.initSettingsReader();

    @BeforeEach
    protected void setup() throws Exception {
        testSettingsReader.reset();
        testSettingsReader.put(TestUtils.getDefaultSettings());
        Context.clearMetadata();
        ScopeManager.INSTANCE.removeAllScopes();
    }
    
    @AfterEach
    protected void teardown() throws Exception {
        testSettingsReader.reset();
        ScopeManager.INSTANCE.removeAllScopes();
        Context.clearMetadata(); //clear context
    }

    @Test
    public void testNestedSpans() throws Exception {
        TestReporter testReporter = ReporterFactory.getInstance().createTestReporter();
        TraceEventSpanReporter traceEventSpanReporter = new TraceEventSpanReporter(testReporter);
        Scope scope1 = startTraceScope("span1", traceEventSpanReporter);
        Span span1 = scope1.span();
        DeserializedEvent span1EntryEvent = getLastSentEvent(testReporter);
        
        span1.setTag("URL", "nested-spans");
        span1.log("test", 2);
        DeserializedEvent span1InfoEvent = getLastSentEvent(testReporter);
        assertEdge(span1EntryEvent, span1InfoEvent);
        
        Scope scope2 = startScope("span2", traceEventSpanReporter);
        Span span2 = scope2.span();
        DeserializedEvent span2EntryEvent = getLastSentEvent(testReporter);
        assertEdge(span1InfoEvent, span2EntryEvent); //span 2 entry should point to the span 1 info event
        
        span2.log("test", 2);
        DeserializedEvent span2InfoEvent = getLastSentEvent(testReporter);
        assertEdge(span2EntryEvent, span2InfoEvent);
        
        Scope scope3 = startScope("span3", traceEventSpanReporter);
        DeserializedEvent span3EntryEvent = getLastSentEvent(testReporter);
        assertEdge(span2InfoEvent, span3EntryEvent); //span 3 entry should point to the span 2 info event
        
        scope3.close();
        DeserializedEvent span3ExitEvent = getLastSentEvent(testReporter);
        assertEdge(span3EntryEvent, span3ExitEvent);
        
        scope2.close();
        DeserializedEvent span2ExitEvent = getLastSentEvent(testReporter);
        assertEdge(span2InfoEvent, span2ExitEvent);
        assertEdge(span3ExitEvent, span2ExitEvent);
        
        scope1.close();
        DeserializedEvent span1ExitEvent = getLastSentEvent(testReporter);
        assertEdge(span1InfoEvent, span1ExitEvent);
        assertEdge(span2ExitEvent, span1ExitEvent);

        assertFalse(Context.isValid()); //should clear all context
    }

    @Test
    public void testSiblingSpans() throws Exception {
        TestReporter testReporter = ReporterFactory.getInstance().createTestReporter();
        TraceEventSpanReporter traceEventSpanReporter = new TraceEventSpanReporter(testReporter);
        Scope scope1 = startTraceScope("span1", traceEventSpanReporter);
        Span span1 = scope1.span();
        DeserializedEvent span1EntryEvent = getLastSentEvent(testReporter);
        
        span1.setTag("URL", "sibling-spans");
        
        Scope scope2 = startScope("span2", traceEventSpanReporter);
        DeserializedEvent span2EntryEvent = getLastSentEvent(testReporter);
        assertEdge(span1EntryEvent, span2EntryEvent); //span 2 entry should point to the span 1 entry event
        
        scope2.close();
        DeserializedEvent span2ExitEvent = getLastSentEvent(testReporter);
        assertEdge(span2EntryEvent, span2ExitEvent);
        
        span1.log("test", 1);
        DeserializedEvent span1InfoEvent = getLastSentEvent(testReporter);
        assertEdge(span1EntryEvent, span1InfoEvent);
        
        Scope scope3 = startScope("span3", traceEventSpanReporter);
        DeserializedEvent span3EntryEvent = getLastSentEvent(testReporter);
        assertEdge(span1InfoEvent, span3EntryEvent); //span 3 entry should point to the span 1 info event
        
        scope3.close();
        DeserializedEvent span3ExitEvent = getLastSentEvent(testReporter);
        assertEdge(span3EntryEvent, span3ExitEvent);
        
        scope1.close();
        DeserializedEvent span1ExitEvent = getLastSentEvent(testReporter);
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
        TestReporter tracingReporter = TestUtils.initTraceReporter();
        legacyRootEntry.report(tracingReporter);
        DeserializedEvent span1EntryEvent = getLastSentEvent(tracingReporter);
        
        Scope scope = startScope("span2", new TraceEventSpanReporter(tracingReporter)); //a span nested below a legacy span
        DeserializedEvent span2EntryEvent = getLastSentEvent(tracingReporter);
        assertEdge(span1EntryEvent, span2EntryEvent); //span 2 entry should point to the span 1 entry event
        
        Event legacyEntry = Context.createEvent();
        legacyEntry.addInfo(
                "Layer", "legacy-span3",
                "Label", "entry");
        legacyEntry.report(tracingReporter);
        DeserializedEvent span3EntryEvent = getLastSentEvent(tracingReporter);
        assertEdge(span2EntryEvent, span3EntryEvent); //legacy event should "inline" into current active span, which is span 2
        
        Event legacyExit = Context.createEvent();
        legacyExit.addInfo(
                "Layer", "legacy-span3",
                "Label", "exit");
        legacyExit.report(tracingReporter);
        DeserializedEvent span3ExitEvent = getLastSentEvent(tracingReporter);
        assertEdge(span3EntryEvent, span3ExitEvent);
        
        scope.close();
        DeserializedEvent span2ExitEvent = getLastSentEvent(tracingReporter);
        assertEdge(span3ExitEvent, span2ExitEvent); //legacy event should "inline" into current active span, which is span 2
        
        Event legacyRootExit = Context.createEvent(); //a root span with legacy event-based approach
        legacyRootExit.addInfo("Layer", "legacy-span1",
                "Label", "exit");
        legacyRootExit.report(tracingReporter);
        DeserializedEvent span1ExitEvent = getLastSentEvent(tracingReporter);
        assertEdge(span1EntryEvent, span1ExitEvent);
    }

    @Test
    public void testSpanLegacySpan() throws Exception {
        TestReporter tracingReporter = TestUtils.initTraceReporter();
        Scope scope1 = startTraceScope("span1", new TraceEventSpanReporter(tracingReporter));
        Span span1 = scope1.span();
        DeserializedEvent span1EntryEvent = getLastSentEvent(tracingReporter);
        
        span1.setTag("URL", "span-legacy-span");
        span1.log("test", 2);
        DeserializedEvent span1InfoEvent = getLastSentEvent(tracingReporter);
        assertEdge(span1EntryEvent, span1InfoEvent);
        
        Event legacyEntry = Context.createEvent();
        legacyEntry.addInfo(
                "Layer", "legacy-span2",
                "Label", "entry");
        legacyEntry.report(tracingReporter);
        DeserializedEvent span2EntryEvent = getLastSentEvent(tracingReporter);
        assertEdge(span1InfoEvent, span2EntryEvent); //span 2 entry should "inline" into active span 1, hence pointing at span 1's info event
        
        Scope scope3 = startTraceScope("span3", new TraceEventSpanReporter(tracingReporter));
        DeserializedEvent span3EntryEvent = getLastSentEvent(tracingReporter);
        assertEdge(span2EntryEvent, span3EntryEvent);
        
        scope3.close();
        DeserializedEvent span3ExitEvent = getLastSentEvent(tracingReporter);
        assertEdge(span3EntryEvent, span3ExitEvent);
        
        Event legacyExit = Context.createEvent();
        legacyExit.addInfo(
                "Layer", "legacy-span2",
                "Label", "exit");
        legacyExit.report(tracingReporter);
        DeserializedEvent span2ExitEvent = getLastSentEvent(tracingReporter);
        assertEdge(span2EntryEvent, span2ExitEvent); //legacy span 2 exit should point directly back to its entry instead of span 3, as span 3 is a "branch"
        
        scope1.close();
        DeserializedEvent span1ExitEvent = getLastSentEvent(tracingReporter);
        assertEdge(span2ExitEvent, span1ExitEvent); //span 1 exit should point to legacy span 2 exit, as span 2 is "inline" into span 1
        assertEdge(span3ExitEvent, span1ExitEvent); //span 1 exit should point to span 3 exit as span 3 is a child of span 1


        assertFalse(Context.isValid()); //should clear all context
    }

    @Test
    public void testAsyncSpans() throws Throwable {
        final TestReporter threadLocalReporter = ReporterFactory.getInstance().createTestReporter(true);

        TraceEventSpanReporter traceEventSpanReporter = new TraceEventSpanReporter(threadLocalReporter);
        Scope scope1 = startTraceScope("span1", traceEventSpanReporter);
        Span span1 = scope1.span();
        DeserializedEvent span1EntryEvent = getLastSentEvent(threadLocalReporter);
        
        span1.setTag("URL", "async-spans");
        span1.log("test", 2);
        final DeserializedEvent span1InfoEvent = getLastSentEvent(threadLocalReporter);
        assertEdge(span1EntryEvent, span1InfoEvent);
        
        final List<Throwable> asyncErrors = Collections.synchronizedList(new ArrayList<Throwable>());
        
        
        Thread thread1 = new Thread(() -> {
            try {
                Scope scope2 = startScope("async-span2", true, traceEventSpanReporter);
                Span span2 = scope2.span();
                DeserializedEvent span2EntryEvent = getLastSentEvent(threadLocalReporter);
                assertEdge(span1InfoEvent, span2EntryEvent);

                span2.log("test", 2);
                DeserializedEvent span2InfoEvent = getLastSentEvent(threadLocalReporter);
                assertEdge(span2EntryEvent, span2InfoEvent);

                Scope scope4 = startScope("span4", traceEventSpanReporter);
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
        });
        thread1.start();
        
        Thread thread2 = new Thread(() -> {
            try {
                Scope scope3 = startScope("async-span3", true, traceEventSpanReporter);
                Span span3 = scope3.span();
                DeserializedEvent span3EntryEvent = getLastSentEvent(threadLocalReporter);
                assertEdge(span1InfoEvent, span3EntryEvent);

                span3.log("test", 3);
                DeserializedEvent span3InfoEvent = getLastSentEvent(threadLocalReporter);
                assertEdge(span3EntryEvent, span3InfoEvent);

                Scope scope5 = startScope("span5", traceEventSpanReporter);
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
        });
        
        thread2.start();
        
        scope1.close();
        DeserializedEvent span1ExitEvent = getLastSentEvent(threadLocalReporter);
        assertEdge(span1InfoEvent, span1ExitEvent);
        assertFalse(Context.isValid()); //should clear all context
        
        thread1.join();
        thread2.join();
        
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
            TestReporter testReporter = ReporterFactory.getInstance().createTestReporter();
            Event event = Context.createEventWithID(metadata.toHexString());
            event.report(testReporter);
            
            DeserializedEvent sentEvent = getLastSentEvent(testReporter);
            
            assertEquals(startEdge, sentEvent.getSentEntries().get(Constants.XTR_EDGE_KEY));
            assertEquals(metadata.toHexString(), sentEvent.getSentEntries().get(Constants.XTR_METADATA_KEY));
        } catch (SamplingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        contextMetadata = new Metadata();
        contextMetadata.randomize(true);
        
        startEdge = contextMetadata.opHexString();
        
        metadata = new Metadata(contextMetadata);
        metadata.randomizeOpID();
        
        try {
            TestReporter testReporter = ReporterFactory.getInstance().createTestReporter();
            Event event = Context.createEventWithIDAndContext(metadata.toHexString(), contextMetadata);
            event.report(contextMetadata, testReporter);
            
            DeserializedEvent sentEvent = getLastSentEvent(testReporter);
            
            assertEquals(startEdge, sentEvent.getSentEntries().get(Constants.XTR_EDGE_KEY));
            assertEquals(metadata.toHexString(), sentEvent.getSentEntries().get(Constants.XTR_METADATA_KEY));
        } catch (SamplingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    protected static Scope startTraceScope(String layerName, SpanReporter spanReporter) {
        return Tracer.INSTANCE
                .buildSpan(layerName)
                .withReporters(spanReporter)
                .withSpanProperty(SpanProperty.TRACE_DECISION_PARAMETERS, new TraceDecisionParameters(Collections.emptyMap(), "testUrl"))
                .startActive();  
    }
    
    protected static Scope startScope(String layerName, SpanReporter spanReporter) {
        return Tracer.INSTANCE.buildSpan(layerName)
                .withReporters(spanReporter)
                .startActive();
    }
    
    protected static Scope startScope(String layerName, boolean isAsync, SpanReporter spanReporter) {
        SpanBuilder spanBuilder = Tracer.INSTANCE.buildSpan(layerName).withReporters(spanReporter);
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
}
 