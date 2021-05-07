/**
 * Java Instrumentation Agent
 */

package com.tracelytics.agent;

import com.tracelytics.ext.javassist.*;
import com.tracelytics.ext.javassist.bytecode.ClassFile;
import com.tracelytics.instrumentation.ClassMap;
import com.tracelytics.instrumentation.*;
import com.tracelytics.joboe.StartupManager;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

class AgentClassFileTransformer implements ClassFileTransformer {
    protected static Logger logger = LoggerFactory.getLogger();
    protected final Instrumentation instrumentation;
    protected ClassPool defaultClassPool;

    private static final int CLASS_LOADER_MAP_MAX_SIZE = 1000;
    protected Map<ClassLoader, ClassPool> classLoaderMap = new MaxSizeWeakHashMap<ClassLoader, ClassPool>(CLASS_LOADER_MAP_MAX_SIZE);
    protected Map<ClassLoader, Set<String>> injectedLoaderMap = new MaxSizeWeakHashMap<ClassLoader, Set<String>>(CLASS_LOADER_MAP_MAX_SIZE);
    private Map<String, AgentTrigger> classLoadingTriggers = new HashMap<String, AgentTrigger>();
    private final Map<String, Object> classLocks = new HashMap<String, Object>();
    private static int APP_LOADER_MIN_SUPPORTED_VERSION = ClassFile.JAVA_7;
    private static boolean APP_LOADER_AVAILABLE = ClassFile.MAJOR_VERSION >= APP_LOADER_MIN_SUPPORTED_VERSION;
    //loader that loads the class resources with package "com.appoptics.apploader" from the agent jar file
    private static final AppLoaderClassLoader appLoaderClassLoader = APP_LOADER_AVAILABLE ? getAppLoaderClassLoader() : null;
    static {
        if (!APP_LOADER_AVAILABLE) {
            logger.info("Running on java version 6 or older. Some instrumentation might be disabled.");
        }
    }

    private static AppLoaderClassLoader getAppLoaderClassLoader() {
        try {
            File agentJarPath = ResourceDirectory.getAgentJarPath();
            if (agentJarPath != null) {
                return new AppLoaderClassLoader(agentJarPath);
            } else {
                logger.warn("Failed to get classloader for framework reference : cannot find agent jar location");
            }
        } catch (Throwable e) {
            logger.warn("Failed to get classloader for framework reference : " + e.getMessage(), e);
        }
        return null;
    }

    AgentClassFileTransformer(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
        defaultClassPool = ClassPool.getDefault();

        registerTriggers();
    }

    private void registerTriggers() {
        classLoadingTriggers.put("com/tracelytics/joboe/TraceDecisionUtil", new AfterSystemStartupTrigger()); //do not reference TraceDecisionUtil.class directly here, otherwise it triggers classloading on it
        classLoadingTriggers.put("org/springframework/batch/core/Job", new AfterSystemStartupTrigger(true));
        classLoadingTriggers.put("org/quartz/Job", new AfterSystemStartupTrigger(true));
        classLoadingTriggers.put("org/jboss/logmanager/Logger", new JBossLogManagerInitCompletedTrigger());
    }

