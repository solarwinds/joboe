package com.tracelytics.instrumentation;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import junit.framework.TestCase;


/**
 * Does not test on any specific instrumentation. Instead it tests whether classes are modified properly even if there are class loaders that cannot be cached in the Agent.
 * 
 * More details in https://github.com/tracelytics/joboe/issues/405
 * 
 * @author pluk
 *
 */
public class ExcessiveClassLoaderInstrumentationTest extends TestCase {
    private static final String TEST_JAR_LOCATION = "src/test/java/com/tracelytics/instrumentation/DummyClass.jar";
    private static final int CLASS_LOADER_MAP_MAX_SIZE = 1000;
    
    public void testExcessiveClassLoader() throws Exception {
        //register the dummy instrumentation for testing
        Method registerInstrumentationMethod = ClassMap.class.getDeclaredMethod("registerInstrumentation", String.class, Class.class, Module.class);
        registerInstrumentationMethod.setAccessible(true);
        registerInstrumentationMethod.invoke(null, "DummyClass", DummyClassInstrumentation.class, null);
        
        File jarFile = new File(TEST_JAR_LOCATION);
        
        //just +1 that is not expected to be cached
        int testSize = CLASS_LOADER_MAP_MAX_SIZE + 1;
        
        //create a class loader array, so the loader will not get GCed during testing
        ClassLoader[] testLoaderArray = new ClassLoader[testSize];
        for (int i = 0 ; i < testSize; i ++) {
            testLoaderArray[i] = new URLClassLoader(new URL[] { jarFile.toURL() });
        }
        
        for (int i = 0 ; i < testSize; i ++) {
            Class<?> dummyClassClass = testLoaderArray[i].loadClass("DummyClass");
            dummyClassClass.getField("dummyField"); //it should NOT throw NoSuchFieldException
        }
    }
    
}
