package com.appoptics.instrumentation.nosql.redisson2;

import com.google.auto.service.AutoService;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.Module;
import com.tracelytics.instrumentation.*;
import com.tracelytics.instrumentation.nosql.redis.redisson.RedisRedissonVersionReader;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Instruments `org.redisson.client.RedisConnection` to capture the start of the lower level "command span" on `send` invocation.
 *
 * A single high level Redisson operation can spawn one or more commands.
 *
 * Take note that the span finish is created by a listener that listens to the operationComplete invocation
 *
 */
@AutoService(ClassInstrumentation.class)
@Instrument(targetType = { "org.redisson.client.RedisConnection" },
        module = Module.REDIS,
        appLoaderPackage = { "com.appoptics.apploader.instrumenter.nosql.redisson2" })
public class RedisRedisson2ConnectionInstrumentation extends ClassInstrumentation {
    private static final String INSTRUMENTER_CLASS = "com.appoptics.apploader.instrumenter.nosql.redisson2.Redisson2Instrumenter.SINGLETON";

    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
            new MethodMatcher<OpType>("send", new String[]{ "org.redisson.client.protocol.CommandData" }, "io.netty.channel.ChannelFuture", OpType.SEND, true),
            new MethodMatcher<OpType>("send", new String[]{ "org.redisson.client.protocol.CommandsData" }, "io.netty.channel.ChannelFuture", OpType.SEND, true),
            new MethodMatcher<OpType>("sync", new String[]{ }, "java.lang.Object", OpType.SYNC)

    );

    private enum OpType { SEND, SYNC }
    private FrameworkVersion version;

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        if (version == null) {
            version = new RedisRedissonVersionReader(classPool).getFrameworkVersion();
        }

        if (!isSupportedVersion(version)) {
            return false;
        }

        Map<CtMethod, OpType> matchingMethods = findMatchingMethods(cc, methodMatchers);

        for (Map.Entry<CtMethod, OpType> matchingMethodEntry : matchingMethods.entrySet()) {
            CtMethod method = matchingMethodEntry.getKey();
            OpType type = matchingMethodEntry.getValue();
            if (type == OpType.SEND) {
                insertBefore(method, instrumenterClass() + ".beforeSend($1, getChannel() != null ? getChannel().remoteAddress() : null);");
            } else if (type == OpType.SYNC) {
                insertBefore(method, instrumenterClass() + ".flagSyncCommand(true);");
                insertAfter(method, instrumenterClass() + ".flagSyncCommand(false);", true);
            }
        }

        return true;
    }

    protected String instrumenterClass() {
        return INSTRUMENTER_CLASS;
    }

    protected boolean isSupportedVersion(FrameworkVersion version) {
        return version.getMajorVersion() == 2;
    }


}