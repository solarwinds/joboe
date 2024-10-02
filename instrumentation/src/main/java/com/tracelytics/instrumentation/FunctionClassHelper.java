package com.tracelytics.instrumentation;

import com.tracelytics.ext.javassist.*;
import com.tracelytics.ext.javassist.bytecode.ClassFile;

/**
 * Helper class to create a function class without triggering the ReflectiveAccess
 *
 * See https://github.com/librato/joboe/pull/1011 for details
 *
 * Do not use class that starts with "java" as the neighborTypeName, as it might run into package name not allowed exception for JDK8-
 */
public class FunctionClassHelper {
    private final Class<?> neighborType;
    private final CtClass createdFunctionClass;
    private FunctionClassHelper(ClassPool classPool, String neighborTypeName, String superTypeName, String functionClassSimpleName) throws ClassNotFoundException, NotFoundException, CannotCompileException {
        this.neighborType = classPool.getClassLoader().loadClass(neighborTypeName);

        final String functionClassQualifiedName = neighborType.getPackage().getName() + "." + functionClassSimpleName;

        createdFunctionClass = classPool.makeClass(functionClassQualifiedName);
        CtClass superType = classPool.get(superTypeName);
        if (superType.isInterface()) {
            createdFunctionClass.addInterface(superType);
        } else {
            createdFunctionClass.setSuperclass(superType);
        }
    }

    /**
     *
     * @param classPool
     * @param superTypeName The superType to be implemented/extended by this new class
     * @param neighborTypeName  Private Lookup by using this class and the new class will be created using the same package as this.
     *                          do NOT use package name that starts with "java".
     * @param functionClassSimpleName
     * @return
     * @throws CannotCompileException
     * @throws NotFoundException
     * @throws ClassNotFoundException
     */
    public static final FunctionClassHelper getInstance(ClassPool classPool, String neighborTypeName, String superTypeName, String functionClassSimpleName) throws CannotCompileException, NotFoundException, ClassNotFoundException {
        return new FunctionClassHelper(classPool, neighborTypeName, superTypeName, functionClassSimpleName);
    }

    /**
     *
     * @param classPool
     * @param superTypeName The superType to be implemented/extended by this new class, this will also be used as the
     *                      `neighborTypeName`.
     *                      Take note that this should NOT be a class that starts with package name "java".
     *                      In order to1 implement/extend "java" class, explicitly provide neighborTypeName that does not
     *                      start with package name "java"
     * @param functionClassSimpleName
     * @return
     * @throws CannotCompileException
     * @throws NotFoundException
     * @throws ClassNotFoundException
     */
    public static final FunctionClassHelper getInstance(ClassPool classPool, String superTypeName, String functionClassSimpleName) throws CannotCompileException, NotFoundException, ClassNotFoundException {
        return getInstance(classPool, superTypeName, superTypeName, functionClassSimpleName);
    }

    public CtClass getFunctionCtClass() {
        return createdFunctionClass;
    }

    public Class<?> toFunctionClass() throws CannotCompileException {
        if (ClassFile.MAJOR_VERSION >= ClassFile.JAVA_9) {
            return createdFunctionClass.toClass(neighborType);
        } else {
            return createdFunctionClass.toClass();
        }
    }
}
