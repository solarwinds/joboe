package com.tracelytics.instrumentation;

import java.util.Arrays;
import java.util.List;

import com.tracelytics.ext.javassist.ClassPool;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.joboe.JoboeTest;

public class MethodMatcherTest extends JoboeTest {
    private ClassPool classPool = ClassPool.getDefault();
    
    public void testMethodMatchers() throws NotFoundException {
        MethodMatcher<MyType> matcher;
        
        
        
        CtClass sampleClass = classPool.get(SampleClass.class.getName());
        
        matcher = new MethodMatcher<MyType>("test", new String[0], "void");
        assertMatch(matcher, sampleClass, classPool, 0);
        
        matcher = new MethodMatcher<MyType>("test", new String[0], "java.lang.Object");
        assertMatch(matcher, sampleClass, classPool, 1, 2, 3, 4);
        
        matcher = new MethodMatcher<MethodMatcherTest.MyType>("test", new String[0], "java.lang.String");
        assertMatch(matcher, sampleClass, classPool, 2, 3, 4);
        
        matcher = new MethodMatcher<MethodMatcherTest.MyType>("test", new String[] { "java.lang.Object", "int"}, "java.lang.String");
        assertMatch(matcher, sampleClass, classPool, 2, 3, 4);
        
        matcher = new MethodMatcher<MethodMatcherTest.MyType>("test", new String[] { "java.lang.String", "int" }, "java.lang.String");
        assertMatch(matcher, sampleClass, classPool, 3, 4);
        
        matcher = new MethodMatcher<MethodMatcherTest.MyType>("test", new String[] { "java.lang.String", "int", "long[]"}, "java.lang.String");
        assertMatch(matcher, sampleClass, classPool, 4);
        
        matcher = new MethodMatcher<MethodMatcherTest.MyType>("test", new String[] { "java.lang.String", "int", "long"}, "java.lang.String");
        assertMatch(matcher, sampleClass, classPool); //no match, param list has no match
        
        matcher = new MethodMatcher<MethodMatcherTest.MyType>("invalidMethodName", new String[0], "java.lang.String");
        assertMatch(matcher, sampleClass, classPool); //no match, method name has no match
        
        matcher = new MethodMatcher<MethodMatcherTest.MyType>("test", new String[0], "java.lang.Number");
        assertMatch(matcher, sampleClass, classPool); //no match, return param has no match
        
        matcher = new MethodMatcher<MethodMatcherTest.MyType>("test", new String[0], "NotExistTestClass");
        assertMatch(matcher, sampleClass, classPool); //no match, return param is not a valid class
        
        matcher = new MethodMatcher<MyType>("test", new String[0], "void");
        assertMatch(matcher, sampleClass, classPool, 0); 
        assertMatch(matcher, sampleClass, new ClassPool(), 0); // force refresh but it should still match
    }
    
    private void assertMatch(MethodMatcher<MyType> methodMatcher, CtClass comparingClass, ClassPool classPool, Integer...matchingMethodIndicesArray) {
        List<Integer> matchingMethodIndices = Arrays.asList(matchingMethodIndicesArray);
        
        CtMethod[] methods = comparingClass.getDeclaredMethods(); 
        for (int i = 0; i < methods.length; i++) {
            if (matchingMethodIndices.contains(i)) {
                assertTrue("MethodMatcher [" + methodMatcher + "] is expected to match method [" + methods[i] + "[] but it did not match!", methodMatcher.matches(methods[i], classPool));
            } else {
                assertFalse("MethodMatcher [" + methodMatcher + "] is expected to NOT match method [" + methods[i] + "[] but it matched!", methodMatcher.matches(methods[i], classPool));
            }
        }
        
    }

    enum MyType {
        TYPE_1, TYPE_2, TYPE_3
    }
    
    private class SampleClass {
        public void test() {}  //0
        public Object test(Object o) { return null; }  //1
        public String test(Object o, int i) { return null; } //2
        public String test(String o, int i) { return null; } //3
        public String test(String o, int i, long[] j) { return null; } //4
    }
}




