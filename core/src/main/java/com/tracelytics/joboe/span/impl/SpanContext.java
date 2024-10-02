package com.tracelytics.joboe.span.impl;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.tracelytics.joboe.Metadata;
import com.tracelytics.joboe.OboeException;

/**
 * Implements OpenTracing's SpanContext interface
 * @author pluk
 *
 */
public class SpanContext implements com.tracelytics.joboe.span.SpanContext {
    private final long traceId;
    private final long spanId; 
    private final Long parentId;
    
    private final byte flags;
    private final Map<String, String> baggage = new HashMap<String, String>();
    
    private final Metadata metadata;
    private final Metadata previousMetadata;

    private Metadata entryMetadata;
    
    /**
     * 
     * @param metadata  Metadata to be associated with this SpanContext. Take note that this metadata is used by {@link SpanReporter} for various operations. 
     * For example {@link TraceEventSpanReporter} use the metadata to build edges and x-trace IDs 
     * 
     * This metadata instance might be updated by the {@link TraceEventSpanReporter} or any other non-span instrumentation during event reporting,
     * however the reference itself should be final once the SpanContext is instantiated
     *  
     * @param traceId
     * @param spanId
     * @param parentId
     * @param flags
     * @param baggage
     */
    public SpanContext(Metadata metadata, long traceId, long spanId, Long parentId, byte flags, Map<String, String> baggage, Metadata previousMetadata) {
        if (baggage == null) {
            throw new NullPointerException();
        }
        this.metadata = metadata;
        this.traceId = traceId;
        this.spanId = spanId;
        this.parentId = parentId;
        this.previousMetadata = previousMetadata;
        

        this.flags = flags;
        this.baggage.putAll(baggage);
    }

    public Iterable<Map.Entry<String, String>> baggageItems() {
        return Collections.unmodifiableSet(baggage.entrySet());
    }

    public String getBaggageItem(String key) {
        return this.baggage.get(key);
    }

    Map<String, String> getBaggage() {
        return this.baggage;
    }

    public long getTraceId() {
        return traceId;
    }

    public long getSpanId() {
        return spanId;
    }

    public Long getParentId() {
        return parentId;
    }

    public byte getFlags() {
        return flags;
    }
    
    public Metadata getPreviousMetadata() {
        return previousMetadata;
    }

    /**
     *  keep a reference on the state of the entry metadata, as otherwise it will be lost when the metadata get updated
     */
    public void markEntryMetadata() {
        this.entryMetadata = new Metadata(metadata);
    }

    public Metadata getEntryMetadata() {
        return entryMetadata;
    }

    /**
     * Indicates whether this span is sampled for tracing. This is specific to {@link TraceEventSpanReporter}
     * @return
     */
    public boolean isSampled() {
        return getMetadata().isSampled();
    }
    
    
    /**
     * Serializes the context into String format. Take note that baggage is NOT serialized for now
     * @return
     */
    public String contextAsString() {
        return String.format("%s:%x:%x:%x:%x", metadata.toHexString(), traceId, spanId, parentId, flags);
    }

    @Override
    public String toString() {
        return contextAsString();
    }

    /**
     * Deserialize a string back to a SpanContext
     * @param value
     * @return
     */
	public static SpanContext contextFromString(String value) {
		if (value == null || value.equals("")) {
			throw new IllegalArgumentException();
		}

		String[] parts = value.split(":");
		if (parts.length != 5) {
			throw new IllegalArgumentException(value);
		}

		try {
			return new SpanContext(new Metadata(parts[0]), 
			                       new BigInteger(parts[1], 16).longValue(), 
			                       new BigInteger(parts[2], 16).longValue(), 
			                       new BigInteger(parts[3], 16).longValue(), 
			                       new BigInteger(parts[4], 16).byteValue(),  
			                       Collections.<String, String>emptyMap(),
			                       null);
		} catch (OboeException e) {
			throw new IllegalArgumentException(value, e);
		}
	}

    public SpanContext withBaggageItem(String key, String val) {
        Map<String, String> newBaggage = new HashMap<String, String>(this.baggage);
        newBaggage.put(key, val);
        return new SpanContext(metadata, traceId, spanId, parentId, flags, newBaggage, previousMetadata);
    }

    public SpanContext withBaggage(Map<String, String> newBaggage) {
        return new SpanContext(metadata, traceId, spanId, parentId, flags, newBaggage, previousMetadata);
    }

    public SpanContext withFlags(byte flags) {
        return new SpanContext(metadata, traceId, spanId, parentId, flags, baggage, previousMetadata);
    }

    public Metadata getMetadata() {
        return metadata;
    }
    

    
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((baggage == null) ? 0 : baggage.hashCode());
		result = prime * result + flags;
		result = prime * result + ((metadata == null) ? 0 : metadata.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SpanContext other = (SpanContext) obj;
		if (baggage == null) {
			if (other.baggage != null)
				return false;
		} else if (!baggage.equals(other.baggage))
			return false;
		if (flags != other.flags)
			return false;
		if (metadata == null) {
			if (other.metadata != null)
				return false;
		} else if (!metadata.equals(other.metadata))
			return false;
		return true;
	}

    @Override
    public String toTraceId() {
        return String.valueOf(getTraceId());
    }

    @Override
    public String toSpanId() {
        return String.valueOf(getSpanId());
    }
}
