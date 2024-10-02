package com.tracelytics.instrumentation;

import java.util.Arrays;
import java.util.List;

import com.tracelytics.ext.javassist.ClassPool;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.joboe.JoboeTest;

public class ConstructorMatcherTest extends JoboeTest {
    private ClassPool classPool = ClassPool.getDefault();
    
    public void testConstructorMatchers() throws NotFoundException {
        ConstructorMatcher<MyType> matcher;
        
        
        
        CtClass sampleClass = classPool.get(SampleClass.class.getName());
        
        matcher = new ConstructorMatcher<MyType>(new String[0]);
        assertMatch(matcher, sampleClass, classPool, 0, 1, 2, 3, 4);
        
        matcher = new ConstructorMatcher<MyType>(new String[0], null, true);
        assertMatch(matcher, sampleClass, classPool, 0);
        
        matcher = new ConstructorMatcher<MyType>(new String[] { "java.lang.Object", "int"});
        assertMatch(matcher, sampleClass, classPool, 2, 3, 4);
        
        matcher = new ConstructorMatcher<MyType>(new String[] { "java.lang.String", "int" });
        assertMatch(matcher, sampleClass, classPool, 3, 4);
        
        matcher = new ConstructorMatcher<MyType>(new String[] { "java.lang.String", "int", "long[]"});
        assertMatch(matcher, sampleClass, classPool, 4);
        
        matcher = new ConstructorMatcher<MyType>(new String[] { "java.lang.String", "int", "long"});
        assertMatch(matcher, sampleClass, classPool); //no match, param list has no match
        
        matcher = new ConstructorMatcher<MyType>(new String[0], null, true);
        assertMatch(matcher, sampleClass, classPool, 0); 
        assertMatch(matcher, sampleClass, new ClassPool(), 0); // force refresh but it should still match
        
        matcher = new ConstructorMatcher<MyType>(new String[0]);
        assertMatch(matcher, sampleClass, classPool, 0, 1, 2, 3, 4); 
        assertMatch(matcher, sampleClass, new ClassPool(), 0, 1, 2, 3, 4); // force refresh but it should still match
    }
    
    private void assertMatch(ConstructorMatcher<MyType> constructorMatcher, CtClass comparingClass, ClassPool classPool, Integer...matchingMethodIndicesArray) {
        List<Integer> matchingMethodIndices = Arrays.asList(matchingMethodIndicesArray);
        
        CtMethod[] methods = comparingClass.getDeclaredMethods(); 
        for (int i = 0; i < methods.length; i++) {
            if (matchingMethodIndices.contains(i)) {
                assertTrue("ConstructorMatcher [" + constructorMatcher + "] is expected to match constructor [" + methods[i] + "[] but it did not match!", constructorMatcher.matches(methods[i], classPool));
            } else {
                assertFalse("ConstructorMatcher [" + constructorMatcher + "] is expected to NOT match constructor [" + methods[i] + "[] but it matched!", constructorMatcher.matches(methods[i], classPool));
            }
        }
        
    }

    enum MyType {}
    
    private class SampleClass {
        public SampleClass() { } //0
        public SampleClass(Object o) {  }  //1
        public SampleClass(Object o, int i) {  } //2
        public SampleClass(String o, int i) { } //3
        public SampleClass(String o, int i, long[] j) { } //4
    }
}




