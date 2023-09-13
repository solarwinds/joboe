package com.tracelytics.joboe.span.impl;

import com.tracelytics.joboe.Metadata;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SpanContextTest {
    private static final Random random = new Random();

	@Test
	public void testConversion() {
		Metadata metadata = new Metadata();
		metadata.randomize();
		
		SpanContext sourceContext = new SpanContext(metadata, random.nextLong(), random.nextLong(), random.nextLong(), (byte) 0, Collections.<String,String>emptyMap(), null);
		
		assertEquals(sourceContext, SpanContext.contextFromString(sourceContext.contextAsString()));
	}

	@Test
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

	@Test
	public void testFlags() {
		Metadata metadata = new Metadata();
		metadata.randomize();
		
		byte flags = 123;
		
		SpanContext spanContext = new SpanContext(metadata, random.nextLong(), random.nextLong(), random.nextLong(), flags, Collections.<String,String>emptyMap(), null);
		
		assertEquals(flags, spanContext.getFlags());
	}
}
