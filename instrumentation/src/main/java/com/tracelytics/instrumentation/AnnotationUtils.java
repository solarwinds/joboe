package com.tracelytics.instrumentation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.tracelytics.ext.javassist.CtBehavior;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.ext.javassist.bytecode.AnnotationsAttribute;
import com.tracelytics.ext.javassist.bytecode.ClassFile;
import com.tracelytics.ext.javassist.bytecode.MethodInfo;
import com.tracelytics.ext.javassist.bytecode.annotation.Annotation;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;

/**
 * Helpers for annotation lookup from javassist objects
 * @author pluk
 *
 */
public class AnnotationUtils {
    private static final Logger logger = LoggerFactory.getLogger();
    private AnnotationUtils() {
        
    }
    
    /**
     * Extracts {@link com.tracelytics.ext.javassist.bytecode.annotation.Annotation} from the CtBehavior.
     * 
     * Code is extracted from the library method <code>com.tracelytics.ext.javassist.CtBehavior.getAnnotations(boolean, Collection<String>, ClassLoader)</code>
     * and <code>com.tracelytics.ext.javassist.CtClassType.toAnnotationType(boolean, Collection<String>, ClassLoader, ClassPool, AnnotationsAttribute, AnnotationsAttribute)</code>
     * directly, as we do not want to modify the library itself if possible.
     * 
     * @param behavior
     * @return
     */
    public static List<Annotation> getAnnotationsFromBehavior(CtBehavior behavior) {
        return getAnnotationsFromBehavior(behavior, false);
    }
    
    /**
     * Extracts {@link com.tracelytics.ext.javassist.bytecode.annotation.Annotation} from the CtBehavior. This includes annotations of the overridden methods from super types if includeSuperTypes is true.
     *  
     * Code is extracted from the library method <code>com.tracelytics.ext.javassist.CtBehavior.getAnnotations(boolean, Collection<String>, ClassLoader)</code>
     * and <code>com.tracelytics.ext.javassist.CtClassType.toAnnotationType(boolean, Collection<String>, ClassLoader, ClassPool, AnnotationsAttribute, AnnotationsAttribute)</code>
     * directly, as we do not want to modify the library itself if possible.
     * 
     * @param behavior
     * @param includeSuperTypes whether includes annotations of the overridden methods from super types
     * @return
     */
    public static List<Annotation> getAnnotationsFromBehavior(CtBehavior behavior, boolean includeSuperTypes) {
        MethodInfo methodInfo = behavior.getMethodInfo2();
        
        AnnotationsAttribute ainfo = (AnnotationsAttribute)
                    methodInfo.getAttribute(AnnotationsAttribute.invisibleTag);  
        AnnotationsAttribute ainfo2 = (AnnotationsAttribute)
                    methodInfo.getAttribute(AnnotationsAttribute.visibleTag);
        
        List<Annotation> annotations = getAnnotationsFromAttributes(ainfo, ainfo2);
        
        if (includeSuperTypes) {
            annotations = new ArrayList<Annotation>(annotations); //make it mutable
            for (CtClass superType : getAllSuperTypes(behavior.getDeclaringClass())) {
                try {
                    CtMethod overriddenMethod = superType.getDeclaredMethod(behavior.getName(), behavior.getParameterTypes());
                    annotations.addAll(getAnnotationsFromBehavior(overriddenMethod, false));
                } catch (NotFoundException e) {
                    //ok, just no such a method overridden
                }
            }
        }
        
        return annotations;
    }
    
    
    private static Set<CtClass> getAllSuperTypes(CtClass clazz) {
        Set<CtClass> superTypes = new HashSet<CtClass>();
        Set<CtClass> interfaces = new HashSet<CtClass>();
        
        //interfaces from this class
        try {
            getAllInterfaces(clazz, interfaces);
        } catch (NotFoundException e) {
            logger.debug("Failed traversing interfaces on [" + clazz.getName() + "] : " + e.getMessage());
        }
        
     // check the superclasses:
        CtClass superClass = null;
        try {
            superClass = clazz.getSuperclass();
            while(superClass != null) {
                superTypes.add(superClass);
                try {
                    getAllInterfaces(superClass, interfaces);
                } catch (NotFoundException e) {
                    logger.debug("Failed traversing interfaces on [" + superClass.getName() + "] : " + e.getMessage());
                }
                superClass = superClass.getSuperclass();
            }
        } catch (NotFoundException e) {
            logger.debug("Failed traversing super classs on [" + (superClass != null ? superClass.getName() : clazz.getName()) + "] : " + e.getMessage());
        }
        
        superTypes.addAll(interfaces);
        
        return superTypes;
    }

    public static List<Annotation> getAnnotationsFromType(CtClass ctClass) {
        ClassFile cf = ctClass.getClassFile2();
        AnnotationsAttribute ainfo = (AnnotationsAttribute)
                cf.getAttribute(AnnotationsAttribute.invisibleTag);  
        AnnotationsAttribute ainfo2 = (AnnotationsAttribute)
                cf.getAttribute(AnnotationsAttribute.visibleTag);  
        return getAnnotationsFromAttributes(ainfo, ainfo2);
    }

    private static List<Annotation> getAnnotationsFromAttributes(AnnotationsAttribute ainfo, AnnotationsAttribute ainfo2) {
        Annotation[] anno1, anno2;
        int size1, size2;

        if (ainfo == null) {
            anno1 = null;
            size1 = 0;
        }
        else {
            anno1 = ainfo.getAnnotations();
            size1 = anno1.length;
        }

        if (ainfo2 == null) {
            anno2 = null;
            size2 = 0;
        }
        else {
            anno2 = ainfo2.getAnnotations();
            size2 = anno2.length;
        }

        
       Annotation[] result = new Annotation[size1 + size2];
       for (int i = 0; i < size1; i++) {
           result[i] = anno1[i];
       }
   
       for (int j = 0; j < size2; j++) {
           result[j + size1] = anno2[j];
       }
    
       return Arrays.asList(result);
    }
    
    private static void getAllInterfaces(CtClass baseClass, Set<CtClass> interfaceList) throws NotFoundException {
        interfaceList.addAll(Arrays.asList(baseClass.getInterfaces()));
        for(CtClass interface_ : baseClass.getInterfaces()) {
            getAllInterfaces(interface_, interfaceList);
        }
    }
}
