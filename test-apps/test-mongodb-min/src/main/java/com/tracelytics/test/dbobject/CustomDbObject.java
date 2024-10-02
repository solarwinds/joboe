package com.tracelytics.test.dbobject;

import com.mongodb.DBObject;
import org.bson.BSONObject;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CustomDbObject implements DBObject {
    private Map<String, Object> data = new ConcurrentHashMap<String, Object>();
    private boolean isPartialObject = false;

    public CustomDbObject(Map<String, ?> data) {
        putAll(data);
    }

    public CustomDbObject append(String key, Object v) {
        put(key, v);
        return this;
    }

    public void markAsPartialObject() {
        this.isPartialObject = true;
    }

    public boolean isPartialObject() {
        return isPartialObject;
    }

    public Object put(String key, Object v) {
        return data.put(key.toUpperCase(), v);
    }

    public void putAll(BSONObject o) {
        for (String key : o.keySet()) {
            put(key, o.get(key));
        }
    }

    public void putAll(Map m) {
        for (Object entryObject : m.entrySet()) {
            Map.Entry<String, Object> entry = (Map.Entry<String, Object>) entryObject;
            put(entry.getKey().toUpperCase(), entry.getValue());
        }
    }

    public Object get(String key) {
        return data.get(key.toUpperCase());
    }

    public Map toMap() {
        return Collections.unmodifiableMap(data);
    }

    public Object removeField(String key) {
        return data.remove(key.toUpperCase());
    }

    public boolean containsKey(String s) {
        return data.containsKey(s.toUpperCase());
    }

    public boolean containsField(String s) {
        return data.containsKey(s.toUpperCase());
    }

    public Set<String> keySet() {
        return toMap().keySet();
    }
}
