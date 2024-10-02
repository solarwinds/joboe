package com.tracelytics.instrumentation.http.play;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.Module;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;

/**
 * Instruments Play controller method in Java template. Since we do not know ahead of time what methods are really the ones that the "route" config maps to, we would have to make
 * guesses by only include methods that returns play.mvc.Result. 
 * 
 * Take note that this approach might produce nested instrumentation, but based on testing on sample projects so far, this does not seem to be common
 *   
 * @author pluk
 *
 */
public class PlayControllerInstrumentation extends PlayBaseInstrumentation {

    private static String CLASS_NAME = PlayControllerInstrumentation.class.getName();
    private static String TEMPLATE_LAYER_NAME = "play-template";
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        
        
        if (getPlayMajorVersion() >= 2) { //play 2+
            logger.debug("Play MVC 2+ Java controller profiles are instrumented in another class");
        } else { //play 1
            instrumentPlay1(cc);
        }

        return true;
    }
    
       
    /**
     * Only instruments the render template method for Play 1. Do not instrument controller method due to https://github.com/tracelytics/joboe/issues/254
     * @param method
     * @throws CannotCompileException
     * @throws NotFoundException 
     */
    private void instrumentPlay1(CtClass controllerClass) throws CannotCompileException, NotFoundException {
        CtMethod renderTemplateMethod = controllerClass.getMethod("renderTemplate", "(Ljava/lang/String;Ljava/util/Map;)V");
        if (shouldModify(controllerClass, renderTemplateMethod)) {
            insertBefore(renderTemplateMethod, CLASS_NAME + ".renderEntry(\"" + renderTemplateMethod.getName() + "\", \"" +  renderTemplateMethod.getDeclaringClass().getName() + "\", $1);");
            insertAfter(renderTemplateMethod, CLASS_NAME + ".renderExit(\"" + renderTemplateMethod.getName() + "\", \"" +  renderTemplateMethod.getDeclaringClass().getName()  + "\", $1);", true);
        }
    }
    
    /**
     * Play 1 template entry. Report both a layer and profile entry event. In Play 1 there could only be 1 template rendering per request (compared to multiple nested ones in Play 2).
     * For consistency, we will still create a play-template layer
     * 
     * @param methodName
     * @param className
     * @param templateName
     */
    public static void renderEntry(String methodName, String className, String templateName) {
        Event renderEntry = Context.createEvent();
        renderEntry.addInfo("Layer", TEMPLATE_LAYER_NAME,
                             "Label", "entry",
                             "Language", "java");
        
        if (className != null) {
            renderEntry.addInfo("Class", className);
        } 
        if (methodName != null) {
            renderEntry.addInfo("FunctionName", methodName);
        }
        if (templateName != null) {
            renderEntry.addInfo("TemplateName", templateName);
        }
        addBackTrace(renderEntry, 1, Module.PLAY);
            
        renderEntry.report();
    }

    /**
     * Play 1 template exit. Report both a layer and profile exit event. In Play 1 there could only be 1 template rendering per request (compared to multiple nested ones in Play 2).
     * For consistency, we will still create a play-template layer
     * 
     * @param methodName
     * @param className
     * @param templateName
     */
    public static void renderExit(String methodName, String className, String templateName) {
        Event renderExit = Context.createEvent();
        renderExit.addInfo("Layer", TEMPLATE_LAYER_NAME,
                             "Label", "exit");
        renderExit.report();
    }
}