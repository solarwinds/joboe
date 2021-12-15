package com.tracelytics.joboe.span.impl;

import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.EventImpl;
import com.tracelytics.joboe.EventReporter;
import com.tracelytics.joboe.SampleRateSource;
import com.tracelytics.joboe.TestReporter;
import com.tracelytics.joboe.TestReporter.DeserializedEvent;
import com.tracelytics.joboe.XTraceHeader;
import com.tracelytics.joboe.settings.SettingsArg;
import com.tracelytics.joboe.settings.TestSettingsReader;
import com.tracelytics.joboe.span.impl.Span.SpanProperty;
import com.tracelytics.util.TestUtils;
import junit.framework.TestCase;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class TraceEventReporterTest extends TestCase {
    private static final String TEST_LAYER = "test";
	private static final TestReporter tracingReporter = TestUtils.initTraceReporter();
	private EventReporter originalReporter;
	private static TestSettingsReader reader;
	public TraceEventReporterTest() throws SecurityException, NoSuchFieldException {
    }

    static {
		reader = TestUtils.initSettingsReader();
	}

    @Override
    protected void setUp() throws Exception {
        super.setUp();

		reader.put(TestUtils.getDefaultSettings());

		originalReporter = EventImpl.setDefaultReporter(tracingReporter);
		Context.getMetadata().randomize(true); //set context to be sampled
    }

	@Override
	protected void tearDown() throws Exception {
	    ScopeManager.INSTANCE.removeAllScopes();
		Context.clearMetadata(); //clear context
		tracingReporter.reset();
		reader.reset();
		EventImpl.setDefaultReporter(originalReporter);

        super.tearDown();
	}


	public void testReportSimpleSpan() {
		List<DeserializedEvent> sentEvents;
		Scope scope = Tracer.INSTANCE.buildSpan(TEST_LAYER).withReporters(TraceEventSpanReporter.REPORTER).startActive();
        Span span = scope.span();

		assertTrue(span.context().isSampled());
	    assertTrue(Context.getMetadata().isValid()); //should initialize the context

	    sentEvents = tracingReporter.getSentEvents();
	    tracingReporter.reset();

		assertEquals(1, sentEvents.size());
		assertEquals("entry", sentEvents.get(0).getSentEntries().get("Label"));
		assertEquals(TEST_LAYER, sentEvents.get(0).getSentEntries().get("Layer"));


		span.log(Collections.singletonMap("log-key", "log-value"));
		sentEvents = tracingReporter.getSentEvents();
		tracingReporter.reset();

		assertEquals(1, sentEvents.size());
        assertEquals("info", sentEvents.get(0).getSentEntries().get("Label"));
        assertEquals("log-value", sentEvents.get(0).getSentEntries().get("log-key"));

		scope.close();
		sentEvents = tracingReporter.getSentEvents();
		tracingReporter.reset();

		assertEquals(1, sentEvents.size());
		assertEquals("exit", sentEvents.get(0).getSentEntries().get("Label"));
		assertEquals(TEST_LAYER, sentEvents.get(0).getSentEntries().get("Layer"));
	}

	public void testReportSpanWithLogEntries() {
		List<DeserializedEvent> sentEvents;
		Span span = Tracer.INSTANCE.buildSpan("with-log-entries").withReporters(TraceEventSpanReporter.REPORTER).startManual();

		sentEvents = tracingReporter.getSentEvents();
		tracingReporter.reset();

		assertEquals(1, sentEvents.size());
		assertEquals("entry", sentEvents.get(0).getSentEntries().get("Label"));
		assertEquals("with-log-entries", sentEvents.get(0).getSentEntries().get("Layer"));

		Map<String, Object> log1Entries = new HashMap<String, Object>();
		log1Entries.put("1", 1);
		log1Entries.put("2", "2");
		log1Entries.put("3", Collections.singletonMap("k3", "v3")); //take note that using List and Set will fail the test case as the BSON builder implicitly converts them to Maps

		Map<String, Object> log2Entries = new HashMap<String, Object>();
		log1Entries.put("4", 4.0);
		log1Entries.put("5", 5l);

		span.log(log1Entries);
		span.log(log2Entries);


		span.finish();
		sentEvents = tracingReporter.getSentEvents();
		tracingReporter.reset();

		assertEquals(3, sentEvents.size());
		assertEquals("info", sentEvents.get(0).getSentEntries().get("Label"));
		assertEquals("with-log-entries", sentEvents.get(0).getSentEntries().get("Layer"));
		for (Entry<String, Object> expectedEntry : log1Entries.entrySet()) {
			assertEquals(expectedEntry.getValue(), sentEvents.get(0).getSentEntries().get(expectedEntry.getKey()));
		}

		assertEquals("info", sentEvents.get(1).getSentEntries().get("Label"));
		assertEquals("with-log-entries", sentEvents.get(1).getSentEntries().get("Layer"));
		for (Entry<String, Object> expectedEntry : log2Entries.entrySet()) {
			assertEquals(expectedEntry.getValue(), sentEvents.get(1).getSentEntries().get(expectedEntry.getKey()));
		}

		assertEquals("exit", sentEvents.get(2).getSentEntries().get("Label"));
		assertEquals("with-log-entries", sentEvents.get(2).getSentEntries().get("Layer"));
	}

	public void testReportSpanWithExplicitTimestamps() {
		List<DeserializedEvent> sentEvents;

		final long START_TIME = 1L;
		Span span = Tracer.INSTANCE.buildSpan("explicit-timestamp").withReporters(TraceEventSpanReporter.REPORTER).withStartTimestamp(START_TIME).startManual();

		sentEvents = tracingReporter.getSentEvents();
		tracingReporter.reset();

		assertEquals(1, sentEvents.size());
		assertEquals("entry", sentEvents.get(0).getSentEntries().get("Label"));
		assertEquals("explicit-timestamp", sentEvents.get(0).getSentEntries().get("Layer"));
		assertEquals(START_TIME, sentEvents.get(0).getSentEntries().get("Timestamp_u"));

		final long LOG_TIME = 2L;
		span.log(LOG_TIME, Collections.singletonMap("something", "value"));


		final long FINISH_TIME = 3L;
		span.finish(FINISH_TIME);
		sentEvents = tracingReporter.getSentEvents();
		tracingReporter.reset();

		assertEquals(2, sentEvents.size());
		assertEquals("info", sentEvents.get(0).getSentEntries().get("Label"));
		assertEquals("explicit-timestamp", sentEvents.get(0).getSentEntries().get("Layer"));
		assertEquals(LOG_TIME, sentEvents.get(0).getSentEntries().get("Timestamp_u"));

		assertEquals("exit", sentEvents.get(1).getSentEntries().get("Label"));
		assertEquals("explicit-timestamp", sentEvents.get(1).getSentEntries().get("Layer"));
		assertEquals(FINISH_TIME, sentEvents.get(1).getSentEntries().get("Timestamp_u"));
	}

	public void testReportTracingKvs() {
	    Context.clearMetadata(); //make this a new trace
	    TraceDecisionParameters traceDecisionParameters = new TraceDecisionParameters(Collections.EMPTY_MAP, null);

	    List<DeserializedEvent> sentEvents;
	    Scope scope = Tracer.INSTANCE
				.buildSpan(TEST_LAYER)
				.withReporters(TraceEventSpanReporter.REPORTER)
				.withSpanProperty(SpanProperty.TRACE_DECISION_PARAMETERS, traceDecisionParameters)
				.startActive();

        sentEvents = tracingReporter.getSentEvents();
        tracingReporter.reset();

        assertEquals(1, sentEvents.size());
        assertEquals("entry", sentEvents.get(0).getSentEntries().get("Label"));
        assertEquals(TEST_LAYER, sentEvents.get(0).getSentEntries().get("Layer"));
        assertEquals((int) TestUtils.getDefaultSettings().getValue(), sentEvents.get(0).getSentEntries().get("SampleRate"));
        assertEquals(SampleRateSource.OBOE_DEFAULT.value(), sentEvents.get(0).getSentEntries().get("SampleSource"));
        assertEquals(TestUtils.getDefaultSettings().getArgValue(SettingsArg.BUCKET_CAPACITY), sentEvents.get(0).getSentEntries().get("BucketCapacity"));
        assertEquals(TestUtils.getDefaultSettings().getArgValue(SettingsArg.BUCKET_RATE), sentEvents.get(0).getSentEntries().get("BucketRate"));

        scope.close();

        sentEvents = tracingReporter.getSentEvents();
        tracingReporter.reset();
        assertEquals(1, sentEvents.size());
        assertEquals("exit", sentEvents.get(0).getSentEntries().get("Label"));
        assertEquals(TEST_LAYER, sentEvents.get(0).getSentEntries().get("Layer"));
        //ensure there are no trace decision KVs in the exit event
        assertEquals(null, sentEvents.get(0).getSentEntries().get("SampleRate"));
        assertEquals(null, sentEvents.get(0).getSentEntries().get("SampleSource"));
        assertEquals(null, sentEvents.get(0).getSentEntries().get("BucketCapacity"));
        assertEquals(null, sentEvents.get(0).getSentEntries().get("BucketRate"));
	}

	public void testTriggerTraceKvs() {
		Context.clearMetadata(); //make this a new trace
		Map<XTraceHeader, String> xTraceHeaders = Collections.singletonMap(XTraceHeader.TRACE_OPTIONS, "trigger-trace;sw-keys=lo:se;custom-key1=value1;custom-key2=value2");
		TraceDecisionParameters traceDecisionParameters = new TraceDecisionParameters(xTraceHeaders, null);

		List<DeserializedEvent> sentEvents;
		Scope scope = Tracer.INSTANCE
				.buildSpan(TEST_LAYER)
				.withReporters(TraceEventSpanReporter.REPORTER)
				.withSpanProperty(SpanProperty.TRACE_DECISION_PARAMETERS, traceDecisionParameters)
				.startActive();

		sentEvents = tracingReporter.getSentEvents();
		tracingReporter.reset();

		assertEquals(1, sentEvents.size());
		assertEquals("entry", sentEvents.get(0).getSentEntries().get("Label"));
		assertEquals(TEST_LAYER, sentEvents.get(0).getSentEntries().get("Layer"));
		assertEquals(-1, sentEvents.get(0).getSentEntries().get("SampleRate"));
		assertEquals(-1, sentEvents.get(0).getSentEntries().get("SampleSource"));
		assertEquals(TestUtils.getDefaultSettings().getArgValue(SettingsArg.BUCKET_CAPACITY), sentEvents.get(0).getSentEntries().get("BucketCapacity"));
		assertEquals(TestUtils.getDefaultSettings().getArgValue(SettingsArg.BUCKET_RATE), sentEvents.get(0).getSentEntries().get("BucketRate"));
		assertEquals("lo:se", sentEvents.get(0).getSentEntries().get("SWKeys"));
		assertEquals(true, sentEvents.get(0).getSentEntries().get("TriggeredTrace"));
		assertEquals("value1", sentEvents.get(0).getSentEntries().get("custom-key1"));
		assertEquals("value2", sentEvents.get(0).getSentEntries().get("custom-key2"));

		scope.close();

		sentEvents = tracingReporter.getSentEvents();
		tracingReporter.reset();
		assertEquals(1, sentEvents.size());
		assertEquals("exit", sentEvents.get(0).getSentEntries().get("Label"));
		assertEquals(TEST_LAYER, sentEvents.get(0).getSentEntries().get("Layer"));
		//ensure there are no trace decision/X-Trace-Options KVs in the exit event
		assertEquals(null, sentEvents.get(0).getSentEntries().get("SampleRate"));
		assertEquals(null, sentEvents.get(0).getSentEntries().get("SampleSource"));
		assertEquals(null, sentEvents.get(0).getSentEntries().get("BucketCapacity"));
		assertEquals(null, sentEvents.get(0).getSentEntries().get("BucketRate"));
		assertEquals(null, sentEvents.get(0).getSentEntries().get("SWKeys"));
		assertEquals(null, sentEvents.get(0).getSentEntries().get("TriggeredTrace"));
		assertEquals(null, sentEvents.get(0).getSentEntries().get("custom-key1"));
		assertEquals(null, sentEvents.get(0).getSentEntries().get("custom-key2"));
	}

}