    /**
     * Applies instrumentation to classes
     * @return  byte array of the modified class, null if class is not modified
     */
    public byte[] transform(ClassLoader loader, String className, Class<?> inputClass,
                            ProtectionDomain protectionDomain, byte[] classBytes)
            throws IllegalClassFormatException {
        checkClassLoadingTriggers(className);

        if (Agent.getStatus() != Agent.AgentStatus.INITIALIZED_SUCCESSFUL) {
            logger.trace("Agent is not initialized or Premain is still in progress, do not instrument class [" + className + "]");
            return null;
        }

        if (className != null) {
            logger.trace("Processing class: " + className);
        } else {
            logger.trace("Skipping null className");
            return null;
        }

        // Check for excluded first, otherwise we could create a ClassPool we may not even use.
        if (ClassMap.isExcluded(className)) {
            logger.trace("Excluding class: " + className);

            return null;
        }

        CtClass cc = null;
        final ClassPool appClassPool = getClassPool(loader);
        boolean modified = false;

        synchronized (getClassLock(className)) {
            try {
                //in rare case same class might get loaded concurrently and create multiple instances of CtClass,
                //though this is probably ok as the same instrumentation will just be applied on each of the instance respectively.
                //The extra overhead is not a big deal and the resulting byte code should be the same
                //Take note that we are calling makeClass instead of makeClassIfNew as other agents might modify the bytecode and we do
                //NOT want to overwrite their change by loading the version in javassist class pool which does not
                //contain changes from other agent (for example https://github.com/librato/joboe/issues/653)
                //And it deems the safest to create the class from the raw class bytes provided from the JVM
                cc = appClassPool.makeClass(new java.io.ByteArrayInputStream(classBytes));

                try {
                    FrameworkRecorder.reportFramework(protectionDomain, loader, cc.getPackageName()); //extract and report the framework that includes this package
                } catch (Throwable e) {
                    // avoid affecting instrumentation if any unexpected error thrown from FrameworkRecorder
                    logger.warn("Failed to extrace framework information on [" + className + "]");
                }


                //check if locator should be run on this class to discover more classes for instrumentation
                Set<InstrumentationBuilder<ClassLocator>> locatorBuilders = ClassMap.getLocator(cc, className);

                for (InstrumentationBuilder<ClassLocator> locatorBuilder : locatorBuilders) {
                    logger.debug("Using locator : " + locatorBuilder + " to locate class for dynamic instrumentation");

                    try {
                        ClassLocator locator = locatorBuilder.build();
                        locator.apply(cc, appClassPool, className);
                    } catch (NotFoundException e) {
                        logger.warn("Error loading class/method while applying locator [" + locatorBuilder + "] to class [" + className + "] : " + e.getMessage());
                    } catch(Exception ex) {
                        logger.warn("Unable to apply locator [" + locatorBuilder + "] to class [" + className + "], locator is skipped : " + ex.getMessage(), ex);
                    }
                }

                //get instrumentation base on class level information
                Set<InstrumentationBuilder<ClassInstrumentation>> instrumentationBuilders = ClassMap.getInstrumentation(cc, className);

                for (InstrumentationBuilder<ClassInstrumentation> instrumentationBuilder : instrumentationBuilders) {
                    logger.debug("Instrumenting class: " + className + " size: " + classBytes.length + " with instrumentation " + instrumentationBuilder);

                    try {
                        ClassInstrumentation instrumentation = instrumentationBuilder.build();

                        //Check if the instrumentation requires access to app loader classes - classes that access 3rd party framework code directly
                        if (APP_LOADER_AVAILABLE) {
                            Instrument annotation = instrumentation.getClass().getAnnotation(Instrument.class);
                            if (annotation != null && annotation.appLoaderPackage() != null) {
                                if (appLoaderClassLoader != null) {
                                    registerLoaderClass(appClassPool, loader, annotation.appLoaderPackage());
                                } else {
                                    logger.warn("Failed to register instrumenter package " + annotation.appLoaderPackage() + " as class loader is not initialized properly");
                                }
                            }
                        }

                        modified |= instrumentation.apply(cc, appClassPool, className, classBytes);
                    } catch (NotFoundException e) {
                        logger.warn("Error loading class/method while applying instrumentation [" + instrumentationBuilder + "] to class [" + className + "] : " + e.getMessage());
                    } catch(Exception ex) {
                        if (ex.getMessage() != null && ex.getMessage().contains("class is frozen")) {
                            logger.debug("Not instrumenting " + className + ": " + ex.getMessage());
                        } else {
                            logger.warn("Unable to apply instrumentation [" + instrumentationBuilder + "] to class [" + className + "], instrumentation is skipped : " + ex.getMessage(), ex);
                        }
                    }
                }

                //get instrumentation base on method annotations
                Map<InstrumentationBuilder<AnnotatedMethodsInstrumentation>, List<AnnotatedMethod>>  annotatedMethodsInstrumentationBuilders = ClassMap.getAnnotatedMethodInstrumentations(cc);

                for (Entry<InstrumentationBuilder<AnnotatedMethodsInstrumentation>, List<AnnotatedMethod>> entry : annotatedMethodsInstrumentationBuilders.entrySet()) {
                    logger.debug("Applying instrumentation " + entry.getKey() + " on annotated methods " + entry.getValue());
                    try {
                        AnnotatedMethodsInstrumentation instrumentation = entry.getKey().build();
                        modified |= instrumentation.apply(appClassPool, cc, entry.getValue());
                    } catch(Exception ex) {
                        if (ex.getMessage() != null && ex.getMessage().contains("class is frozen")) {
                            logger.debug("Not instrumenting " + className + ": " + ex.getMessage());
                        } else {
                            logger.warn("Unable to apply annotation instrumentation [" + entry.getKey() +  "] to [" + className + "], instrumentation is skipped : " + ex.getMessage(), ex);
                        }
                    }
                }



                return modified ? cc.toBytecode() : null;
            } catch(Throwable ex) {
                logger.debug("Unable to instrument class: " + className, ex);
            } finally {
                // See http://www.csg.is.titech.ac.jp/~chiba/javassist/html/javassist/ClassPool.html
                // Recommends either recreating ClassPool or using detach() to avoid high memory consumption
                if (cc != null) {
                    try {
                        cc.detach(); //make best effort to detach. This might throw NPE if the same instance is being evicted multiple times in some edge cases, see https://github.com/librato/joboe/issues/627
                    } catch (Exception e) {
                        logger.debug("Cannot detach class " + className + " probably due to concurrent classloading: "  + e.getMessage());
                    }
                }

                removeClassLock(className);
            }
        }

        return null;
    }


