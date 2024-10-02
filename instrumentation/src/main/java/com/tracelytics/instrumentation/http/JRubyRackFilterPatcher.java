package com.tracelytics.instrumentation.http;

import java.text.MessageFormat;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;

/**
 * Patches the JRuby Rack filter to address the issue that JRuby Rack filer resets the response header x-trace set by our servlet layer entry
 * 
 * This sets the x-trace response header back to what the java servlet instrumentation generated before  
 *  
 * @author Patson Luk
 *
 */
public class JRubyRackFilterPatcher extends FilterInstrumentation {
    private static final String CLASS_NAME = JRubyRackFilterPatcher.class.getName();
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        CtMethod filterMethod = cc.getMethod("doFilter", "(Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletResponse;Ljavax/servlet/FilterChain;)V");
        
        if (filterMethod.getDeclaringClass() == cc) {
            insertAfter(filterMethod, CLASS_NAME + ".resetXTraceId(this, $2);", true, false);
        }
        
        super.applyInstrumentation(cc, className, classBytes);
        return true;
    }
    
    public static void resetXTraceId(Object caller, Object responseObject) {
        if (!(responseObject instanceof HttpServletResponse)) {
            logger.debug(MessageFormat.format("Skipping instrumentation on JRubyRackFilter [{0}] as the response object is not Http servlet objects, response [{1}]",
                                              caller.getClass().getName(),
                                              responseObject != null ? responseObject.getClass().getName() : "null"));
            return;
        }
        
        HttpServletResponse resp = (HttpServletResponse) responseObject;
        
        if (resp.tlysGetXTraceID() != null) { 
            resp.setHeader(ServletInstrumentation.XTRACE_HEADER, resp.tlysGetXTraceID()); //reset the x-trace response header to the servlet exit x-trace id pre-generated
        }
    }

  
}
