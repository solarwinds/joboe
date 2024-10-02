
/** Instruments javax.servlet.Filter : See http://docs.oracle.com/javaee/5/api/javax/servlet/Filter.html
 */

package com.tracelytics.instrumentation.http;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.ClassInstrumentation;

public class FilterInstrumentation extends ClassInstrumentation {

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        CtMethod filterMethod = cc.getMethod("doFilter", "(Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletResponse;Ljavax/servlet/FilterChain;)V");
        
        if (filterMethod.getDeclaringClass() == cc) {
            modifyFilter(filterMethod);
        }
        
        return true;
   
    }

    public void modifyFilter(CtMethod method)
            throws CannotCompileException, NotFoundException {
        // Filters are another HTTP entry point: we reuse servlet instrumentation.
        insertBefore(method, SERVLET_CLASS_NAME + ".layerEntry(this, (Object)$1,(Object)$2);", false);
        addErrorReporting(method, Throwable.class.getName(), layerName, classPool);
        insertAfter(method, SERVLET_CLASS_NAME + ".layerExit(this, (Object)$1,(Object)$2);", true, false);
    }

    static public final String SERVLET_CLASS_NAME = ServletInstrumentation.class.getName();


}
