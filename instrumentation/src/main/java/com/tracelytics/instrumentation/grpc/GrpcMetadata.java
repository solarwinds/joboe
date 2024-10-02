package com.tracelytics.instrumentation.grpc;

/**
 * Interface tagged to `io.grpc.Metadata` to make it easier to put and get gRPC Metadata headers (which usually are Http Headers)
 * @author Patson
 *
 */
public interface GrpcMetadata {
    public void tvPut(String key, String value);
    public Object tvGet(String key);
}