    public static class ByteClassLoader extends URLClassLoader {
        private final byte[] bytecode;
        private final String name;

        public ByteClassLoader(String name, byte[] bytecode) {
            super(new URL[0]);
            this.name = name;
            this.bytecode = bytecode;
        }

        @Override
        protected Class<?> findClass(final String name) throws ClassNotFoundException {
            if (this.name.equals(name)) {
                return defineClass(name, bytecode, 0, bytecode.length);
            }
            return super.findClass(name);
        }

    }

    private Method defineClassMethod;
    private Object internalUnsafeInstance;
    /**
     * 1. Inject the appLoaderClasses into the provided loader, so that the loader can loads those classes
     * 2. Append to the classPool the extra class path that can loads the appLoaderClasses
     *
     * @param classPool
     * @param loader
     * @param appLoaderPackages
     */
    private void registerLoaderClass(ClassPool classPool, ClassLoader loader, String[] appLoaderPackages) {
        if (loader != null && appLoaderPackages.length > 0) {
            try {
//                Method defineClassMethod = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
//                defineClassMethod.setAccessible(true);
                //CtClass tempCtClass = classPool.getOrNull(ClassLoader.class.getName() + "$AppOptics");

                if (defineClassMethod == null) {
                    Class<?> unsafeType = Class.forName("sun.misc.Unsafe");
                    Field theUnsafe = unsafeType.getDeclaredField("theUnsafe");
                    theUnsafe.setAccessible(true);
                    Object unsafeInstance = theUnsafe.get(null);


                    CtClass mirrorCtClass = classPool.get("java.lang.reflect.AccessibleObject");
                    mirrorCtClass.setName("com.appoptics.MirrorClass");
                    byte[] b = mirrorCtClass.toBytecode();
//                    mirrorCtClass = classPool.makeClass(new ByteArrayInputStream(b));
//                    Class mirrorClass = mirrorCtClass.toClass();

                    Class mirrorClass = new ByteClassLoader("com.appoptics.MirrorClass", b).loadClass("com.appoptics.MirrorClass");
                    Field overrideField = mirrorClass.getDeclaredField("override");

                    long offset = (Long) unsafeType
                            .getMethod("objectFieldOffset", Field.class)
                            .invoke(unsafeInstance, overrideField);

                    Method putBoolean = unsafeType.getMethod("putBoolean", Object.class, long.class, boolean.class);

                    Class<?> internalUnsafeType = Class.forName("jdk.internal.misc.Unsafe");
                    Field theUnsafeInternalField = internalUnsafeType.getDeclaredField("theUnsafe");
                    putBoolean.invoke(unsafeInstance, theUnsafeInternalField, offset, true); //need to set this, otherwise theUnsafeInternalField.get(null) throws exception below
                    //cannot access class jdk.internal.misc.Unsafe (in module java.base) because module java.base does not export jdk.internal.misc to unnamed module @4883b407
                    //	at java.base/jdk.internal.reflect.Reflection.newIllegalAccessException(Reflection.java:385)
                    internalUnsafeInstance = theUnsafeInternalField.get(null);

                    defineClassMethod = internalUnsafeType.getMethod("defineClass",
                            String.class,
                            byte[].class,
                            int.class,
                            int.class,
                            ClassLoader.class,
                            ProtectionDomain.class);
                    //Set this so we can call Unsafe.defineClassMethod
                    putBoolean.invoke(unsafeInstance, defineClassMethod, offset, true);

                    //Method defineClassMethod = unsafeInstance.getClass().getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class, ClassLoader.class, ProtectionDomain.class);
//                    tempCtClass = classPool.makeClass(ClassLoader.class.getName() + "$AppOptics");
//                    CtMethod tempMethod = CtNewMethod.make(
//                            "public Class tempMethod(ClassLoader classLoader, String name, byte[] bytecode, int offset, int length) {" +
//                            "   return classLoader.defineClass(name, bytecode, offset, length);" +
//                            "}", tempCtClass);
//                    tempCtClass.addMethod(tempMethod);
//
//                    byte[] bytes = tempCtClass.toBytecode();
//                    tempClass = (Class) defineClassMethod.invoke(internalUnsafeInstance, tempCtClass.getName(), bytes, 0, 0, null, null);
                }



                //Method defineClassMethod = tempClass.getDeclaredMethod("tempMethod", ClassLoader.class, String.class, byte[].class, int.class, int.class);


//                for (Method method : unsafeInstance.getClass().getDeclaredMethods()) {
//                    System.out.println(method);
//                }
//


                if (!injectedLoaderMap.containsKey(loader)) {
                    injectedLoaderMap.put(loader, new HashSet<String>());
                }
                Set<String> injectedPackages = injectedLoaderMap.get(loader);

                for (String appLoaderPackage : appLoaderPackages) {
                    if (!injectedPackages.contains(appLoaderPackage)) {
                        try {
                            for (Entry<String, byte[]> classEntry : appLoaderClassLoader.getPackageClasses(appLoaderPackage).entrySet()) {
                                String appLoaderClass = classEntry.getKey();
                                byte[] classBytes = classEntry.getValue();
                                defineClassMethod.invoke(internalUnsafeInstance, appLoaderClass, classBytes, 0, classBytes.length, loader, null);
//                                defineClassMethod.invoke(unsafeInstance, appLoaderClass, classBytes, 0, classBytes.length, loader, null);
                            }
                        } catch (Throwable e) {
                            logger.warn("Failed to register app loader package " + appLoaderPackage + " : " + e.getMessage(), e);
                        }
                        injectedPackages.add(appLoaderPackage);
                    }
                }

                classPool.appendClassPath(new LoaderClassPath(appLoaderClassLoader)); //append the resource to classpool too so code injection can reference these classes
            } catch (Exception e) {
                logger.warn("Failed to register app loader packages : " + e.getMessage(), e);
            }
        }
    }

