package com.tracelytics.instrumentation.cache.xmemcached;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodSignature;
import com.tracelytics.instrumentation.Module;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;


/**
 * Instruments XMemcached client: https://github.com/killme2008/xmemcached
 *
 * XXX: Try to make this generic once we have another memcache client instrumentation, though probably not be possible
 * given there is no standard Java interface for Memcache clients, unlike JDBC.
 */
public class XMemcachedInstrumentation extends ClassInstrumentation {

    // Instrumented methods are categorized using the following constants
    private static final int
            INST_OP = 0,           // Generic op method: just logs the op (method)
            INST_OP_KEY = 1,       // Op with key: first param is key, returns value is ignored
            INST_GET = 2,          // Get-ish: first param is key, returns object (or null)
            INST_SET = 3;          // Set-ish: first param is key, second is TTL, returns boolean

    
    // Signatures for the various "op" methods we are instrumenting.
    // We only instrument the lowest level methods to avoid multiple start/end events for the same call.
    static MethodSignature[] opMethods = {
        //add
        new MethodSignature("add", "(Ljava/lang/String;ILjava/lang/Object;Lnet/rubyeye/xmemcached/transcoders/Transcoder;J)Z", INST_OP_KEY),
        new MethodSignature("addWithNoReply", "(Ljava/lang/String;ILjava/lang/Object;Lnet/rubyeye/xmemcached/transcoders/Transcoder;)V", INST_OP_KEY),

        // append
        new MethodSignature("append", "(Ljava/lang/String;Ljava/lang/Object;J)Z", INST_OP_KEY),
        new MethodSignature("appendWithNoReply", "(Ljava/lang/String;Ljava/lang/Object;)V", INST_OP_KEY),

        // cas
        new MethodSignature("cas", "(Ljava/lang/String;ILnet/rubyeye/xmemcached/CASOperation;Lnet/rubyeye/xmemcached/transcoders/Transcoder;)Z", INST_OP_KEY),
        new MethodSignature("cas", "(Ljava/lang/String;ILnet/rubyeye/xmemcached/GetsResponse;Lnet/rubyeye/xmemcached/CASOperation;Lnet/rubyeye/xmemcached/transcoders/Transcoder;)Z", INST_OP_KEY),
        new MethodSignature("cas", "(Ljava/lang/String;ILjava/lang/Object;Lnet/rubyeye/xmemcached/transcoders/Transcoder;JJ)Z", INST_OP_KEY),
        new MethodSignature("casWithNoReply", "(Ljava/lang/String;ILnet/rubyeye/xmemcached/CASOperation;)V", INST_OP_KEY),
        new MethodSignature("casWithNoReply", "(Ljava/lang/String;ILnet/rubyeye/xmemcached/GetsResponse;Lnet/rubyeye/xmemcached/CASOperation;)V", INST_OP_KEY),

        // decr:
        new MethodSignature("decr", "(Ljava/lang/String;JJJ)J", INST_OP_KEY),
        new MethodSignature("decr", "(Ljava/lang/String;JJJI)J", INST_OP_KEY),
        new MethodSignature("decr", "(Ljava/lang/String;JJ)J", INST_OP_KEY),
        new MethodSignature("decr", "(Ljava/lang/String;J)J", INST_OP_KEY),
        new MethodSignature("decrWithNoReply", "(Ljava/lang/String;J)V", INST_OP_KEY),

        // delete:
        new MethodSignature("delete", "(Ljava/lang/String;J)Z", INST_OP_KEY),
        new MethodSignature("delete", "(Ljava/lang/String;I)Z", INST_OP_KEY),
        new MethodSignature("deleteWithNoReply", "(Ljava/lang/String;I)V", INST_OP_KEY),

        // get
        new MethodSignature("get", "(Ljava/lang/String;JLnet/rubyeye/xmemcached/transcoders/Transcoder;)Ljava/lang/Object;", INST_GET),
        new MethodSignature("gets", "(Ljava/lang/String;JLnet/rubyeye/xmemcached/transcoders/Transcoder;)Lnet/rubyeye/xmemcached/GetsResponse;", INST_GET),

        // get multi : we don't log actual keys here, just the op
        new MethodSignature("get", "(Ljava/util/Collection;Lnet/rubyeye/xmemcached/transcoders/Transcoder;)Ljava/util/Map;", INST_OP),
        new MethodSignature("get", "(Ljava/util/Collection;JLnet/rubyeye/xmemcached/transcoders/Transcoder;)Ljava/util/Map;", INST_OP),
        new MethodSignature("gets", "(Ljava/util/Collection;JLnet/rubyeye/xmemcached/transcoders/Transcoder;)Ljava/util/Map;", INST_OP),

        // getAndTouch
        new MethodSignature("getAndTouch", "(Ljava/lang/String;IJ)Ljava/lang/Object;" ,INST_OP_KEY),

        // incr
        new MethodSignature("incr", "(Ljava/lang/String;JJJ)J", INST_OP_KEY),
        new MethodSignature("incr", "(Ljava/lang/String;JJJI)J", INST_OP_KEY),
        new MethodSignature("incr", "(Ljava/lang/String;JJ)J", INST_OP_KEY),
        new MethodSignature("incr", "(Ljava/lang/String;J)J", INST_OP_KEY),
        new MethodSignature("incrWithNoReply", "(Ljava/lang/String;J)V", INST_OP_KEY),

        // prepend
        new MethodSignature("prepend", "(Ljava/lang/String;Ljava/lang/Object;J)Z", INST_OP_KEY),
        new MethodSignature("prependWithNoReply", "(Ljava/lang/String;Ljava/lang/Object;)V", INST_OP_KEY),

        // replace
        new MethodSignature("replace", "(Ljava/lang/String;ILjava/lang/Object;Lnet/rubyeye/xmemcached/transcoders/Transcoder;J)Z", INST_OP_KEY),
        new MethodSignature("replaceWithNoReply", "(Ljava/lang/String;ILjava/lang/Object;Lnet/rubyeye/xmemcached/transcoders/Transcoder;)V", INST_OP_KEY),

        // set
        new MethodSignature("set", "(Ljava/lang/String;ILjava/lang/Object;Lnet/rubyeye/xmemcached/transcoders/Transcoder;J)Z", INST_SET),

        // set_multi ? XXX does not seem to be present in API
          
        // touch
        new MethodSignature("touch", "(Ljava/lang/String;IJ)Z", INST_OP_KEY)
    };

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {

        /// XXX need to gather memcache host name / IP
        applyOpInstrumentation(cc, opMethods);

        return true;
    }

