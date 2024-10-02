package com.tracelytics.instrumentation;

import com.tracelytics.ext.javassist.ClassPool;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.instrumentation.http.ServletInstrumentation;
import com.tracelytics.joboe.JoboeTest;

public class ClassMapTest extends JoboeTest {
    private ClassPool classPool = ClassPool.getDefault();
    
    public void testRegisterInstrumentationTest() throws Exception {
        CtClass test1 = classPool.makeClass("test1");
        test1.toClass();
        ClassMap.registerInstrumentation(test1.getName(), DummyClassInstrumentation.class, Module.SERVLET);
        ClassMap.registerInstrumentation(test1.getName(), DummyClassInstrumentation.class, Module.SERVLET); //should not register it twice

        assertEquals(1, ClassMap.getInstrumentation(test1, test1.getName()).size());


        CtClass test2 = classPool.makeClass("test2");
        test2.toClass();
        ClassMap.registerInstrumentation(test2.getName(), DummyClassInstrumentation.class, Module.SERVLET);
        ClassMap.registerInstrumentation(test2.getName(), ServletInstrumentation.class, Module.SERVLET);

        assertEquals(2, ClassMap.getInstrumentation(test2, test2.getName()).size());


        CtClass test3 = classPool.makeClass("test3");
        test3.toClass();
        ClassMap.registerInstrumentation(test3.getName(), new DummyInstrumentationBuilder(1), Module.SERVLET, false);
        ClassMap.registerInstrumentation(test3.getName(), new DummyInstrumentationBuilder(2), Module.SERVLET, false); //consider as different builder

        assertEquals(2, ClassMap.getInstrumentation(test3, test3.getName()).size());
    }

    private static class DummyInstrumentationWithParameter extends ClassInstrumentation {
        private final int parameter;
        private DummyInstrumentationWithParameter(int parameter) {
            this.parameter = parameter;
        }

        @Override
        protected boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes) throws Exception {
            return false;
        }
    }

    private static class DummyInstrumentationBuilder implements InstrumentationBuilder<ClassInstrumentation> {
        private final int parameter;

        public DummyInstrumentationBuilder(int parameter) {
            this.parameter = parameter;
        }

        @Override
        public DummyInstrumentationWithParameter build() throws Exception {
            return new DummyInstrumentationWithParameter(parameter);
        }
    }



}




