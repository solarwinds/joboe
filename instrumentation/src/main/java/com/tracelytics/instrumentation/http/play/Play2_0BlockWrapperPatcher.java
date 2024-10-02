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
 *  Patches Play 2.0 wrapper class to provide details of the wrapped block
 *
 * @author pluk
 *
 */
public class Play2_0BlockWrapperPatcher extends PlayBaseInstrumentation {
    @SuppressWarnings("unchecked")
    private static List<ConstructorMatcher<Object>> constructorMatchers = Arrays.asList(
      new ConstructorMatcher<Object>(new String[] { "scala.Function0" }, null, true)
    ); 
    

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes) throws CannotCompileException, NotFoundException {
        Version version = identifyVersion();
        if (version != Version.PLAY_2_0) { //only apply to play 2.0
            return false;
        }
        
        Set<CtConstructor> constructors = findMatchingConstructors(cc, constructorMatchers).keySet();
        if (!constructors.isEmpty()) {
            cc.addField(CtField.make("private Object wrappedBlock;", cc));
            cc.addMethod(CtNewMethod.make("public Object getWrappedBlock() { if (wrappedBlock instanceof " + PlayBlockWrapper.class.getName() + " && wrappedBlock != this) {  return ((" + PlayBlockWrapper.class.getName() + ")wrappedBlock).getWrappedBlock(); } else { return wrappedBlock; }}", cc));
            tagInterface(cc, PlayBlockWrapper.class.getName());
            
            for (CtConstructor constructor : cc.getConstructors()) {
                insertAfter(constructor, "wrappedBlock = $1;", true, false);
            }
            return true;
        } else {
            return false;
        }
    }
    
}