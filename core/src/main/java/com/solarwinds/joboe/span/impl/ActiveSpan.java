package com.solarwinds.joboe.span.impl;

/**
 * @deprecated Use {@link Scope} instead
 * @author pluk
 *
 */
public class ActiveSpan implements BaseSpan {
    private final Scope wrapped;

    public ActiveSpan(Scope scope) {
        this.wrapped = scope;
    }
    
    public String getOperationName() {
        return wrapped.span().getOperationName();
    }
    
    public <V> V getSpanPropertyValue(SpanProperty<V> property) {
        return wrapped.span().getSpanPropertyValue(property.getWrapped());
    }
    
    public void deactivate() {
        wrapped.close();
    }
    
    public ActiveSpan setTagAsObject(String key, Object value) {
        wrapped.span().setTagAsObject(key, value);
        return this;
    }
    
    public <V> void setSpanPropertyValue(SpanProperty<V> property, V value) {
        wrapped.span().setSpanPropertyValue(property.getWrapped(), value);
    }
    
    public Scope getWrapped() {
        return wrapped;
    }
}
