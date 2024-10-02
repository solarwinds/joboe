package com.tracelytics.instrumentation.nosql.cassandra;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtField;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.ext.javassist.Modifier;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.joboe.EventValueConverter;

/**
 * Tag the Cassandra Statement/Query if it has the getQueryString method
 * @author pluk
 *
 */
public class CassandraStatementInstrumentation extends CassandraBaseInstrumentation {
    private static final String TV_PARAMETERS_FIELD = "tvParameters";
    
    protected static final EventValueConverter valueConverter = new EventValueConverter();
    protected static final String STATEMENT_INSTRUMENTATION_CLASS_NAME = CassandraStatementInstrumentation.class.getName();
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        try {
            CtClass builtStatementClass = classPool.get("com.datastax.driver.core.querybuilder.BuiltStatement");
            
            if (cc.subtypeOf(builtStatementClass)) { //do not use getQueryString as BuiltStatement should show parameters
                cc.addMethod(CtNewMethod.make("public String tvGetQueryString() { return toString(); }", cc));
            } else {
                cc.getMethod("getQueryString", "()Ljava/lang/String;");
                cc.addMethod(CtNewMethod.make("public String tvGetQueryString() { return getQueryString(); }", cc));
            }
            
            tagInterface(cc, StatementWithQueryString.class.getName());
        } catch (NotFoundException e) {
            //OK, does not have the getQueryString method;
        }
        
        return true;
    }

    private static enum OpType {
        GET_QUERY_STRING
    }
    
    protected void tagAsStatementWithParameters(CtClass cc)
        throws CannotCompileException, NotFoundException {
        
        // Check if these fields may have already been added (in a base class):
        try {
            if (cc.getField(TV_PARAMETERS_FIELD) != null) {
                return;
            }
        } catch(NotFoundException ex) {
            // Continue
        }

        CtField f;
        CtMethod m;
        
        // To track parameters, use sortedMap so the converted param array will be ordered by the index (key in this map)
        f = new CtField(classPool.getCtClass("java.util.SortedMap"), TV_PARAMETERS_FIELD, cc);
        f.setModifiers(Modifier.PRIVATE);
        
        cc.addField(f, "new java.util.TreeMap()");
        
        //set parameter method
        m = CtNewMethod.make("public void tvSetParameter(int index, Object parameter) { " + TV_PARAMETERS_FIELD + ".put(Integer.valueOf(index), parameter); }", cc);
        cc.addMethod(m);
        
        //get parameters method
        m = CtNewMethod.make("public java.util.SortedMap tvGetParameters() { return " + TV_PARAMETERS_FIELD + "; }", cc);
            cc.addMethod(m);
            
        tagInterface(cc, StatementWithParameters.class.getName());
    }
    
    public static Object getBsonValue(Object parameter) {
        return valueConverter.convertToEventValue(parameter);
    }
}