package com.tracelytics.instrumentation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.tracelytics.ext.javassist.ClassPool;
import com.tracelytics.ext.javassist.CtBehavior;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;

/**
 * Matcher to match against CtMethod with the given classpool.
 * 
 * This is used to allow a less restrictive matching than direct method signature matching via CtClass.getMethod(methodName, methodSignature), such that we 
 * do not have to list every single method signature across different versions of frameworks.
 * 
 * The difference of this matcher compared to CtClass.getMethod(methodName, methodSignature): 
 * 1. Instead of exact class match on the return class, it is considered a match if the method's return Type is a sub-type (including itself) of the one declared in MethodMatcher
 * 2. Instead of exact signature match on the parameter list, it is considered a match if the param is a sub-type (including itself) of the ones declared in MethodMatcher
 * 3. It is considered a match if the param's sublist matches the ones declared in MethodMatcher even if the list sizes are different (unless matchParamCount is true). The sublist should always starts with the first param.  
 * 
 * For example for MethodMatcher with methodName "test", paramTypes ["String", "Object", "int"] and returnType "Collection"
 *  
 *  When matches() is invoked against methods as below: 
 * 1. Collection test(String, Object) - does not match, ["String, "Object"] is not a sublist of ["String", "Object", "int"]
 * 2. Collection test(String, Object, int) - matches
 * 3. Collection test(String, Object, int, int) - matches, ["String, "Object", int, int] is a sublist of ["String", "Object", "int"]
 * 4. Collection test(String, String, int) - matches, the 2nd param "String" is a sub-type of "Object"
 * 5. Collection test(int, Object, int) - does not match, the 1st param int is not a sub-type of "String"
 * 6. List test(String, Object, int) - matches, the return type "List" is a sub-type of "Collection" 
 *  
 * 
 * @author Patson Luk
 *
 * @param <T> a type to be associated with the matcher  
 */
public class MethodMatcher<T> {
    private Logger logger = LoggerFactory.getLogger();
    
    
    private String methodName;
    private String[] paramTypes ;
    private String returnType;
    
    private T instType;
    
    private boolean matchParamCount;
    
    /**
     * 
     * @param methodName
     * @param paramTypes an array of param types in fully qualified class names. Take note that primitive type uses the same name as in java syntax (int, short, long etc)
     * @param returnType return type in fully qualified class name. Take note that primitive type uses the same name as in java syntax (int, short, long etc). For void return, use "void"
     */
    public MethodMatcher(String methodName, String[] paramTypes, String returnType) {
        this(methodName, paramTypes, returnType, null);
    }
    
    /**
     * 
     * @param methodName
     * @param paramTypes
     * @param returnType    return type of the method, null if void
     * @param instType      a type to be associated with this matcher, usually is the user defined category that needs to tagged to the method matched
     */
    public MethodMatcher(String methodName, String[] paramTypes, String returnType, T instType) {
        this(methodName, paramTypes, returnType, instType, false);
    }
    
    /**
     * 
     * @param methodName
     * @param paramTypes
     * @param returnType        return type of the method, null if void
     * @param instType          a type to be associated with this matcher, usually is the user defined category that needs to tagged to the method matched
     * @param matchParamCount   whether method has to match exactly the number of params defined in paramTypes
     */
    public MethodMatcher(String methodName, String[] paramTypes, String returnType, T instType, boolean matchParamCount) {
        this.methodName = methodName;
        this.paramTypes = paramTypes;
        this.returnType = returnType;
        this.instType = instType;
        this.matchParamCount = matchParamCount;
    }
    
    /**
     * 
     * @param method
     * @param classPool
     * @return true if method or constructor is a match to the pattern defined in this MethodMatcher
     */
    public boolean matches(CtBehavior method, ClassPool classPool) {
        //Compare name first, this is the cheapest comparison
        if (method instanceof CtMethod && !methodName.equals(method.getName())) {
            return false;
        }
        
        //now compare param and return type which requires CtClass
        CtClass returnTypeCtClass;
        List<CtClass> paramTypeCtClasses;
        
        try {
            if (returnType != null) {
                returnTypeCtClass = classPool.get(returnType);
            } else {
                logger.warn("Found return type declared as null in matcher, use \"void\" instead!");
                returnTypeCtClass = classPool.get("void");
            }
            
            paramTypeCtClasses = new ArrayList<CtClass>();
            
            for (String paramType : paramTypes) {
                paramTypeCtClasses.add(classPool.get(paramType));
            }
            
        } catch (NotFoundException e) {
            logger.debug("Cannot match against " + toString() + " As one of the types cannot be loaded: [" + e.getMessage() + "] perhaps the framework is on an earlier version...");
            return false;
        }
            
            
        try {
            //compare return type only on methods (not constructors)
            if (method instanceof CtMethod && !((CtMethod)method).getReturnType().subtypeOf(returnTypeCtClass)) {
                return false; 
            }
        } catch (NotFoundException e) {
            logger.warn("Error loading return type: " + e.getMessage(), e);
            
            return false;
        }
            
        CtClass[] inputMethodParamTypes;
        try {
            inputMethodParamTypes = method.getParameterTypes();
            
            
            if (matchParamCount) { //has to exactly match the param list defined
                if (paramTypeCtClasses.size() != inputMethodParamTypes.length) {
                    return false;
                }
            } else { //Make sure defined param list is the sublist of the matching CtMethod.
                if (paramTypeCtClasses.size() > inputMethodParamTypes.length) {
                    return false;
                }
            }
        } catch (NotFoundException e) {
            logger.warn("Error loading param type: " + e.getMessage(), e);
            
            return false;
        }
            
        try {
          //compare param type 
            for (int i = 0 ; i < paramTypeCtClasses.size(); i++) {
                CtClass paramTypeCtClass = paramTypeCtClasses.get(i);
                
                if (!inputMethodParamTypes[i].subtypeOf(paramTypeCtClass)) {
                    return false;
                }
            }
        } catch (NotFoundException e) {
            logger.debug("Cannot check param types :" + e.getMessage(), e);
            return false;
        }
            
        return true;
    }
    
    
    public T getInstType() {
        return instType;
    }

    @Override
    public String toString() {
        return "method methodName [" + methodName + "] param types " + Arrays.toString(paramTypes) + " return type [" + returnType + "]";
    }   
    
    public String getMethodName() {
        return methodName;
    }
    
    public String getReturnType() {
        return returnType;
    }
}
