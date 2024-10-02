package com.tracelytics.instrumentation.nosql;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.Modifier;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.ClassInstrumentation;

/**
 * Tags hbase operations that provide Column Family and/or Row information
 * 
 * @author Patson Luk
 *
 */
public class HbaseOperationInstrumentation extends ClassInstrumentation {
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        if ((cc.getModifiers() & Modifier.ABSTRACT) != Modifier.ABSTRACT) { //we only care about concrete class as we are not modifying any methods but adding methods here
            if (!cc.subtypeOf(classPool.get(HbaseOperationWithFamilyInfo.class.getName()))) { //to avoid double tagging
                try {
                    CtMethod getFamilyMapMethod = cc.getMethod("getFamilyCellMap", "()Ljava/util/NavigableMap;"); //try the method for hbase 1.0
                    
                    if (!getFamilyMapMethod.getDeclaringClass().isInterface()) { //then it is safe to invoke it here  
                        cc.addMethod(CtMethod.make("public java.util.Map getTvFamilies() { return getFamilyCellMap(); }", cc));
                        tagInterface(cc, HbaseOperationWithFamilyInfo.class.getName());
                    }
                } catch (NotFoundException e) { //ok try older version 0.x signature
                    try {
                        CtMethod getFamilyMapMethod = cc.getMethod("getFamilyMap", "()Ljava/util/Map;");
                        
                        if (!getFamilyMapMethod.getDeclaringClass().isInterface()) { //then it is safe to invoke it here  
                            cc.addMethod(CtMethod.make("public java.util.Map getTvFamilies() { return getFamilyMap(); }", cc));
                            tagInterface(cc, HbaseOperationWithFamilyInfo.class.getName());
                        }
                    } catch (NotFoundException e1) {
                        logger.debug("Operation [" + className + "] does not have getFamilyCellMap nor getFamilyMap method, Column Family value will not be reported for this class.");
                    }
                }
            }
            
            if (!cc.subtypeOf(classPool.get(HbaseOperationWithRow.class.getName()))) { //to avoid double tagging
                try {
                    CtMethod getRowMethod = cc.getMethod("getRow", "()[B");
                    
                    if (!getRowMethod.getDeclaringClass().isInterface()) { //then it is safe to invoke it here  
                        cc.addMethod(CtMethod.make("public byte[] getTvRowKey() { return getRow(); }", cc));
                        tagInterface(cc, HbaseOperationWithRow.class.getName());
                    }
                } catch (NotFoundException e) {
                    //OK...not all operations have getRow
                    logger.debug("Operation [" + className + "] does not have getRow method. Row value will not be reported for this class.");
                }
            }
        }
        return true;
    }
}