    private void applyOpInstrumentation(CtClass cc, MethodSignature[] methods) {
         for (MethodSignature s: methods) {
            CtMethod m = null;
            
            try {
                m = cc.getMethod(s.getName(), s.getSignature());
                if (m.getDeclaringClass() != cc) {
                    continue;
                }

                switch(s.getInstType()) {
                    case INST_OP:
                        modifyOpMethod(s.getName(), m);
                        break;

                    case INST_OP_KEY : 
                        modifyOpKeyMethod(s.getName(), m);
                        break;

                    case INST_GET:
                        modifyGetMethod(s.getName(), m);
                        break;

                    case INST_SET:
                        modifySetMethod(s.getName(), m);
                        break;
                }

            } catch(NotFoundException ex) {
                logger.debug("Unable to find method with signature: " + s, ex);
            } catch(CannotCompileException ex) {
                logger.debug(ex.getMessage(), ex);
            }
        }
    }

    private void modifyOpMethod(String opName, CtMethod m)
            throws CannotCompileException {
        insertBefore(m, CLASS_NAME + ".layerOpEntry(\"" + opName + "\");");
        insertAfter(m, CLASS_NAME + ".layerOpExit(\"" + opName + "\");", true);
    }

    private void modifyOpKeyMethod(String opName, CtMethod m)
            throws CannotCompileException {
        insertBefore(m, CLASS_NAME + ".layerOpKeyEntry(\"" + opName + "\", $1);");
        insertAfter(m, CLASS_NAME + ".layerOpKeyExit(\"" + opName + "\");", true);
    }

    private void modifyGetMethod(String opName, CtMethod m)
            throws CannotCompileException {

        insertBefore(m, CLASS_NAME + ".layerOpKeyEntry(\"" + opName + "\", $1);");
        insertAfter(m, CLASS_NAME + ".layerGetExit(\"" + opName + "\", $_);", true);
    }

    private void modifySetMethod(String opName, CtMethod m)
            throws CannotCompileException {

        insertBefore(m, CLASS_NAME + ".layerSetEntry(\"" + opName + "\", $1, $2);");
        insertAfter(m, CLASS_NAME + ".layerSetExit(\"" + opName + "\", $_);", true);
    }


    // These methods are called from within the instrumented code:

    public static void layerOpEntry(String op) {
        Event event = Context.createEvent();
        event.addInfo("Layer", LAYER_NAME,
                      "Label", "entry",
                      "KVOp", op);

        ClassInstrumentation.addBackTrace(event, 2, Module.X_MEMCACHED);
        event.report();
    }

    public static void layerOpExit(String op) {
        Event event = Context.createEvent();
        event.addInfo("Layer", LAYER_NAME,
                      "Label", "exit");

        event.report();
    }

    public static void layerOpKeyEntry(String op, String key) {
        Event event = Context.createEvent();
        event.addInfo("Layer", LAYER_NAME,
                      "Label", "entry",
                      "KVOp", op,
                      "KVKey", key);

        ClassInstrumentation.addBackTrace(event, 2, Module.X_MEMCACHED);
        event.report();
    }

    public static void layerOpKeyExit(String op) {
        Event event = Context.createEvent();
        event.addInfo("Layer", LAYER_NAME,
                      "Label", "exit");

        event.report();
    }

    public static void layerGetExit(String op, Object ret) {
        Event event = Context.createEvent();
        event.addInfo("Layer", LAYER_NAME,
                      "Label", "exit",
                      "KVHit", ret != null);

        event.report();
    }


    public static void layerSetEntry(String op, String key, int ttl) {
        Event event = Context.createEvent();
        event.addInfo("Layer", LAYER_NAME,
                      "Label", "entry",
                      "KVOp", op,
                      "KVKey", key,
                      "TTL", ttl);

        ClassInstrumentation.addBackTrace(event, 2, Module.X_MEMCACHED);
        event.report();
    }

    public static void layerSetExit(String op, boolean result) {
        Event event = Context.createEvent();
        event.addInfo("Layer", LAYER_NAME,
                      "Label", "exit",
                      "Result", result);

        event.report();
    }

    private static final String CLASS_NAME = "com.tracelytics.instrumentation.cache.xmemcached.XMemcachedInstrumentation";
    private static final String LAYER_NAME = "xmemcached";
}