    private void checkClassLoadingTriggers(String className) {
        AgentTrigger trigger = classLoadingTriggers.get(className);
        if (trigger != null) {
            trigger.triggerOnClassLoading();
        }
    }

    /**
     * Get a lock corresponding to this className. Repeated call on the same className will give same lock instance until the lock is removed from the map
     *
     * Take note that we cannot synchronize directly on String as it could have different references for each value,
     * nor should we call intern on the String as it's a bad idea https://stackoverflow.com/questions/133988/problem-with-synchronizing-on-string-objects
     *
     * Therefore, we will keep a simple HashMap with className as the key and an object as the lock object to be synchronized on. Take note that this will
     * return the same object for classes with same name but from different classloader. However, since it's rare to have different classloader to load on the same class name simultaneously
     * , let's not add too much complexity into the code logic
     *
     * @param className
     * @return  A lock Object by this className
     */
    private Object getClassLock(String className) {
        synchronized(classLocks) {
            Object lock = classLocks.get(className);
            if (lock != null) {
                return lock;
            } else {
                lock = new Object();
                classLocks.put(className, lock);
                return lock;
            }
        }
    }

    private void removeClassLock(String className) {
        synchronized(classLocks) {
            classLocks.remove(className);
        }
    }

    synchronized protected ClassPool getClassPool(ClassLoader baseClassLoader) {
        // We keep one class pool per class loader. App servers often use multiple class loaders.
        // There was a leak here, since entries were never explicitly removed from classLoaderMap...
        // classLoaderMap is now a WeakHashMap so the entry is removed when the class loader is garbage collected.
        // Also confirmed that LoaderClassPath uses weak references: http://www.csg.is.titech.ac.jp/~chiba/javassist/html/javassist/LoaderClassPath.html
        ClassPool baseClassPool = classLoaderMap.get(baseClassLoader);
        if (baseClassPool == null) {
            baseClassPool = new ClassPool(null);
            baseClassPool.childFirstLookup = true;

            baseClassPool.insertClassPath(new LoaderClassPath(baseClassLoader));

            //append extra loader to ensure our agent class is visible to the classpool
            //for jdk 9+, use the system class loader/thread context loader - more details in https://github.com/librato/joboe/pull/698 and https://issues.jboss.org/browse/JASSIST-270
            if (ClassFile.MAJOR_VERSION >= ClassFile.JAVA_9) {
                if (ClassLoader.getSystemClassLoader() != null) {
                    //system class loader should have access to bootstrap class loader, which is where we make our agent classes available via MANIFEST.MF's Boot-Class-Path
                    baseClassPool.appendClassPath(new LoaderClassPath(ClassLoader.getSystemClassLoader()));
                } else {
                    //fall-back: javassist 3.22 handles jdk 9 by using context class loader, though it does not work for jboss/wildfly, which uses module class loader
                    baseClassPool.appendClassPath(new LoaderClassPath(Thread.currentThread().getContextClassLoader()));
                }
            } else {
                //javassist 3.21- handling for jdk 8-, safer to just keep it as it was
                baseClassPool.appendClassPath(new ClassClassPath(Object.class));
            }

            classLoaderMap.put(baseClassLoader, baseClassPool);
            logger.debug("Created class pool for " + (baseClassLoader != null ? baseClassLoader.getClass().getName() : "null"));
        }

        return baseClassPool;
    }

