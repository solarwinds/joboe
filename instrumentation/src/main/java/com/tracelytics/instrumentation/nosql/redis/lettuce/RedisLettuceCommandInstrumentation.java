package com.tracelytics.instrumentation.nosql.redis.lettuce;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtConstructor;
import com.tracelytics.ext.javassist.CtField;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.ConstructorMatcher;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;
import com.tracelytics.joboe.Metadata;

/**
 * Instruments the Lettuce {@code Command}. Instrumentation on this class serves 2 purpose:
 * <ol>
 *  <li>Propagate the metadata context such that {@link RedisLettuceCommandHandlerInstrumentation } can have the correct context for entry event creation</li>
 *  <li>Ends the extent when the Command is completed (method complete)</li>
 * </ol>
 * 
 * Take note that we do not want to start the extent in the ctor of this class as construction of this class does not imply the Command will be processed/completed
 * 
 * @author Patson Luk
 *
 */
public class RedisLettuceCommandInstrumentation extends ClassInstrumentation {
    private static final String CLASS_NAME = RedisLettuceCommandInstrumentation.class.getName();
    private static final String LAYER_NAME = "redis-lettuce";
    private enum OpType { WRITE }
    
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
        new MethodMatcher<OpType>("complete", new String[]{ }, "void", OpType.WRITE, true)
    );
    
    @SuppressWarnings("unchecked")
    private static List<ConstructorMatcher<Object>> constructorMatchers = Arrays.asList(
         new ConstructorMatcher<Object>(new String[] { "java.lang.Object", "java.lang.Object", "java.lang.Object", "boolean" })                                                                           
    );
    
    
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        Map<CtMethod, OpType> matchingMethods = findMatchingMethods(cc, methodMatchers);
        
        cc.addField(CtField.make("private com.tracelytics.joboe.Metadata tvMetadata;", cc));
        cc.addMethod(CtNewMethod.make("public void tvSetMetadata(com.tracelytics.joboe.Metadata metadata) { tvMetadata = metadata; }", cc));
        cc.addMethod(CtNewMethod.make("public com.tracelytics.joboe.Metadata tvGetMetadata() { return tvMetadata; }", cc));
        
        cc.addMethod(CtNewMethod.make("public String tvGetName() { return type != null ? type.name() : null; }", cc));
        cc.addMethod(CtNewMethod.make("public boolean tvIsHit() { return getOutput() != null ? getOutput().get() != null : false; }", cc));
        cc.addMethod(CtNewMethod.make("public Object tvGetArgs() { return args; }", cc));
        
        cc.addField(CtField.make("private boolean tvIsMulti;", cc));
        cc.addMethod(CtNewMethod.make("public void tvSetMulti(boolean isMulti) { tvIsMulti = isMulti; }", cc));
        cc.addMethod(CtNewMethod.make("public boolean tvIsMulti() { return tvIsMulti; }", cc));
        
        cc.addField(CtField.make("private boolean tvHasSent;", cc));
        cc.addMethod(CtNewMethod.make("public void tvSetHasSent(boolean hasSent) { tvHasSent = hasSent; }", cc));
        cc.addMethod(CtNewMethod.make("public boolean tvHasSent() { return tvHasSent; }", cc));
        
        tagInterface(cc, RedisLettuceCommand.class.getName());
        
        for (CtConstructor constructor : findMatchingConstructors(cc, constructorMatchers).keySet()) {
            insertAfter(constructor, CLASS_NAME + ".storeContextToCommand(this, $4);", true, false);
        }
        
        
        for (Entry<CtMethod, OpType> matchingMethodEntry : matchingMethods.entrySet()) {
            CtMethod method = matchingMethodEntry.getKey();
            insertBefore(method, CLASS_NAME + ".layerExit(this);", false);
        }
        
        return true;
    }
    
    public static void storeContextToCommand(Object commandObject, boolean isMulti) {
        if (RedisLettuceContext.getActiveDispatch() != null) {
            //store a clone of the context so it appears as fork
            ((RedisLettuceCommand)commandObject).tvSetMetadata(new Metadata(RedisLettuceContext.getActiveDispatch()));
            ((RedisLettuceCommand)commandObject).tvSetMulti(isMulti);
        }
    }
    
    public static void layerExit(Object commandObject) {
        RedisLettuceCommand command = (RedisLettuceCommand) commandObject;
        synchronized(command) { //asynchronous write and read (also cancel trigger exit as well) has to synchronize to avoid inconsistent state
            if (command.tvGetMetadata() != null && command.tvGetMetadata().isSampled() && command.tvHasSent()) { //only end if it has already been sent(entry). The command might exit if it's cancelled before getting sent out
                try {
                    //store the current context so we can restore it after
                    Metadata currentContext = Context.getMetadata();
                    
                    Context.setMetadata(command.tvGetMetadata()); //set the current context to the one tagged to this object, such that this event can point to a correct previous context even in different thread
                    Event event = Context.createEvent();
                    event.addInfo("Label", "exit",
                                  "Layer", LAYER_NAME);
    
                    if (!command.tvIsMulti() && "GET".equals(command.tvGetName())) { //Only set KVHit on GET operations in non multi mode (multi mode always return QUEUED)
                        event.addInfo("KVHit", command.tvIsHit());
                    }
                    
                    event.report();
                    
                    //restore the previous context
                    if (currentContext.isValid()) {
                        Context.setMetadata(currentContext);
                    } else {
                        Context.clearMetadata();
                    }
                    
                } finally {
                    command.tvSetMetadata(null); //remove the context tagged to this object
                }
            }
        }
    }
    
}