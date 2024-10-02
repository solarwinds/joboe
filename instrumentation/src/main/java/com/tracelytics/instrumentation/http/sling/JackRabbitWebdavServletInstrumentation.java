package com.tracelytics.instrumentation.http.sling;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;

/**
 * Instruments WebDAV/DavEx handling of Sling (which extends jackrabbit webdav implementation). Take note that since we instrument the parent class {@code org.apache.jackrabbit.webdav.server.AbstractWebdavServlet} 
 * which actually is a jackrabbit implementation, therefore we check the package name of the instrumentation class to determine whether layer name "jackrabbit-webdav" or "sling" should be used
 * 
 * Take note that most of the Sling operation goes through the {@code SlingRequestProcessorImpl} which is instrumented in {@ SlingRequestProcessorInstrumentation}. However, for requests
 * that have url of {@code <deployed location>/server/<resource path>} (DavEx) or {@code <deployed location>/dav/<workspace>/<resource path>} (WebDAV) would bypass the processor and be
 * handled by the this servlet. * 
 * 
 * @author pluk
 *
 */
public class JackRabbitWebdavServletInstrumentation extends ClassInstrumentation {
    private static final String CLASS_NAME = JackRabbitWebdavServletInstrumentation.class.getName();
    
    private static final String GENERAL_LAYER_NAME = "jackrabbit-webdav";
    private static final String SLING_LAYER_NAME = "sling";
    
    
            
    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
        new MethodMatcher<OpType>("service", new String[] {"javax.servlet.ServletRequest", "javax.servlet.ServletResponse"}, "void", OpType.SERVICE),
        new MethodMatcher<OpType>("execute", new String[] {"org.apache.jackrabbit.webdav.WebdavRequest", "org.apache.jackrabbit.webdav.WebdavResponse", "int", "org.apache.jackrabbit.webdav.DavResource"}, "boolean", OpType.EXECUTE)
    );
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        Map<CtMethod, OpType> matchingMethods = findMatchingMethods(cc, methodMatchers);
        
        for (Entry<CtMethod, OpType> matchingMethodEntry : matchingMethods.entrySet()) {
            if (matchingMethodEntry.getValue() == OpType.SERVICE) {
                insertBefore(matchingMethodEntry.getKey(), CLASS_NAME + ".layerEntry(getClass().getName());");
                insertAfter(matchingMethodEntry.getKey(), CLASS_NAME + ".layerExit(getClass().getName());", true);
            } else if (matchingMethodEntry.getValue() == OpType.EXECUTE) {
                insertAfter(matchingMethodEntry.getKey(), CLASS_NAME + ".reportResourcePath(getClass().getName(), $4 != null ? $4.getResourcePath() : null);", true);
            }
        }
        
        
        
        return true;
    }
    
    public static void layerEntry(String servletClassName) {
        Event event = Context.createEvent();
        event.addInfo("Layer", getLayerName(servletClassName),
                      "Label", "entry",
                      "ServletClass", servletClassName);

        event.report();
    }

    public static void layerExit(String servletClassName) {
        Event event = Context.createEvent();
        event.addInfo("Layer", getLayerName(servletClassName),
                      "Label", "exit");

        event.report();
    }
    
    public static void reportResourcePath(String servletClassName, String resourcePath) {
        if (resourcePath != null) {
            Event event = Context.createEvent();
            event.addInfo("Layer", getLayerName(servletClassName),
                          "Label", "info",
                          "ResourcePath",resourcePath);
                          
            event.report();
        }
    }
    
    private static String getLayerName(String servletClassName) {
        if (servletClassName.startsWith("org.apache.sling.jcr.")) {
            return SLING_LAYER_NAME;
        } else {
            return GENERAL_LAYER_NAME;
        }
    }

    
    private enum OpType {
        SERVICE, EXECUTE
    }
  
}