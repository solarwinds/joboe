package com.tracelytics.joboe.span.impl;

import com.tracelytics.joboe.Metadata;
import com.tracelytics.joboe.span.impl.Span.TraceProperty;
import junit.framework.TestCase;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SpanTest extends TestCase {
	public void testGeneral() {
		final int START_TIME = 1;
		Metadata metadata = new Metadata();
		metadata.randomize();
		
		Map<String, String> baggages = new HashMap<String, String>();
		baggages.put("baggage1", "1");
		baggages.put("baggage2", "2");
		
		SpanContext spanContext = new SpanContext(metadata, 0l, 0l, null, (byte) 0, baggages, null);
		
		Map<String, Object> tags = new HashMap<String, Object>();
		tags.put("tag1", 1);
		tags.put("tag2", "2");
		tags.put("tag3", 3.0);
		
		
		Span span = new Span(Tracer.INSTANCE, "test", spanContext, START_TIME, tags, Collections.EMPTY_LIST);
		
		span.setTracePropertyValue(TraceProperty.HAS_ERROR, true);
		
		final int LOG_TIME = 2;
		Map<String, Object> logFields = new HashMap<String, Object>();
		logFields.put("test-key", "test-log-value");
		span.log(LOG_TIME, logFields);
		
		//test getter methods
		assertEquals(spanContext, span.context());
		assertEquals("1", span.getBaggageItem("baggage1"));
		assertEquals("test", span.getOperationName());
		assertEquals(true, (boolean)span.getTracePropertyValue(TraceProperty.HAS_ERROR));
		assertEquals(START_TIME, span.getStart());
		assertEquals(3.0, span.getTags().get("tag3"));
	}
	
	public void testTags() {
		final int START_TIME = 1;
		Metadata metadata = new Metadata();
		metadata.randomize();
		
		Map<String, String> baggages = new HashMap<String, String>();
		baggages.put("baggage1", "1");
		baggages.put("baggage2", "2");
		
		SpanContext spanContext = new SpanContext(metadata, 0l, 0l, 0l, (byte) 0, baggages, null);
		
		Map<String, Object> tags = new HashMap<String, Object>();
		tags.put("tag1", 1);
		tags.put("tag2", "2");
		tags.put("tag3", 3.0);
		
		
		Span span = new Span(Tracer.INSTANCE, "test", spanContext, START_TIME, tags, Collections.EMPTY_LIST);
		span.setTag("boolean", true);
		span.setTag("number", 5);
		span.setTag("string", "");
		Object object = new Object();
		span.setTagAsObject("object", object);
		
		assertEquals(1, span.getTags().get("tag1"));
		assertEquals("2", span.getTags().get("tag2"));
		assertEquals(3.0, span.getTags().get("tag3"));
		assertEquals(true, span.getTags().get("boolean"));
		assertEquals(5, span.getTags().get("number"));
		assertEquals("", span.getTags().get("string"));
		assertEquals(object, span.getTags().get("object"));
	}
	
	public void testBaggage() {
		final int START_TIME = 1;
		Metadata metadata = new Metadata();
		metadata.randomize();
		
		Map<String, String> baggages = new HashMap<String, String>();
		baggages.put("baggage1", "1");
		baggages.put("baggage2", "2");
		
		SpanContext spanContext = new SpanContext(metadata, 0l, 0l, 0l, (byte) 0, baggages, null);
		
		Map<String, Object> tags = new HashMap<String, Object>();
		
		Span span = new Span(Tracer.INSTANCE, "test", spanContext, START_TIME, tags, Collections.EMPTY_LIST);
		
		span.setBaggageItem("baggage3", "3");
		
		assertEquals("1", span.getBaggageItem("baggage1"));
		assertEquals("2", span.getBaggageItem("baggage2"));
		assertEquals("3", span.getBaggageItem("baggage3"));
	}
}