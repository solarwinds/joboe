package com.tracelytics.instrumentation;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;

/**
 * Some app servers (Glassfish) use OSGi and implement class loading restrictions that prevent
 * our instrumented code from accessing our com.tracelytics.* classes. We work around it by patching the Classloader.
 */
public class ClassLoaderPatcher extends ClassInstrumentation {
    private static ThreadLocal<String> currentLoadingClass = new ThreadLocal<String>();
    private static final String CLASS_NAME = ClassLoaderPatcher.class.getName();

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        if (!shouldPatch(cc)) {
            return false;
        }

        try {
            CtMethod m = null;

            m = cc.getMethod("loadClass", "(Ljava/lang/String;)Ljava/lang/Class;");
            if (shouldModify(cc, m)) {
                patchLoadClass(m);
            }
       
            m = cc.getMethod("loadClass", "(Ljava/lang/String;Z)Ljava/lang/Class;");
            if (shouldModify(cc, m)) {
                patchLoadClass(m);
            }

        } catch(CannotCompileException ex) {
            logger.debug("Unable to patch class loader: " + className, ex);
        }

        return true;
    }

    /**
     * Check whether a ClassLoader should be patched. We patch all ClassLoader to ensure consistent access to our core classes via System Class Loader
     * @param cc
     * @return whether the ClassLoader should be patched
     */
    private boolean shouldPatch(CtClass cc) {
        return true;    
    }
    
    /**
     * For all com.tracelytics classes in the agent, always delegate to the system class loader. Take note that exception is made for test package and api.ext.
     * 
     * For api.ext, the classes are included in the "api" project which is included as a jar in the WEB-INF/lib. Therefore it might only be available to the individual
     * webapp ClassLoader not the System ClassLoader.
     * 
     * @param m
     * @throws CannotCompileException
     */
    private void patchLoadClass(CtMethod m)
            throws CannotCompileException {
        insertBefore(m, "try {" +
        		        "if ($1 != null && " +
                        "    ($1.startsWith(\"com.tracelytics.\") || $1.startsWith(\"com.appoptics.\")) && " +
                        "    !$1.startsWith(\"com.tracelytics.test\") && " +
                        "    !$1.startsWith(\"com.tracelytics.api.ext\")  && " +
                        "    !$1.startsWith(\"com.appoptics.api.ext\")  && " +
                        "    !$1.startsWith(\"com.appoptics.apploader\")  && " +
                        "    this != ClassLoader.getSystemClassLoader()" + //to avoid infinite recursive calls
                        " ) { " +
                        "    Class loadedClass =  " + CLASS_NAME + ".loadBySystemClassLoader($1);" +
                        "    if (loadedClass != null) { " +
                        "        return loadedClass;" +
                        "    }" +
                        "  }" +
                        "} catch (ClassNotFoundException e) {" +
                        "  if ($1.endsWith(\"Customizer\") || $1.endsWith(\"BeanInfo\")) {" + //java.beans.Introspector might attempt to look up class with "Customizer" or "BeanInfo" that does not exist
                        "      " + //do nothing, let the original classloader load it instead (which might throw the exception again, but it is correct
                        "  } else {" +
                        "      throw e;" + //other unexpected exceptions, we should let our other exception handling handles it
                        "  }" +
                        "}",
                     false);
    }

    public static Class<?> loadBySystemClassLoader(String className) throws ClassNotFoundException {
        if (!className.equals(currentLoadingClass.get())) {
            currentLoadingClass.set(className); //set it to avoid the system cl loading below triggering recursive loading
            try {
                return ClassLoader.getSystemClassLoader().loadClass(className);
            } finally {
                currentLoadingClass.remove();
            }
        } else { //recursive call on loading this class, do not attempt to use System CL again
            return null;
        }



    }


}
