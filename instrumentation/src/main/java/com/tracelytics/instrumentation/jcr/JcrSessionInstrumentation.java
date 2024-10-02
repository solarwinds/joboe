package com.tracelytics.instrumentation.jcr;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.Module;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;

/**
 * Instruments the save operation done in {@code javax.jcr.Session}. Take note that the node operations are NOT instrumented now as they are invoked very often hence deem too noisy.
 *  
 * @author pluk
 *
 */
public class JcrSessionInstrumentation extends ClassInstrumentation {
    private static final String LAYER_NAME = "jcr";
    private static final String CLASS_NAME = JcrSessionInstrumentation.class.getName();  
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
                                                                              //new MethodMatcher<Object>("addNode", new String[] { }, "javax.jcr.Node"),
//                                                                              new MethodMatcher<OpType>("getNode", new String[] { "java.lang.String" }, "javax.jcr.Node", OpType.GET_NODE_PATH),
//                                                                              new MethodMatcher<OpType>("getNodeByIdentifier", new String[] { "java.lang.String" }, "javax.jcr.Node", OpType.GET_NODE_ID),
//                                                                              new MethodMatcher<OpType>("getNodeByUUID", new String[] { "java.lang.String" }, "javax.jcr.Node", OpType.GET_NODE_ID),
//                                                                              new MethodMatcher<OpType>("getItem", new String[] { "java.lang.String" }, "javax.jcr.Item", OpType.GET_ITEM),
//                                                                              new MethodMatcher<OpType>("getProperty", new String[] { "java.lang.String" }, "javax.jcr.Property", OpType.GET_PROPERTY),
//                                                                              new MethodMatcher<OpType>("itemExists", new String[] { "java.lang.String" }, "boolean", OpType.ITEM_EXISTS),
                                                                              new MethodMatcher<OpType>("save", new String[] { }, "void", OpType.SAVE)
                                                                          );
    
    private static ThreadLocal<Integer> depthThreadLocal = new ThreadLocal<Integer>() {
        protected Integer initialValue() {
            return 0;
        }
    };
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        
        for (Entry<CtMethod, OpType> methodEntry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            CtMethod method = methodEntry.getKey();
            
            if (methodEntry.getValue() == OpType.GET_NODE_PATH) {
                insertBefore(method,  CLASS_NAME + ".layerEntry($1, null, \"" + method.getName() + "\");");
                insertAfter(method,  CLASS_NAME + ".layerNodeExit($_ != null && $_.getPrimaryNodeType() != null ? $_.getPrimaryNodeType().getName() : null);", true);
            } else if (methodEntry.getValue() == OpType.GET_NODE_ID) {
                insertBefore(method,  CLASS_NAME + ".layerEntry(null, $1, \"" + method.getName() + "\");");
                insertAfter(method,  CLASS_NAME + ".layerNodeExit($_ != null && $_.getPrimaryNodeType() != null ? $_.getPrimaryNodeType().getName() : null);", true);
            } else if (methodEntry.getValue() == OpType.GET_PROPERTY) {
                insertBefore(method, CLASS_NAME + ".layerEntry($1, null, \"" + method.getName() + "\");");
                insertAfter(method, CLASS_NAME + ".layerPropertyExit($_ != null ? $_.getLength() : -1);", true);
            } else if (methodEntry.getValue() == OpType.GET_ITEM) {
                insertBefore(method, CLASS_NAME + ".layerEntry($1, null, \"" + method.getName() + "\");");
                insertAfter(method, 
                            "if ($_ instanceof javax.jcr.Node) { " +
                            "    javax.jcr.Node node = (javax.jcr.Node)$_;" +
                                 CLASS_NAME + ".layerNodeExit(node.getPrimaryNodeType() != null ? node.getPrimaryNodeType().getName() : null);" +
                            "} else if ($_ instanceof javax.jcr.Property) {" +
                            "    javax.jcr.Property property = (javax.jcr.Property)$_;" +
                                 CLASS_NAME + ".layerPropertyExit(property.getLength());" +
                            "} else {" +
                                 CLASS_NAME + ".layerExit();" +
                            "}", true);
                            
                            
            } else if (methodEntry.getValue() == OpType.ITEM_EXISTS) {
                insertBefore(method,  CLASS_NAME + ".layerEntry($1, null, \"" + method.getName() + "\");");
                insertAfter(method,  CLASS_NAME + ".layerBooleanExit($_);", true);
            } else if (methodEntry.getValue() == OpType.SAVE) {
                insertBefore(method,  CLASS_NAME + ".layerEntry(null, null, \"" + method.getName() + "\");");
                insertAfter(method,  CLASS_NAME + ".layerExit();", true);
            } else {
                logger.warn("Unknown JCR Session OpType [" + methodEntry.getValue() + "] on method [" + method.getName() + "]");
                insertBefore(method,  CLASS_NAME + ".layerEntry(null, null, \"" + method.getName() + "\");");
                insertAfter(method,  CLASS_NAME + ".layerExit();", true);
            }
            
        }
      
        
        
        return true;
    }
    
    public static void layerEntry(String path, String id, String method) {
        if (shouldStartExtent()) {
            Event event = Context.createEvent();
            event.addInfo("Layer", LAYER_NAME,
                          "Label", "entry",
                          "Query", method,
                          "Flavor", "jcr");
                          
            if (path != null) {
                event.addInfo("NodePath", path);
            }
            if (id != null) {
                event.addInfo("NodeId", id);
            }
            
            addBackTrace(event, 1, Module.JCR);
            
            event.report();
        }
    }
    
    public static void layerExit() {
        if (shouldEndExtent()) {
            Event event = Context.createEvent();
            event.addInfo("Layer", LAYER_NAME,
                          "Label", "exit");
                          
            
            event.report();
        }
    }
    
//    public static void layerNodeExit(String nodeType) {
//        if (shouldEndExtent()) {
//            Event event = Context.createEvent();
//            event.addInfo("Layer", LAYER_NAME,
//                          "Label", "exit",
//                          "NodeType", nodeType);
//                          
//            
//            event.report();
//        }
//    }
//    
//    public static void layerPropertyExit(long propertyLength) {
//        if (shouldEndExtent()) {
//            Event event = Context.createEvent();
//            event.addInfo("Layer", LAYER_NAME,
//                          "Label", "exit",
//                          "PropertyLength", propertyLength);
//                          
//            
//            event.report();
//        }
//    }
//    
//    public static void layerBooleanExit(boolean isExists) {
//        if (shouldEndExtent()) {
//            Event event = Context.createEvent();
//            event.addInfo("Layer", LAYER_NAME,
//                          "Label", "exit");
//            
//            event.addInfo("isExists", isExists);
//            event.report();
//        }
//    }
//  
    
    /**
     * Checks whether the current instrumentation should start a new extent. If there is already an active Hbase extent, then do not start one
     * @return
     */
    protected static boolean shouldStartExtent() {
        int currentDepth = depthThreadLocal.get();
        depthThreadLocal.set(currentDepth + 1);

        if (currentDepth == 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Checks whether the current instrumentation should end the current extent. If this is the active Hbase extent being traced, then ends it
     * @return
     */
    protected static boolean shouldEndExtent() {
        int currentDepth = depthThreadLocal.get();
        depthThreadLocal.set(currentDepth - 1);

        if (currentDepth == 1) {
            return true;
        } else {
            return false;
        }
    }
    
    private enum OpType { SAVE, ITEM_EXISTS, GET_PROPERTY, GET_NODE_PATH, GET_ITEM, GET_NODE_ID }
    
    
    

}