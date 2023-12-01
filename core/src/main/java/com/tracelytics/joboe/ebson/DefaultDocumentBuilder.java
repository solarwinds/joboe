package com.tracelytics.joboe.ebson;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import javax.annotation.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

final class DefaultDocumentBuilder implements BsonDocument.Builder {

  private final Map<String, Object> builder;

  DefaultDocumentBuilder() {
    builder = Maps.newLinkedHashMap();
  }

  @Override
  public BsonDocument.Builder putAll(Map<String, Object> map) {
    Preconditions.checkNotNull(map, "null map");
    for (Entry<String, Object> entry : map.entrySet())
      put(entry.getKey(), entry.getValue());
    return this;
  }

  @Override
  public BsonDocument.Builder put(String key, @Nullable Object value) {
    Preconditions.checkNotNull(key, "null key");
    Preconditions.checkArgument(!builder.containsKey(key), "key: '%s' is already present", key);
    builder.put(key, value);
    return this;
  }
  
  @Override
  public BsonDocument.Builder putAllowMultiVal(String key, @Nullable Object value) {
    Preconditions.checkNotNull(key, "null key");
    if (builder.containsKey(key)) {
        Object existingValue = builder.get(key);
        MultiValList<Object> list;
        if (existingValue instanceof MultiValList) {
            list = (MultiValList<Object>) existingValue;
        } else {
            //convert the existing value into a MultiValList
            list = new MultiValList<Object>();
            list.add(existingValue);
            builder.put(key, list);
        }
        list.add(value);
    } else {
        builder.put(key, value);
    }
    return this;
  }
  

  @Override
  public BsonDocument build() {
    return new DefaultDocument(builder.isEmpty()
        ? Collections.<String, Object>emptyMap()
        : builder);
  }
}
