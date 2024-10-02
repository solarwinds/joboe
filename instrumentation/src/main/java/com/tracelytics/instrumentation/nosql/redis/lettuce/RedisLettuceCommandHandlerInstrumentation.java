package com.tracelytics.instrumentation.nosql.redis.lettuce;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;
import com.tracelytics.joboe.EventValueConverter;
import com.tracelytics.joboe.Metadata;

/**
 * Instruments the Lettuce {@code CommandHandler} in order to trace the start of the Redis operation. This is suitable entry point as it accurately indicates
 * the "write" to the output stream (high level operation is captured by Redisson). However, the exit is captured in {@link RedisLettuceCommandInstrumentation} instead,
 * as this write operation is non-blocking, the response is handled elsewhere.
 *   
 *  
 * @author Patson Luk
 *
 */
public class RedisLettuceCommandHandlerInstrumentation extends ClassInstrumentation {
    private static final String CLASS_NAME = RedisLettuceCommandHandlerInstrumentation.class.getName();
    private static final String LAYER_NAME = "redis-lettuce";
    private enum OpType { WRITE }
    
    private static final EventValueConverter eventValueConverter = new EventValueConverter(100); //limit to 100 for key and script value
    static final ThreadLocal<String> transactionCommandContext = new ThreadLocal<String>();
            
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
        new MethodMatcher<OpType>("write", new String[]{ "io.netty.channel.ChannelHandlerContext", "java.lang.Object", "io.netty.channel.ChannelPromise" }, "void", OpType.WRITE, true)
    );
    
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        Map<CtMethod, OpType> matchingMethods = findMatchingMethods(cc, methodMatchers);
        
        for (Entry<CtMethod, OpType> matchingMethodEntry : matchingMethods.entrySet()) {
            CtMethod method = matchingMethodEntry.getKey();
            
            insertBefore(method, CLASS_NAME + ".layerEntry($2, $1 != null && $1.channel() != null ? $1.channel().remoteAddress() : null);", false);
        }
        
        return true;
    }
    
    public static void layerEntry(Object commandObject, Object remoteAddress) {
        if (commandObject instanceof RedisLettuceCommand) {
            RedisLettuceCommand command = (RedisLettuceCommand) commandObject;
            synchronized(command) { //asynchronous write and read (also cancel trigger exit as well) has to synchronize to avoid inconsistent state
                if (command.tvGetMetadata() != null && command.tvGetMetadata().isSampled()) {
                    //store the current context so we can restore it after
                    Metadata currentContext = Context.getMetadata();
                    
                    Context.setMetadata(command.tvGetMetadata());
                    
                    Event event = Context.createEvent();
                    event.addInfo("Label", "entry",
                                  "Layer", LAYER_NAME);
                                  
                    if (command.tvGetName() != null) {
                        event.addInfo("KVOp", command.tvGetName().toLowerCase());
                    }
                    
                    if (remoteAddress != null) {
                        event.addInfo("RemoteHost", remoteAddress.toString());
                    }
                    
                    if (command.tvGetArgs() instanceof RedisLettuceCommandArgs) {
                        RedisLettuceCommandArgs args = (RedisLettuceCommandArgs) command.tvGetArgs();
                        String commandName = command.tvGetName();
                        if (!"AUTH".equals(commandName)) { //do not report AUTH password
                            if ("EVAL".equals(commandName) || "EVALSHA".equals(commandName)) { //report script if it's eval or evalsha
                                if (args.tvGetScript() != null) { 
                                    event.addInfo("Script", eventValueConverter.convertToEventValue(args.tvGetScript()));
                                }
                            } else if (args.tvGetKeyCount() == 1 && args.tvGetKey() != null) { //only report if there is one param key according to the KV spec 
                                event.addInfo("KVKey", eventValueConverter.convertToEventValue(args.tvGetKey()));
                            }
                        }
                    }
                
                    
                    event.setAsync();
                    
                    event.report();
                    
                    command.tvSetHasSent(true); //indicates that the command has been sent
                    
                    //restore the previous context
                    if (currentContext.isValid()) {
                        Context.setMetadata(currentContext);
                    } else {
                        Context.clearMetadata();
                    }
                } else {
                    logger.debug("Redisson Command written without context, probably not going contructed through RedisAsyncConnection, command [" + command.tvGetName() + "]"); 
                }
            }
        }
    }
    
}
