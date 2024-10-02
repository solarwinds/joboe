package com.appoptics.instrumentation.reactor;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import com.google.auto.service.AutoService;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.Instrument;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.Module;

/**
 * Instruments Reactor's Flux, Mono and ParallelFlux on the target method: `onAssembly`
 *
 */

@AutoService(ClassInstrumentation.class)
@Instrument(targetType = {"reactor.core.publisher.Flux", "reactor.core.publisher.Mono", "reactor.core.publisher.ParallelFlux"}, module = Module.REACTOR, appLoaderPackage = "com.appoptics.apploader.instrumenter.reactor")
public class PublisherInstrumentation extends ClassInstrumentation {
    private static final String INSTRUMENTER_CLASS_NAME = "com.appoptics.apploader.instrumenter.reactor.PublisherInstrumenter";
    private enum OpType { ON_ASSEMBLY }

    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
            new MethodMatcher<OpType>("onAssembly", new String[]{"java.lang.Object" }, "java.lang.Object", OpType.ON_ASSEMBLY)
    );


    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        for (Entry<CtMethod, OpType> matchingMethodEntry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            OpType type = matchingMethodEntry.getValue();
            CtMethod method = matchingMethodEntry.getKey();
            if (type == OpType.ON_ASSEMBLY) {
                insertBefore(method, INSTRUMENTER_CLASS_NAME + ".enterOnAssembly();", false);
            }
        }
        return true;
    }
}