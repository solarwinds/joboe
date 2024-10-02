package com.tracelytics.instrumentation.http.play;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtConstructor;
import com.tracelytics.ext.javassist.CtField;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ConstructorMatcher;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.joboe.Context;

/**
 * Instrumentation for Play mvc 2.0 and 2.1 action and profile. For Play mvc 2.2+, please refer to {@link PlayScalaActionProfileInstrumentation}
 * 
 * Since in play 2.0 and 2.1, there is no invokeBlock method in play.api.mvc.ActionBuilder, we would need to instead look for the 
 * apply(Request) : Result methods. It is ok to capture these method directly as they are synchronous (returns Result). For 2.2+,
 * we have to attach callbacks for exit events as the methods return Future[Result]
 * 
 * Take note that for layerEntry and layerExit handling, this still uses the same static methods provided by <code>PlayActionProfileInstrumentation</code>
 *  
 * @author pluk
 *
 */
public class Play2_0And2_1ActionProfileInstrumentation extends PlayBaseInstrumentation {

    private static String PLAY_PROFILE_INSTRUMENTATION_CLASS_NAME = PlayScalaActionProfileInstrumentation.class.getName();
    
    private enum OpType {
        APPLY, CTOR_2_0, CTOR_2_1
    }
    
 // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
            new MethodMatcher<OpType>("apply", new String[] { "play.api.mvc.Request" }, "play.api.mvc.Result", OpType.APPLY) 
    );
    @SuppressWarnings("unchecked")
    private static List<ConstructorMatcher<OpType>> constructorMatchers = Arrays.asList(
            new ConstructorMatcher<OpType>(new String[] { "java.lang.Object", "scala.Function1"}, OpType.CTOR_2_0, true) //2.0
          , new ConstructorMatcher<OpType>(new String[] { "java.lang.Object", "java.lang.Object", "scala.Function1"}, OpType.CTOR_2_1, true)); //2.1
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        //make sure this is not 2.2+, as we capture the profile in PlayActionProfileInstrumentation instead
        Version version = identifyVersion();
        if (version.isNewerOrEqual(Version.PLAY_2_2)) {
            return false;
        }
        
        cc.addField(CtField.make("private Object tvFunction;", cc)); //keep track of the function block used, so we can construct the profile name KV
        for (Entry<CtConstructor, OpType> constructorEntry : findMatchingConstructors(cc, constructorMatchers).entrySet()) {
            if (constructorEntry.getValue() == OpType.CTOR_2_0) {
                insertAfter(constructorEntry.getKey(), "tvFunction = $2;", true, false);
            } else {
                insertAfter(constructorEntry.getKey(), "tvFunction = $3;", true, false);
            }
        }
        
        for (CtMethod method : findMatchingMethods(cc, methodMatchers).keySet()) {
            insertBefore(method, 
                         "scala.Option spanKeyOption = $1.headers().get(\"" + X_SPAN_KEY + "\");" +  //spanKey to look up span for reporting action/controller used by metrics
                         "String spanKey = spanKeyOption.isEmpty() ? null : (String)spanKeyOption.get();" +
                          PLAY_PROFILE_INSTRUMENTATION_CLASS_NAME + ".layerEntry(spanKey, tvFunction);"
                         , false);
                
            addErrorReporting(method, Throwable.class.getName(), LAYER_NAME, classPool);
            
            insertAfter(method, PLAY_PROFILE_INSTRUMENTATION_CLASS_NAME + ".layerExit(" + Context.class.getName() + ".getMetadata(), null);", true);
        }

        return true;
    }
}