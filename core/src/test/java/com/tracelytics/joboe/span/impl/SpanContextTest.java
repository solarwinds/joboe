package com.tracelytics.joboe.span.impl;

import com.tracelytics.joboe.Metadata;
import junit.framework.TestCase;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SpanContextTest extends TestCase {
    private static final Random random = new Random();
    
	public void testConversion() {
		Metadata metadata = new Metadata();
		metadata.randomize();
		
		SpanContext sourceContext = new SpanContext(metadata, random.nextLong(), random.nextLong(), random.nextLong(), (byte) 0, Collections.<String,String>emptyMap(), null);
		
		assertEquals(sourceContext, SpanContext.contextFromString(sourceContext.contextAsString()));
	}
	
	public void testBaggages() {
		Metadata metadata = new Metadata();
		metadata.randomize();
		
		Map<String, String> baggages = new HashMap<String, String>();
		baggages.put("baggage1", "1");
		baggages.put("baggage2", "2");
		
		SpanContext sourceContext = new SpanContext(metadata, random.nextLong(), random.nextLong(), random.nextLong(), (byte) 0, baggages, null);
		
		assertEquals("1", sourceContext.getBaggageItem("baggage1"));
		assertEquals("2", sourceContext.getBaggageItem("baggage2"));
		
		sourceContext = sourceContext.withBaggageItem("baggage3", "3");
		
		assertEquals("1", sourceContext.getBaggageItem("baggage1"));
		assertEquals("2", sourceContext.getBaggageItem("baggage2"));
		assertEquals("3", sourceContext.getBaggageItem("baggage3"));
		assertEquals(metadata, sourceContext.getMetadata());
	}
	
	public void testFlags() {
		Metadata metadata = new Metadata();
		metadata.randomize();
		
		byte flags = 123;
		
		SpanContext spanContext = new SpanContext(metadata, random.nextLong(), random.nextLong(), random.nextLong(), flags, Collections.<String,String>emptyMap(), null);
		
		assertEquals(flags, spanContext.getFlags());
	}
}
