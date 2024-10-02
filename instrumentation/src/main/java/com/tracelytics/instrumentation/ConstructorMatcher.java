package com.tracelytics.instrumentation;

/**
 * A specialized version of {@link MethodMatcher} that represents Constructor matching
 * @author pluk
 *
 * @param <T>
 */
public class ConstructorMatcher<T> extends MethodMatcher<T>{
    public ConstructorMatcher(String[] paramTypes) {
        this(paramTypes, null);
    }

    public ConstructorMatcher(String[] paramTypes, T instType) {
        this(paramTypes, instType, false);
    }
    
    public ConstructorMatcher(String[] paramTypes, T instType, boolean matchParamCount) {
        super("", paramTypes, "void", instType, matchParamCount);
        //public MethodMatcher(String methodName, String[] paramTypes, String returnType, T instType, boolean matchParamCount) {        
    }
    
}
