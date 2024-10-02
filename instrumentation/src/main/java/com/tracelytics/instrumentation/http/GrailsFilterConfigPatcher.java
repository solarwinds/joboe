package com.tracelytics.instrumentation.http;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.ClassInstrumentation;

/**
 * Patches the org.codehaus.groovy.grails.plugins.web.filters.FilterConfig class to return the name of the Config. 
 * @author pluk
 *
 */
public class GrailsFilterConfigPatcher extends ClassInstrumentation {
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        if (cc.subtypeOf(classPool.get(GrailsFilterConfig.class.getName()))) { //already tagged
            return false;
        }
        
        
        try {
            cc.getDeclaredField("name", "Ljava/lang/String;");
        } catch (NotFoundException e) {
            logger.warn("Cannot find field name in class [" + cc.getName() + "], skipping patching");
            return false;
        }
        
        
        
        cc.addMethod(CtNewMethod.make("public String getTVConfigName() { return name; }", cc));
        tagInterface(cc, GrailsFilterConfig.class.getName());
        
        return true;
    }

 
}