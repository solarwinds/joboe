package com.tracelytics.instrumentation.grpc;

/**
 * Exposes call target and gRPC method type of a particular `io.grpc.internal.ClientCallImpl`.
 * 
 * @author pluk
 *
 */
public interface GrpcClientCall {
    String tvGetTarget();
    void tvSetTarget(String target);
    
    String tvGetMethod();
    String tvGetMethodType();
}