    /**
     * Checks for any classes loaded so far that are eligible for "re-transformation". This should be applied after the premain is completed in order to avoid
     * crashing VM as reported in https://bugs.openjdk.java.net/browse/JDK-8074299
     */
    void checkRetransformation() {
        try {
            if (instrumentation.isRetransformClassesSupported()) {
                Set<String> retransformClasses = ClassMap.getRetransformClasses();

                List<Class<?>> matchedClasses = new ArrayList<Class<?>>();
                for (Class<?> loadedClass : instrumentation.getAllLoadedClasses()) {
                    Set<String> superTypes = new HashSet<String>();
                    //load all the super types of this class, take note that we need the string value instead of the actual class instance as classes might be loaded
                    //from different class loader, so comparing class instance directly might not work when classes are loaded by different classloaders
                    getAllSuperTypes(loadedClass, superTypes);
                    if (superTypes.removeAll(retransformClasses)) { //then some overlap, need to re-transform this loaded class
                        matchedClasses.add(loadedClass);
                        logger.debug("Going to retransform " + loadedClass.getName());
                    }
                }
                if (!matchedClasses.isEmpty()) {
                    instrumentation.retransformClasses(matchedClasses.toArray(new Class<?>[matchedClasses.size()]));
                }
            } else {
                logger.info("JVM does not support class retransformation");
            }
        } catch (Exception e) {
            logger.warn("Cannot retransform classes!" + e);
        } catch (NoSuchMethodError e) {
            //ok 1.5
        }
    }


