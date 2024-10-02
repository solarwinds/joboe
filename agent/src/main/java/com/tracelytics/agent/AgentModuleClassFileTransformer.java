/**
 * Java Instrumentation Agent
 */

package com.tracelytics.agent;

import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * ClassFileTransformer that supports the JDK 9+ module
 */
class AgentModuleClassFileTransformer extends AgentClassFileTransformer {
    private static Set<Module> patchedModules; //lazily initialized - with added benefit that no error is triggered on JDK 5

    AgentModuleClassFileTransformer(Instrumentation instrumentation) {
        super(instrumentation);
    }



    /**
     * Applies instrumentation to classes. Take note that this method signature is only invoked for JDK 9+ which introduces
     * the module system. Due to a bug as reported in https://bugs.openjdk.java.net/browse/JDK-8202408, modules with
     * instrumented classes would need to be redefined to add read access to the our agent module.
     *
     * Therefore on the first modified classes on any given module, we would add a call to redefine the module to add
     * read access to our agent module
     *
     * @param module
     * @param loader
     * @param className
     * @param inputClass
     * @param protectionDomain
     * @param classBytes
     * @return
     * @throws IllegalClassFormatException
     */
    public byte[] transform(Module module, ClassLoader loader, String className, Class<?> inputClass,
                            ProtectionDomain protectionDomain, byte[] classBytes) throws IllegalClassFormatException {
        boolean shouldRedefineModule = false;
        byte[] transformedBytes = transform(loader, className, inputClass, protectionDomain, classBytes);
        synchronized (this) {
            if (patchedModules == null) {
                patchedModules = Collections.newSetFromMap(new WeakHashMap<Module, Boolean>());
            }
            if (transformedBytes != null && module != null && !patchedModules.contains(module) && module.isNamed() && instrumentation.isModifiableModule(module)) {
                patchedModules.add(module); //ensure we do not patch the same module twice, doing that seems to trigger classloading problem
                shouldRedefineModule = true;
            }
        }
        if (shouldRedefineModule) {
            logger.debug("Redefining module " + module + " to add extra access to agent module");
            instrumentation.redefineModule(module, Collections.singleton(getClass().getModule()), Collections.EMPTY_MAP, Collections.EMPTY_MAP, Collections.EMPTY_SET, Collections.EMPTY_MAP);
        }
        return transformedBytes;
    }
}
