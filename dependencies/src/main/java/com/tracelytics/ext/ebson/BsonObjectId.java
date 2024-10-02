package com.tracelytics.ext.ebson;

import java.nio.ByteBuffer;

// @checkstyle:off .
public interface BsonObjectId {

  ByteBuffer objectId();

  ByteBuffer time();

  ByteBuffer machineId();

  ByteBuffer processId();

  ByteBuffer increment();
}
