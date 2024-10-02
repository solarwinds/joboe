package com.tracelytics.instrumentation.grpc;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;

/**
 * Patches `io.grpc.Metadata` to make it easier to put and gRPC Metadata headers (which usually are Http Headers)
 * @author Patson
 *
 */
public class GrpcMetadataPatcher extends ClassInstrumentation {
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        
        cc.addMethod(CtNewMethod.make("public void tvPut(String key, String value) { put(io.grpc.Metadata.Key.of(key, io.grpc.Metadata.ASCII_STRING_MARSHALLER), value); }", cc));
        cc.addMethod(CtNewMethod.make("public Object tvGet(String key) { return get(io.grpc.Metadata.Key.of(key, io.grpc.Metadata.ASCII_STRING_MARSHALLER)); }", cc));
        
        tagInterface(cc, GrpcMetadata.class.getName());
        
        return true;
    }
}