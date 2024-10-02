package com.tracelytics.instrumentation.nosql.cassandra;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtConstructor;
import com.tracelytics.ext.javassist.CtField;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.jdbc.SQLSanitizer;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;

/**
 * Instruments Cassandra's <code>BoundStatement</code>, it is usually creates by binding parameters to <code>PreparedStatement</code>
 * 
 * @author pluk
 *
 */
public class CassandraBoundStatementInstrumentation extends CassandraStatementInstrumentation {
    private enum OpType { BIND, SET_BY_INDEX, SET_BY_NAME, SET_VALUE };
    
    //List of methods from BoundStatement, we need those to capture the parameters set against it
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
         new MethodMatcher<OpType>("bind", new String[] { "java.lang.Object[]" }, "com.datastax.driver.core.BoundStatement", OpType.BIND),
         new MethodMatcher<OpType>("setValue", new String[] { "int", "java.nio.ByteBuffer" }, "com.datastax.driver.core.BoundStatement", OpType.SET_VALUE),
         new MethodMatcher<OpType>("setBool", new String[] { "int", "boolean" }, "com.datastax.driver.core.BoundStatement", OpType.SET_BY_INDEX),
         new MethodMatcher<OpType>("setBool", new String[] { "java.lang.String", "boolean" }, "com.datastax.driver.core.BoundStatement", OpType.SET_BY_NAME),
         new MethodMatcher<OpType>("setBytes", new String[] { "int", "java.nio.ByteBuffer" }, "com.datastax.driver.core.BoundStatement", OpType.SET_BY_INDEX),
         new MethodMatcher<OpType>("setBytes", new String[] { "java.lang.String", "java.nio.ByteBuffer" }, "com.datastax.driver.core.BoundStatement", OpType.SET_BY_NAME),
         new MethodMatcher<OpType>("setBytesUnsafe", new String[] { "int", "java.nio.ByteBuffer" }, "com.datastax.driver.core.BoundStatement", OpType.SET_BY_INDEX),
         new MethodMatcher<OpType>("setBytesUnsafe", new String[] { "java.lang.String", "java.nio.ByteBuffer" }, "com.datastax.driver.core.BoundStatement", OpType.SET_BY_NAME),
         new MethodMatcher<OpType>("setDate", new String[] { "int", "java.util.Date" }, "com.datastax.driver.core.BoundStatement", OpType.SET_BY_INDEX),
         new MethodMatcher<OpType>("setDate", new String[] { "java.lang.String", "java.util.Date" }, "com.datastax.driver.core.BoundStatement", OpType.SET_BY_NAME),
         new MethodMatcher<OpType>("setDecimal", new String[] { "int", "java.math.BigDecimal" }, "com.datastax.driver.core.BoundStatement", OpType.SET_BY_INDEX),
         new MethodMatcher<OpType>("setDecimal", new String[] { "java.lang.String", "java.math.BigDecimal" }, "com.datastax.driver.core.BoundStatement", OpType.SET_BY_NAME),
         new MethodMatcher<OpType>("setDouble", new String[] { "int", "double" }, "com.datastax.driver.core.BoundStatement", OpType.SET_BY_INDEX),
         new MethodMatcher<OpType>("setDouble", new String[] { "java.lang.String", "double" }, "com.datastax.driver.core.BoundStatement", OpType.SET_BY_NAME),
         new MethodMatcher<OpType>("setFloat", new String[] { "int", "float" }, "com.datastax.driver.core.BoundStatement", OpType.SET_BY_INDEX),
         new MethodMatcher<OpType>("setFloat", new String[] { "java.lang.String", "float" }, "com.datastax.driver.core.BoundStatement", OpType.SET_BY_NAME),
         new MethodMatcher<OpType>("setInet", new String[] { "int", "java.net.InetAddress" }, "com.datastax.driver.core.BoundStatement", OpType.SET_BY_INDEX),
         new MethodMatcher<OpType>("setInet", new String[] { "java.lang.String", "java.net.InetAddress" }, "com.datastax.driver.core.BoundStatement", OpType.SET_BY_NAME),
         new MethodMatcher<OpType>("setInt", new String[] { "int", "int" }, "com.datastax.driver.core.BoundStatement", OpType.SET_BY_INDEX),
         new MethodMatcher<OpType>("setInt", new String[] { "java.lang.String", "int" }, "com.datastax.driver.core.BoundStatement", OpType.SET_BY_NAME),
         new MethodMatcher<OpType>("setList", new String[] { "int", "java.util.List" }, "com.datastax.driver.core.BoundStatement", OpType.SET_BY_INDEX),
         new MethodMatcher<OpType>("setList", new String[] { "java.lang.String", "java.util.List" }, "com.datastax.driver.core.BoundStatement", OpType.SET_BY_NAME),
         new MethodMatcher<OpType>("setLong", new String[] { "int", "long" }, "com.datastax.driver.core.BoundStatement", OpType.SET_BY_INDEX),
         new MethodMatcher<OpType>("setLong", new String[] { "java.lang.String", "long" }, "com.datastax.driver.core.BoundStatement", OpType.SET_BY_NAME),
         new MethodMatcher<OpType>("setMap", new String[] { "int", "java.util.Map" }, "com.datastax.driver.core.BoundStatement", OpType.SET_BY_INDEX),
         new MethodMatcher<OpType>("setMap", new String[] { "java.lang.String", "java.util.Map" }, "com.datastax.driver.core.BoundStatement", OpType.SET_BY_NAME),
         new MethodMatcher<OpType>("setSet", new String[] { "int", "java.util.Set" }, "com.datastax.driver.core.BoundStatement", OpType.SET_BY_INDEX),
         new MethodMatcher<OpType>("setSet", new String[] { "java.lang.String", "java.util.Set" }, "com.datastax.driver.core.BoundStatement", OpType.SET_BY_NAME),
         new MethodMatcher<OpType>("setString", new String[] { "int", "java.lang.String" }, "com.datastax.driver.core.BoundStatement", OpType.SET_BY_INDEX),
         new MethodMatcher<OpType>("setString", new String[] { "java.lang.String", "java.lang.String" }, "com.datastax.driver.core.BoundStatement", OpType.SET_BY_NAME),
         new MethodMatcher<OpType>("setUUID", new String[] { "int", "java.util.UUID" }, "com.datastax.driver.core.BoundStatement", OpType.SET_BY_INDEX),
         new MethodMatcher<OpType>("setUUID", new String[] { "java.lang.String", "java.util.UUID" }, "com.datastax.driver.core.BoundStatement", OpType.SET_BY_NAME),
         new MethodMatcher<OpType>("setVarint", new String[] { "int", "java.math.BigInteger" }, "com.datastax.driver.core.BoundStatement", OpType.SET_BY_INDEX),
         new MethodMatcher<OpType>("setVarint", new String[] { "java.lang.String", "java.math.BigInteger" }, "com.datastax.driver.core.BoundStatement", OpType.SET_BY_NAME)
    );
    
    @Override
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        super.applyInstrumentation(cc, className, classBytes);
        
        tagAsStatementWithQuery(cc); 
        
        Integer sanitizeFlag = (Integer) ConfigManager.getConfig(ConfigProperty.AGENT_SQL_SANITIZE);
        
        if (sanitizeFlag != null && sanitizeFlag == SQLSanitizer.DISABLED) {  //Only report parameter instrumentation if sanitize is explicitly disabled
            //add fields and methods to trace bound parameters
            tagAsStatementWithParameters(cc);
            addCurrentParameterMethods(cc);
            
            //modify setXXX methods
            modifySetMethods(cc);
        }

        return true;
    }

    /**
     * Since <code>BoundStatement</code> does not have the getQueryString() method, therefore we would need to capture the query from the ctor with the Query String from 
     * the <code>PreparedStatement</code> argument
     * @param cc
     * @throws NotFoundException
     * @throws CannotCompileException
     */
    private void tagAsStatementWithQuery(CtClass cc) throws NotFoundException, CannotCompileException {
        try  {
            cc.getMethod("getTvQueryString", "()Ljava/lang/String;"); //check whether method is already existed
            return;
        } catch (NotFoundException e) {
            //expected
        }
        
        cc.addField(CtField.make("private String tvQueryString;", cc));
        
        CtConstructor constructor = cc.getConstructor("(Lcom/datastax/driver/core/PreparedStatement;)V");
        insertAfter(constructor, "if ($1 != null) { tvQueryString = $1.getQueryString(); }", true);
        
        cc.addMethod(CtNewMethod.make("public String tvGetQueryString() { return tvQueryString; }", cc));
        
        tagInterface(cc, StatementWithQueryString.class.getName());
    }

    /**
     * BoundStatement supports a method call like
     * <code>
     *     boundStatement.setInt(String, int)
     * </code>
     * which the first parameter is the name of the variable. To extract the indexes, we do not want to call <code>metadata().getAllIdx(name)</code>, so instead we 
     * first store the parameter value from the setXXX method, then figure out the index when it internally calls the <mode>setValue(int, ByteBuffer)</mode> method 
     * @param cc
     * @throws CannotCompileException
     * @throws NotFoundException
     */
    private void addCurrentParameterMethods(CtClass cc)
            throws CannotCompileException, NotFoundException {
     // to trace current parameter, so paired with the index it can be later set to the map
        cc.addField(CtField.make("private Object tvCurrentParameter;", cc));
        cc.addField(CtField.make("private boolean tvHasCurrentParameter;", cc));
        cc.addMethod(CtNewMethod.make("public void tvSetCurrentParameter(Object currentParameter) { tvCurrentParameter = currentParameter; tvHasCurrentParameter = true; }", cc));
        cc.addMethod(CtNewMethod.make("public Object tvGetCurrentParameter() { return tvCurrentParameter; }", cc));
        cc.addMethod(CtNewMethod.make("public void tvRemoveCurrentParameter() { tvCurrentParameter = null; tvHasCurrentParameter = false; }", cc));
        cc.addMethod(CtNewMethod.make("public boolean tvHasCurrentParameter() { return tvHasCurrentParameter; }", cc));
    }


   
    private void modifySetMethods(CtClass cc) throws CannotCompileException, NotFoundException {
        for (Entry<CtMethod, OpType> methodEntry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            CtMethod method = methodEntry.getKey();
            OpType type = methodEntry.getValue();
            
            if (!shouldModify(cc, method)) {
               continue;
            }
                
            if (type == OpType.SET_BY_NAME) {
                //Set parameter value to the field tvCurrentParameter so it can be picked up later on by setCurrentParameterToIndex
                insertBefore(method, "tvSetCurrentParameter(" + STATEMENT_INSTRUMENTATION_CLASS_NAME + ".getBsonValue($args[1]));"); //use $args[1] as $2 could be primitive type and cannot be cast to Object
                //clear the current parameter afterwards
                insertAfter(method,  "tvRemoveCurrentParameter();", true);
            } else if (type == OpType.SET_BY_INDEX) {
                insertBefore(method, "tvSetParameter($1, " + STATEMENT_INSTRUMENTATION_CLASS_NAME + ".getBsonValue($args[1]));"); //use $args[1] as $2 could be primitive type and cannot be cast to Object
            } else if (type == OpType.BIND) {
                insertBefore(method, "if ($1 != null) {" +
                		             "    for (int i = 0; i < $1.length; i ++) {" +
                                     "        tvSetParameter(i, " + STATEMENT_INSTRUMENTATION_CLASS_NAME + ".getBsonValue($1[i]));" +
                		             "    }" +
                                     "}");
            } else if (type == OpType.SET_VALUE) {
                insertBefore(method, "if (tvHasCurrentParameter()) { " +
                		             "    tvSetParameter($1, tvGetCurrentParameter()); " +
                		             "}");
            }
        }
    }
}
