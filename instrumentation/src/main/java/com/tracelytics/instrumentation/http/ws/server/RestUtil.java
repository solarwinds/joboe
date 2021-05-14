package com.tracelytics.instrumentation.http.ws.server;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;

public class RestUtil {
    protected static final Logger logger = LoggerFactory.getLogger();
    
    private RestUtil() {
    }
    
    public static final String buildTransactionName(Method resourceMethod) {
        if (resourceMethod == null) {
            return null;
        }
        Class<?> resourceClass = resourceMethod.getDeclaringClass();
        
        String classResourcePath = null;
        String methodResourcePath = null;
        
        if (resourceClass != null) {
            classResourcePath = getPathAnnotationValue(resourceClass);
        }
        methodResourcePath = getPathAnnotationValue(resourceMethod);
        
        String transactionName = buildTransactionNameByPaths(classResourcePath, methodResourcePath);
        if (transactionName == null) {
            //this is possible for default internal handling class such as `org.glassfish.jersey.server.wadl.processor.WadlModelProcessor$OptionsHandler`
            logger.debug("Failed to build transaction name for " + resourceMethod);
        }
        return transactionName;
    }
    
    private static String buildTransactionNameByPaths(String classResourcePath, String methodResourcePath) {
        if ((classResourcePath == null || "".equals(classResourcePath.trim())) &&
            (methodResourcePath == null || "".equals(methodResourcePath.trim()))) {
            return null;
        }
        
        if (classResourcePath != null) {
            classResourcePath = formatPath(classResourcePath);
        } else {
            classResourcePath = "";
        }
        
        if (methodResourcePath != null) {
            methodResourcePath = formatPath(methodResourcePath);
        } else {
            methodResourcePath = "";
        }
        
        return classResourcePath + methodResourcePath;
    }

    /**
     * Removes regular expression in the variable definition and also replace {var} with :var
     * 
     * For example : 
     * "/users/{username: [a-zA-Z][a-zA-Z_0-9]}/profile" => "/users/:username/profile" 
     * "/users/{username}" => "/users/:username"
     * 
     * @param path
     * @return
     */
    static String formatPath(String path) {
        //remove all space
        path = path.replace(" ", "");
        
        int colonIndex = path.indexOf(':');
        if (colonIndex == -1) { //no regex, easy handling
            //replace all '{' with ':'
            path = path.replace('{', ':');
            
            //remove all '}' 
            path = path.replace("}", "");
        } else {
            path = formatRegexPath(path);
        }
        
        return path;
    }
    
    
    /**
     * Use simple state machine to remove regex portion. Take note that the full parsing can be very complicated,
     * see `org.glassfish.jersey.uri.internal.UriTemplateParser` 
     * @param path
     * @return
     */
    private static String formatRegexPath(String path) {
        return new StateMachine(path).getResult();
    }
    
    private static class StateMachine {
        private enum State {
            SIMPLE,
            IN_VARIABLE,
            IN_REGEX,
            IN_REGEX_GREEDY
        }
        
        private State state = State.SIMPLE;
        private StringBuilder result = new StringBuilder();
        
        public StateMachine(String input) {
            for (char character : input.toCharArray()) {
                state = consume(character);
            }
        }
        
        private State consume(char character) {
            switch(state) {
            case SIMPLE:
                if (character == '{') {
                    result.append(':');
                    return State.IN_VARIABLE;
                } else {
                    result.append(character);
                    return State.SIMPLE;
                }
            case IN_VARIABLE:
                if (character == '}') { //do not append anything
                    return State.SIMPLE;
                } else if (character == ':'){
                    return State.IN_REGEX;
                } else {
                    result.append(character);
                    return State.IN_VARIABLE;
                }
            case IN_REGEX:
                if (character == '{') {
                    return State.IN_REGEX_GREEDY;
                } else if (character == '}') { //do not append anything
                    return State.SIMPLE;
                } else { //do not append anything
                    return State.IN_REGEX;
                }
            case IN_REGEX_GREEDY:
                if (character == '}') {
                    return State.IN_REGEX;
                } else {
                    return State.IN_REGEX_GREEDY;
                }
            default:
                return state;
            }
            
        }
        
        private String getResult() {
            return result.toString();
        }
    }

    private static final String getPathAnnotationValue(AnnotatedElement element) {
        for (Annotation annotation : element.getDeclaredAnnotations()) {
            String annotationType = annotation.annotationType().getName();
            if ("javax.ws.rs.Path".equals(annotationType) ||  "jakarta.ws.rs.Path".equals(annotationType)) {
                Object pathObject = element.getAnnotation(annotation.annotationType());
                try {
                    return (String) pathObject.getClass().getMethod("value").invoke(pathObject);
                } catch (Exception e) {
                    logger.warn("Failed to build transaction name from " + element + " message : " + e.getMessage(), e);
                    return null;
                }
            }
        }
        return null;
   }
}
