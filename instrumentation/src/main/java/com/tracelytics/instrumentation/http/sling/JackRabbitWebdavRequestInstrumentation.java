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
 * Reports the REPORT Http request handling done in org.apache.jackrabbit.webdav.WebdavRequest
 * 
 * @author pluk
 *
 */
public class JackRabbitWebdavRequestInstrumentation extends ClassInstrumentation {
    private static final String CLASS_NAME = JackRabbitWebdavRequestInstrumentation.class.getName();
    
            
    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
        new MethodMatcher<OpType>("getReportInfo", new String[] { }, "org.apache.jackrabbit.webdav.version.report.ReportInfo", OpType.REPORT)
         
    );
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        Map<CtMethod, OpType> matchingMethods = findMatchingMethods(cc, methodMatchers);
        
        for (Entry<CtMethod, OpType> matchingMethodEntry : matchingMethods.entrySet()) {
            if (matchingMethodEntry.getValue() == OpType.REPORT) {
                insertAfter(matchingMethodEntry.getKey(),
                            "if ($_ != null) {" + 
                            "    java.util.Iterator iter = $_.getPropertyNameSet().iterator();" +
                            "    java.util.List propertyNames = new java.util.ArrayList();" +
                            "    while (iter.hasNext()) {" +
                            "        propertyNames.add(((org.apache.jackrabbit.webdav.property.DavPropertyName)iter.next()).getName());" +
                            "    }" +
                                 CLASS_NAME + ".reportInfo(propertyNames, $_.getDepth(), $_.getReportName());" +
                            "}" 
                            , true);
            }
        }
        
        
        
        return true;
    }
  
    public static void reportInfo(List<String> propertyNames, int depth, String reportName) {
        Event event = Context.createEvent();
        event.addInfo("Label", "info");
                      
        if (!propertyNames.isEmpty()) {
            event.addInfo("Properties", propertyNames.toArray(new String[propertyNames.size()]));
        }
        
        if (depth == Integer.MAX_VALUE) {
            event.addInfo("Depth", "Infinity");
        } else {
            event.addInfo("Depth", depth);
        }
        
        if (reportName != null) {
            event.addInfo("ReportName", reportName);
        }
        
        event.report();
    }
    
    
    
    private enum OpType {
        REPORT
    }
  
}