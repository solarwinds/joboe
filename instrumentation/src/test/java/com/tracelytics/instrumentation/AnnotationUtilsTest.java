package com.tracelytics.instrumentation;

import java.io.IOException;
import java.util.List;

import com.tracelytics.ext.javassist.ClassPool;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.ext.javassist.bytecode.annotation.Annotation;

import junit.framework.TestCase;

public class AnnotationUtilsTest extends TestCase {
    
    public void testGetAnnotationsFromBehavior() throws IOException, NotFoundException {
        ClassPool classPool = ClassPool.getDefault();
        
        CtClass classB = null;
        
        try {
            classB = classPool.get(ClassB.class.getName());
            
            List<Annotation> annotations;
                    
            //test lookup without super types
            annotations = AnnotationUtils.getAnnotationsFromBehavior(classB.getDeclaredMethod("method1", new CtClass[] {}));
            assertEquals(false, hasDeprecatedMethodAnnotation(annotations));
            
            annotations = AnnotationUtils.getAnnotationsFromBehavior(classB.getDeclaredMethod("method1", new CtClass[] { classPool.get(String.class.getName())}));
            assertEquals(false, hasDeprecatedMethodAnnotation(annotations)); //even if the parent overridden method1 has the annotation, by default we do NOT look up super type
            
            annotations = AnnotationUtils.getAnnotationsFromBehavior(classB.getDeclaredMethod("method2", new CtClass[] {}));
            assertEquals(false, hasDeprecatedMethodAnnotation(annotations)); //even if the parent overridden method2 has the annotation, by default we do NOT look up super type
            
            annotations = AnnotationUtils.getAnnotationsFromBehavior(classB.getDeclaredMethod("method3", new CtClass[] {}));
            assertEquals(true, hasDeprecatedMethodAnnotation(annotations));
            
            //test lookup with super types
            annotations = AnnotationUtils.getAnnotationsFromBehavior(classB.getDeclaredMethod("method1", new CtClass[] {}), true);
            assertEquals(false, hasDeprecatedMethodAnnotation(annotations));
            
            annotations = AnnotationUtils.getAnnotationsFromBehavior(classB.getDeclaredMethod("method1", new CtClass[] { classPool.get(String.class.getName())}), true);
            assertEquals(true, hasDeprecatedMethodAnnotation(annotations)); //parent overridden method1 has the annotation
            
            annotations = AnnotationUtils.getAnnotationsFromBehavior(classB.getDeclaredMethod("method2", new CtClass[] {}), true);
            assertEquals(true, hasDeprecatedMethodAnnotation(annotations)); //parent overridden method2 has the annotation
            
            annotations = AnnotationUtils.getAnnotationsFromBehavior(classB.getDeclaredMethod("method3", new CtClass[] {}), true);
            assertEquals(true, hasDeprecatedMethodAnnotation(annotations));
        } finally {
            if (classB != null) {
                classB.detach();
            }
        }
    }
    
    
    private Object hasDeprecatedMethodAnnotation(List<Annotation> annotations) {
        for (Annotation annotation : annotations) {
            if (annotation.getTypeName().equals("java.lang.Deprecated")) {
                return true;
            }
        }
        return false;
    }


    private static class ClassA {
        void method1() {
            
        }

        @Deprecated
        void method1(String in) {
            
        }
    }
    
    private static interface InterfaceA {
        @Deprecated
        void method2();
    }
    
    private static class ClassB extends ClassA implements InterfaceA {
        @Override
        void method1() {
            super.method1();
        }
        
        @Override
        void method1(String in) {
            super.method1(in);
        }
        
        @Override
        public void method2() {
        }

        @Deprecated
        public void method3() {
            
        }
        
    }
        

}
