package com.solarwinds.joboe.span.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.concurrent.TimeUnit;
/**
 * Provides lookup of span with span ID, take note that the entry expires 10 minutes after it's inserted into the dictionary
 * @author pluk
 *
 */
public class SpanDictionary {
	private static final Cache<Long, Span> directory = CacheBuilder.newBuilder().expireAfterWrite(600, TimeUnit.SECONDS).build();
	
	public static long setSpan(Span span) {
	    long spanId = getId(span);
		directory.put(spanId, span);
		return spanId;
	}
	
	public static Span getSpan(long key) {
		return directory.getIfPresent(key);
	}
	
	public static void removeSpan(long key) {
		directory.invalidate(key);
	}
	
	public static void removeSpan(Span span) {
	    removeSpan(getId(span));
	}
	
	private static long getId(Span span) {
	    return span.context().getSpanId();
	}
}
