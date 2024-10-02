package com.tracelytics.instrumentation.http.play;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtConstructor;
import com.tracelytics.ext.javassist.CtField;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.ConstructorMatcher;

/**
 *  Patches wrapper class (on scala.Function) so we can find out the original block used in order to generate Play profile controller/action KVs
 *
 * @author pluk
 *
 */
public class PlayBlockWrapperPatcher extends PlayBaseInstrumentation {
    @SuppressWarnings("unchecked")
    private static List<ConstructorMatcher<Object>> constructorMatchers = Arrays.asList(
      new ConstructorMatcher<Object>(new String[] { "java.lang.Object", "scala.Function1" }),
      new ConstructorMatcher<Object>(new String[] { "java.lang.Object", "scala.Function0" })
    ); 
    

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes) throws CannotCompileException, NotFoundException {
        Set<CtConstructor> constructors = findMatchingConstructors(cc, constructorMatchers).keySet();
        if (!constructors.isEmpty()) {
            cc.addField(CtField.make("private Object wrappedBlock;", cc));
            cc.addMethod(CtNewMethod.make("public Object getWrappedBlock() { if (wrappedBlock instanceof " + PlayBlockWrapper.class.getName() + " && wrappedBlock != this) {  return ((" + PlayBlockWrapper.class.getName() + ")wrappedBlock).getWrappedBlock(); } else { return wrappedBlock; }}", cc));
            tagInterface(cc, PlayBlockWrapper.class.getName());
            
            for (CtConstructor constructor : cc.getConstructors()) {
                insertAfter(constructor, "wrappedBlock = $2;", true, false);
            }
            return true;
        } else {
            return false;
        }
    }
}