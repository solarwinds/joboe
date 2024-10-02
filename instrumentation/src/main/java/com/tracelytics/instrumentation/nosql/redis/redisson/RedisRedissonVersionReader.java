package com.tracelytics.instrumentation.nosql.redis.redisson;

import com.tracelytics.instrumentation.FrameworkVersion;
import com.tracelytics.ext.javassist.ClassPool;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.NotFoundException;

import java.util.Arrays;
import java.util.List;

public class RedisRedissonVersionReader {
    private final ClassPool classPool;

    public RedisRedissonVersionReader(ClassPool classPool) {
        this.classPool = classPool;
    }

    public FrameworkVersion getFrameworkVersion() {
        try {
            classPool.get("org.redisson.core.RObject");
            return new FrameworkVersion(1, 0);
        } catch (NotFoundException e) {
            try {
                classPool.get("org.redisson.api.RObject");
                try {
                    CtClass rFutureClass = classPool.get("org.redisson.api.RFuture");
                    CtClass completionStageClass = classPool.get("java.util.concurrent.CompletionStage");
                    List<CtClass> rFutureInterfaces = Arrays.asList(rFutureClass.getInterfaces());
                    if (rFutureInterfaces.contains(completionStageClass)) {
                        return new FrameworkVersion(3, 0);
                    } else {
                        return new FrameworkVersion(2, 0);
                    }
                } catch (NotFoundException e1) { //at least has RObject
                    return new FrameworkVersion(2, 0);
                }
            } catch (NotFoundException ex) {
                return new FrameworkVersion(1, 0);
            }

        }
    }
}
