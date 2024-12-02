package com.solarwinds.joboe.sampling;

import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RequiredArgsConstructor
public class LruCache<K, V> extends LinkedHashMap<K,V> {

    private final long maximumSize;

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maximumSize;
    }

    @Override
    public Object clone() {
        Object clone = super.clone();
        return new LruCache<K, V>(maximumSize) {{
            putAll((HashMap<K, V>) clone);
        }};
    }
}