    /**
     * Finds all the super types of the supplied clazz parameter, put the result in the types parameters supplied
     * @param clazz
     * @param types
     */
    private static void getAllSuperTypes(Class<?> clazz, Set<String> types) {
        if (clazz != null) {
            types.add(clazz.getName());
            if (clazz.getSuperclass() != null) {
                getAllSuperTypes(clazz.getSuperclass(), types);
            }
            for (Class<?> interfaze : clazz.getInterfaces()) {
                getAllSuperTypes(interfaze, types);
            }
        }
    }


    /**
     * A simple class that extends WeakHashMap and enforce a rough max size.
     * Take note that the map can still grow to bigger than the max size with concurrent accesses. 
     *
     * However, this class is only used internally so simple checks should serve the purpose
     *
     * @author pluk
     *
     * @param <K>
     * @param <V>
     */
    static class MaxSizeWeakHashMap<K, V> extends WeakHashMap<K, V> {
        private final int MAX_SIZE;
        /**
         *
         * @param maxSize   no new elements can be added to map if this size is reached
         */
        MaxSizeWeakHashMap(int maxSize) {
            super();
            MAX_SIZE = maxSize;
        }

        @Override
        /**
         * Inserts the element only if there is space available
         */
        public V put(K key, V value) {
            if (size() >= MAX_SIZE) {
                return null;
            } else {
                return super.put(key, value);
            }
        }

        @Override
        /**
         * Either puts all or none depending on whether there is enough space for the whole inserting Map argument
         */
        public void putAll(Map<? extends K, ? extends V> m) {
            if (size() + m.size() <= MAX_SIZE) {
                super.putAll(m);
            }
        }
    }

    private interface AgentTrigger {
        void triggerOnClassLoading();
    }

    private class AfterSystemStartupTrigger implements AgentTrigger {
        private final boolean blocking;
        private static final long BLOCKING_TIME_IN_MILLISEC = 10000; //block for at most 10 seconds

        private AfterSystemStartupTrigger(boolean blocking) {
            this.blocking = blocking;
        }

        private AfterSystemStartupTrigger() {
            this(false);
        }

        @Override
        public void triggerOnClassLoading() {
            StartupManager.flagSystemStartupCompleted(blocking ? BLOCKING_TIME_IN_MILLISEC : null);
        }
    }

    private class JBossLogManagerInitCompletedTrigger implements AgentTrigger {
        private AtomicBoolean hasReportedInit = new AtomicBoolean(false);
        @Override
        public void triggerOnClassLoading() {
            if (!hasReportedInit.getAndSet(true)) {
                if (ClassFile.MAJOR_VERSION >= ClassFile.JAVA_7) { //to avoid class deadlocking as described in https://github.com/librato/joboe/pull/704 for JDK 6-
                    Agent.reportInit();
                }
            }
        }
    }
}
