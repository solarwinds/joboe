package com.tracelytics.instrumentation.actor.akka;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.ContextPropagationPatcher;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.Module;
import com.tracelytics.instrumentation.config.HideParamsConfig;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;
import com.tracelytics.joboe.Metadata;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;


/**
 * Instruments <code>akka.actor.ActorCell</code> for the "receive" action of the actor. This instruments a bit lower level at the ActorCell instead of the Actor itself access to 
 * the <code>Envelope</code> is needed for context propagation and such info is only available to the <code>ActorCell.invoke</code> method
 * 
 * Take note that this instrumentation takes care of 2 tasks
 * <ol>
 *  <li> Propagate the context tagged in <code>EnvelopeInfo</code> coupled with the <code>Envelope</code> for ALL messages (if envelope created within a valid context)
 *  <li> Trace the operation if the actor (that receives the message) if and only if it matches the pattern specified in <code>AGENT_AKKA_ACTORS</code>
 * </ol>
 * 
 * @author pluk
 *
 */
public class AkkaActorCellInstrumentation extends ClassInstrumentation {
    private static final String LAYER_NAME = "akka-actor";
    //private static EventValueConverter converter = new EventValueConverter();
    private static String CLASS_NAME = AkkaActorCellInstrumentation.class.getName();
    
    private enum OpType { INVOKE }
    
    private static List<Pattern> akkaActorPatterns = new ArrayList<Pattern>();
    
    private static boolean hideMessage = ConfigManager.getConfig(ConfigProperty.AGENT_HIDE_PARAMS) != null ? ((HideParamsConfig) ConfigManager.getConfig(ConfigProperty.AGENT_HIDE_PARAMS)).shouldHideParams(Module.AKKA_ACTOR) : false;
    
    //read the list of akka actor patterns which matching actors' operations should be instrumented
    static {
        if (ConfigManager.getConfig(ConfigProperty.AGENT_AKKA_ACTORS) instanceof String[]) {
            for (String patternAsString : (String[]) ConfigManager.getConfig(ConfigProperty.AGENT_AKKA_ACTORS)) {
                try {
                    akkaActorPatterns.add(Pattern.compile(patternAsString));
                } catch (PatternSyntaxException e) {
                    logger.warn("Akka actor pattern " + patternAsString + " is not in valid syntax. Skipping this pattern");
                }
            }
        }
    }
    
    
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
         new MethodMatcher<OpType>("invoke", new String[] { "akka.dispatch.Envelope"} , "void", OpType.INVOKE)
    );
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        
        //convenient method to get a router path (router parent) if available
        cc.addMethod(CtNewMethod.make("private akka.actor.ActorPath tvGetRouterPath() {"
                                    + "    if (parent() instanceof akka.routing.RoutedActorRef) {"
                                    + "        return parent().path();"
                                    + "    } else {"
                                    + "        return null;"
                                    + "    }"
                                    + "}", cc));
        
    	for (CtMethod method : findMatchingMethods(cc, methodMatchers).keySet()) {
    	    insertBefore(method, "akka.actor.ActorPath actorPath = (actor() != null && actor().self() != null) ? actor().self().path() : null;"
    	                       + "String toActorPath = actorPath != null ? actorPath.toString() : null;"
    	                       + "String toActorName = actorPath != null ? actorPath.name() : null;"
    	                       + "akka.actor.ActorPath routerPath =  tvGetRouterPath();"
    	                       + "String toRouterPath = routerPath != null ? routerPath.toString() : null;"
    	                       + "String toRouterName = routerPath != null ? routerPath.name() : null;"
    	                       +  CLASS_NAME + ".operationEntry(($1 != null && $1.sender() != null && $1.sender().path() != null) ? $1.sender().path().toString() : null, "
    	                                         + "                toActorPath,"
    	                                         + "                toActorName,"
    	                                         + "                toRouterPath, "
    	                                         + "                toRouterName, "
    	                                         + "                $1 != null ? $1.message() : null,"
    	                                         + "                $1);"
    	                         , false);
            insertAfter(method, CLASS_NAME + ".operationExit($1);", true, false);
        }
        return true;
    }
    
    private static boolean isMatchingActor(String actorPath) {
        if (actorPath == null) {
            return false;
        }
        for (Pattern pattern : akkaActorPatterns) {
            if (pattern.matcher(actorPath).matches()) {
                return true;
            }
        }
        return false;
    }
    
    
    public static void operationEntry(String fromActorPath, String toActorPath, String toActorName, String routerPath, String routerName, Object messageObject, Object envelopeObject) {
        AkkaEnvelopeInfo info = AkkaEnvelopeCompanionInstrumentation.getEnvelopeInfo(envelopeObject);
        if (info == null) {
            return;
        }
        
        //always attempt to propagate the context
        boolean contextRestored = ContextPropagationPatcher.restoreContext(info);
        if (!contextRestored) {
            return;
        }
        
        if (Context.getMetadata().isSampled() && messageObject != null && (isMatchingActor(toActorPath) || isMatchingActor(routerPath))) {
            Event event = Context.createEvent();
    
            event.addInfo("Layer", LAYER_NAME,
                          "Label", "entry");
    
            event.addInfo("FunctionName", "receive");
            event.addInfo("Language", "scala");
            event.addInfo("Actor", toActorPath);
            event.addInfo("ActorName", toActorName);
            event.addInfo("RouterName", routerName);
            
            if (fromActorPath != null) {
                event.addInfo("SenderActor", fromActorPath);
            }
            
            if (!hideMessage) {
                //do not use converter for now due to performance concern, in most cases this will likely be a case class, so class name is sufficient
                //profileEvent.addInfo("Message", converter.convertToEventValue(messageObject)); 
                String messageLabel = "(" + messageObject.getClass().getName() + ") id [" + System.identityHashCode(messageObject) + "]";
                event.addInfo("Message", messageLabel);
            }
            
            long creationTime = info.getCreationTimestamp();
            long currentTime = System.nanoTime();
            event.addInfo("MessageWaitTime", (currentTime - creationTime)/1000 + " mircosecond(s)"); //calculate time between envelope creation to message consumption
            event.report();
            
            info.setEntryContext(new Metadata(Context.getMetadata())); //keep track of the entry context in case other instrumentation clears the context before envelope exits
        } 
    }
    
    public static void operationExit(Object envelopeObject) {
        AkkaEnvelopeInfo info = AkkaEnvelopeCompanionInstrumentation.getEnvelopeInfo(envelopeObject);
        if (info == null) {
            if (Context.isValid()) { // always clear context when Akka actor finishes processing an envelope
                Context.clearMetadata();
            }
            return;
        }
        
        if (info.getEntryContext() != null) { //only exit of the actor that handles this envelope has been traced
                //special case that even if the thread's context is cleared by other instrumentation, we should restore the context to create exit event
            Context.setMetadata(info.getEntryContext());
            
            Event event = Context.createEvent();
            event.addInfo("Label", "exit",
                          "Layer", LAYER_NAME);
            
            event.report();
            
            info.setEntryContext(null);            
        }
        ContextPropagationPatcher.resetContext(info, false); //just clear the context as Akka actor should handle one envelope at a time

        AkkaEnvelopeCompanionInstrumentation.unregisterEnvelope(envelopeObject);
    }
}

    
