package com.tracelytics.instrumentation.http;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;

/**
 * Instruments Spring Views - if url is available, use that as "View" KV; otherwise use the class name
 */
public class SpringViewInstrumentation extends ClassInstrumentation {
    public static final String CLASS_NAME = SpringViewInstrumentation.class.getName();

    // templateViewMethod contains the mapping of a template engine's view class and the method
    // which can be used to get the view/template name.
    private static final Map<String, String> templateViewMethod;

    private static List<MethodMatcher<Object>> methodMatchers = Collections.singletonList(new MethodMatcher<Object>("render", new String[]{"java.util.Map", "javax.servlet.http.HttpServletRequest", "javax.servlet.http.HttpServletResponse"}, "void"));

    static {
        Map<String, String> m = new HashMap<String, String>();
        m.put("org.thymeleaf.spring3.view.AbstractThymeleafView", "getTemplateName()");
        m.put("org.thymeleaf.spring4.view.AbstractThymeleafView", "getTemplateName()");
        m.put("org.thymeleaf.spring5.view.AbstractThymeleafView", "getTemplateName()");
        m.put("org.springframework.web.servlet.view.AbstractUrlBasedView", "getUrl()");

        templateViewMethod = Collections.unmodifiableMap(m);
    }

    public interface SpringView {
        String tvGetViewName();
    }

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        // Patch the class to get the view name with a unified method for (hopefully)
        // all different template engines. The view name will be used in reporting the
        // `View` KV in the entry event.
        insertTvGetViewName(cc);

        Set<CtMethod> renderMethods = findMatchingMethods(cc, methodMatchers).keySet();

        for (CtMethod renderMethod : renderMethods) {
            modifyRender(renderMethod);
        }

        return true;
    }

    private void insertTvGetViewName(CtClass cc) throws CannotCompileException {
        String methodBody = "public String tvGetViewName() { return " + getViewMethodInvocation(cc) + "; }";
        cc.addMethod(CtNewMethod.make(methodBody, cc));
        // let it implements the functional interface SpringViewGetter
        try {
            tagInterface(cc, SpringView.class.getName());
        } catch (NotFoundException e) {
            // it can be safely ignored.
            logger.debug(e.getMessage());
        }
    }

    private String getViewMethodInvocation(CtClass cc) {
        for (Map.Entry<String, String> entry : templateViewMethod.entrySet()) {
            try {
                CtClass cls = classPool.getCtClass(entry.getKey());
                if (cc.subtypeOf(cls)) {
                    logger.debug("Use method " + entry.getValue() + "to get view name for class " + cc.getName());
                    return entry.getValue();
                }
            } catch (NotFoundException e) {
                // ignore this exception and try the next one
            }
        }
        // the fallback method if no better candidate is found.
        return "this.getClass().getName()";
    }

    private void modifyRender(CtMethod method)
            throws CannotCompileException {
        insertBefore(method, CLASS_NAME + ".renderEntry(this);");
        insertAfter(method, CLASS_NAME + ".renderExit();", true);
    }

    public static void renderEntry(Object viewObject) {
        Event entry = Context.createEvent();
        entry.addInfo("Layer", "spring-render",
                "Label", "entry");

        if (viewObject instanceof SpringView) {
            entry.addInfo("View", ((SpringView) viewObject).tvGetViewName());
        } else {
            entry.addInfo("View", viewObject.getClass().getName());
        }

        entry.addInfo("ViewClass", viewObject.getClass().getName());

        entry.report();
    }

    public static void renderExit() {
        Event exit = Context.createEvent();
        exit.addInfo("Layer", "spring-render",
                "Label", "exit");
        exit.report();
    }
}
