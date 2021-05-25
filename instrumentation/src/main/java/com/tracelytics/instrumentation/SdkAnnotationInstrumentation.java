package com.tracelytics.instrumentation;

import java.util.List;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.ClassPool;
import com.tracelytics.ext.javassist.CtBehavior;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.ext.javassist.bytecode.AccessFlag;
import com.tracelytics.ext.javassist.bytecode.annotation.Annotation;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;
import com.tracelytics.joboe.EventValueConverter;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;

/**
 * Changes bytecode of concrete method that is annotated by our SDK annotation LogMethod or ProfileMethod
 * 
 * @author pluk
 *
 */
public class SdkAnnotationInstrumentation extends AnnotatedMethodsInstrumentation {
    private static final String CLASS_NAME = SdkAnnotationInstrumentation.class.getName();
    private static EventValueConverter converter = new EventValueConverter();
    
    @Override
    public boolean applyInstrumentation(ClassPool classPool, CtClass cc, List<AnnotatedMethod> annotatedMethods) throws Exception {
        boolean modified = false;
        String className = cc.getName();
        for (AnnotatedMethod annotatedMethod : annotatedMethods) {
            CtMethod method = annotatedMethod.getMethod();
            Annotation annotation = annotatedMethod.getAnnotation();
            if (ClassInstrumentation.shouldModify(cc, method) && !isSyntheticMethod(method)) { // do not process annotation from synthetic method, other it could inject code twice
                if (annotation.getTypeName().equals("com.tracelytics.api.ext.LogMethod") || annotation.getTypeName().equals("com.appoptics.api.ext.LogMethod")) {
                    logger.debug("Adding logging to method [" + method.getName() + "] with signature [" + method.getSignature() + "] of class [" + className + "]");
                    addLogMethod(method, cc, getAnnotationWithDefault(annotation, classPool));
                    modified = true;
                } else if (annotation.getTypeName().equals("com.tracelytics.api.ext.ProfileMethod") || annotation.getTypeName().equals("com.appoptics.api.ext.ProfileMethod")) {
                    logger.debug("Adding profiling to method [" + method.getName() + "] with signature [" + method.getSignature() + "] of class [" + className + "]");
                    addProfileMethod(method, cc, getAnnotationWithDefault(annotation, classPool));
                    modified = true;
                }
            }
        }

        return modified;
    }

    private static boolean isSyntheticMethod(CtMethod method) {
        return (method.getMethodInfo().getAccessFlags() & AccessFlag.SYNTHETIC) != 0;
    }

    private static void addLogMethod(CtBehavior m, CtClass cc, AnnotationWithDefault annotation) throws CannotCompileException, NotFoundException {
        String layer = (String) annotation.getMemberValue("layer");
        boolean backTrace = (Boolean) annotation.getMemberValue("backTrace");
        boolean storeReturn = (Boolean) annotation.getMemberValue("storeReturn");
        boolean reportExceptions = (Boolean) annotation.getMemberValue("reportExceptions");

        if (layer == null || layer.equals("")) {
            layer = (String) ConfigManager.getConfig(ConfigProperty.AGENT_LAYER);
        }

        layer = layer.replace("\"", "\\\"");

        ClassInstrumentation.insertBefore(m, CLASS_NAME + ".logMethodEntry(\"" + layer + "\",\"" + cc.getName() + "\",\"" + m.getName() + "\"," + backTrace + ");");
        ClassInstrumentation.insertAfter(m, CLASS_NAME + ".logMethodExit(\"" + layer + "\"," + storeReturn + ", ($w)$_);", true);

        if (reportExceptions) {
            ClassInstrumentation.addErrorReporting(m, "java.lang.Throwable", layer, cc.getClassPool());
        }
    }

    public static void logMethodEntry(String layer, String className, String methodName, boolean backTrace) {
        Event event = Context.createEvent();
        event.addInfo("Layer", layer, "Class", className, "MethodName", methodName, "Label", "entry");

        if (backTrace) {
            ClassInstrumentation.addBackTrace(event, 1, null);
        }

        event.report();
    }

    public static void logMethodExit(String layer, boolean storeReturn, Object ret) {
        Event event = Context.createEvent();
        event.addInfo("Layer", layer, "Label", "exit");

        if (storeReturn && ret != null) {
            event.addInfo("ReturnValue", converter.convertToEventValue(ret));
        }

        event.report();
    }

    private static void addProfileMethod(CtBehavior m, CtClass cc, AnnotationWithDefault annotation) throws CannotCompileException {
        String profileName = (String) annotation.getMemberValue("profileName");
        if (profileName == null) {
            logger.warn("Profile name should not be null for annotation on " + m);
            return;
        }
        profileName = profileName.replace("\"", "\\\"");

        boolean backTrace = (Boolean) annotation.getMemberValue("backTrace");
        boolean storeReturn = (Boolean) annotation.getMemberValue("storeReturn");

        String sourceFile = cc.getClassFile().getSourceFile();
        int lineNumber = m.getMethodInfo().getLineNumber(0);

        ClassInstrumentation.insertBefore(m, CLASS_NAME + ".profileMethodEntry(\"" + profileName + "\",\"" + cc.getName() + "\",\"" + m.getName() + "\",\"" + m.getSignature() + "\",\"" + sourceFile
                + "\"," + lineNumber + "," + backTrace + ");");
        ClassInstrumentation.insertAfter(m, CLASS_NAME + ".profileMethodExit(\"" + profileName + "\"," + storeReturn + ", ($w)$_);", true);
    }

    public static void profileMethodEntry(String profileName, String className, String methodName, String signature, String sourceFile, int lineNumber, boolean backTrace) {

        Event event = Context.createEvent();
        event.addInfo("Layer", profileName,
                "Class", className,
                "FunctionName", methodName,
                "Signature", signature,
                "File", sourceFile,
                //"LineNumber", lineNumber, // excluding for now, does not seem to be accurate
                "Label", "entry");
        
        if (backTrace) {
            ClassInstrumentation.addBackTrace(event, 1, null);
        }

        event.report();
    }

    public static void profileMethodExit(String profileName, boolean storeReturn, Object ret) {
        Event event = Context.createEvent();
        event.addInfo("Layer", profileName,
                      "Label", "exit");

        if (storeReturn && ret != null) {
            event.addInfo("ReturnValue", converter.convertToEventValue(ret));
        }

        event.report();
    }
}
