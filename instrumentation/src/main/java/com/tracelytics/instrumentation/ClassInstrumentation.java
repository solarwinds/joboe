package com.tracelytics.instrumentation;

import com.tracelytics.ext.javassist.*;
import com.tracelytics.ext.javassist.expr.MethodCall;
import com.tracelytics.joboe.*;
import com.tracelytics.joboe.TraceDecisionUtil.RequestType;
import com.tracelytics.joboe.config.*;
import com.tracelytics.joboe.span.impl.*;
import com.tracelytics.joboe.span.impl.Span.SpanProperty;
import com.tracelytics.joboe.span.impl.Tracer.SpanBuilder;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;
import com.tracelytics.util.BackTraceUtil;

import java.util.*;

/**
 * Instrumentation on a class matching certain criteria
 * 
 * This also contains convenient methods for bytecode modifications
 * 
 * @author pluk
 *
 */
public abstract class ClassInstrumentation {
    protected static final Logger logger = LoggerFactory.getLogger();
  
    static final List<Module> allExtendedBackTraceModules = Arrays.asList(Module.X_MEMCACHED, Module.PLAY, Module.SLING, Module.RABBIT_MQ, Module.KAFKA, Module.JMS);
    private static Set<Module> backTraceModules;
  //Map from known Http headers to XTraceHeader ENUM type
    public static final Map<String, XTraceHeader> XTRACE_HTTP_HEADER_KEYS = new HashMap<String, XTraceHeader>();
    private static final boolean PROFILER_ENBALED = ConfigManager.getConfig(ConfigProperty.PROFILER) != null && ((ProfilerSetting) ConfigManager.getConfig(ConfigProperty.PROFILER)).isEnabled();
    
    public static final String X_SPAN_KEY = "x-tv-span";
    protected static final String X_TRACE_OPTIONS_KEY = "X-Trace-Options";
    public static final String XTRACE_HEADER = HeaderConstants.W3C_TRACE_CONTEXT_HEADER;
    public static final String X_TRACE_OPTIONS_RESPONSE_KEY = "X-Trace-Options-Response";
    private static final String X_TRACE_OPTIONS_SIGNATURE_KEY = "X-Trace-Options-Signature";
    protected static final Tracer tracer = Tracer.INSTANCE;
    protected static final ScopeManager scopeManager = ScopeManager.INSTANCE;
    private static final String[] forwardedTags = {
            "Forwarded-For",
            "Forwarded-Host",
            "Forwarded-Proto",
            "Forwarded-Port",
    };

    static {
        init();
        XTRACE_HTTP_HEADER_KEYS.put(XTRACE_HEADER, XTraceHeader.TRACE_ID);
        XTRACE_HTTP_HEADER_KEYS.put(X_SPAN_KEY, XTraceHeader.SPAN_ID);
        XTRACE_HTTP_HEADER_KEYS.put(X_TRACE_OPTIONS_KEY, XTraceHeader.TRACE_OPTIONS);
        XTRACE_HTTP_HEADER_KEYS.put(X_TRACE_OPTIONS_SIGNATURE_KEY, XTraceHeader.TRACE_OPTIONS_SIGNATURE);
    }
    
    private static void init() {
        initBackTraceModules(ConfigManager.getConfigs(ConfigGroup.AGENT));
    }
    
    public ClassInstrumentation() {
        
    }

    static Set<Module> getBackTraceModulesFromConfigs(ConfigContainer configs) {
        Set<Module> backTraceModules = new HashSet<Module>();

        //handle legacy config properties {@link ConfigProperty#AGENT_EXTENDED_BACK_TRACES} and {@link ConfigProperty#AGENT_EXTENDED_BACK_TRACES_BY_MODULE} might affect the values in
        Boolean extendedBackTraceObject = (Boolean) configs.get(ConfigProperty.AGENT_EXTENDED_BACK_TRACES);
        if (extendedBackTraceObject != null && extendedBackTraceObject) { //only set it to true if the flag present and is set to true
            backTraceModules.addAll(allExtendedBackTraceModules);
        } else {
            @SuppressWarnings("unchecked")
            List<Module> extendedBackTraceModules = (List<Module>) configs.get(ConfigProperty.AGENT_EXTENDED_BACK_TRACES_BY_MODULE);
            if (extendedBackTraceModules != null) {
                backTraceModules.addAll(extendedBackTraceModules);
            }
        }

        //now add the back trace modules by config
        @SuppressWarnings("unchecked")
        List<Module> backTraceModulesByConfig = (List<Module>) configs.get(ConfigProperty.AGENT_BACKTRACE_MODULES);
        if (backTraceModulesByConfig != null) {
            backTraceModules.addAll(backTraceModulesByConfig);
        } else { //not defined, by default we add all modules - back trace extended modules
            List<Module> defaultModules = new ArrayList<Module>(Arrays.asList(Module.values())); //create a new mutable list
            defaultModules.removeAll(allExtendedBackTraceModules);
            backTraceModules.addAll(defaultModules);
        }

        if (backTraceModules.isEmpty()) {
            logger.info("Not reporting any back traces based on configuration");
        }
        return backTraceModules;
    }

    static void initBackTraceModules(ConfigContainer configs) {
        ClassInstrumentation.backTraceModules = getBackTraceModulesFromConfigs(configs);
    }

    protected ClassPool classPool = null;
    protected String layerName = null;
    
    /**
     * Applies instrumentation an existing class and returns whether any modification has been made to the cc class passed in
     * 
     * @param cc    the javassist representation of the loaded class, bytecode modification should be applied to this parameter
     * @param classPool
     * @param className
     * @param classBytes
     * 
     * @return  whether the cc class passed in has been modified
     * @throws  Exception 
     */
    
    public boolean apply(CtClass cc, ClassPool classPool, String className, byte[] classBytes) throws Exception {

        this.classPool = classPool;
        this.layerName = getLayerName(cc);
        synchronized (cc) {
            if (!cc.isFrozen()) { //if a class is frozen, that means someone else has called cc.toBytecode or cc.toClass(), do not attempt to modify the byte code anymore
                return applyInstrumentation(cc, className, classBytes);
            } else {
                logger.info("CtClass of [" + cc.getName() + "] is frozen. It probably has been modified already, skipping bytecode modification on this instance");
                return true; //should still return the byte code, as this class is modified elsewhere, we should not return null as it might override the modification in rare race condition
            }
        }
    }
    
    /**
     * Applies instrumentation an existing class and returns new byte code. This should be implemented by subclasses.
     * @param cc
     * @param className
     * @param classBytes
     * @return true if class was modified, otherwise false
     */
    protected abstract boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception;

    public static void addErrorReporting(CtBehavior method, String exceptionClassName, String layerName, ClassPool classPool) throws NotFoundException, CannotCompileException {
        addErrorReporting(method, exceptionClassName, layerName, classPool, false);
    }


    /**
     * Applies error reporting to a method 
     */
    public static void addErrorReporting(CtBehavior method, String exceptionClassName, String layerName, ClassPool classPool, boolean flagErrorOnTrace) throws NotFoundException, CannotCompileException {
        CtClass exClass = classPool.get(exceptionClassName);
        if (layerName != null) {
            method.addCatch("{ com.tracelytics.instrumentation.ClassInstrumentation.reportError(\"" + layerName + "\", $e, " + flagErrorOnTrace + "); throw $e; }", exClass);
        } else {
            method.addCatch("{ com.tracelytics.instrumentation.ClassInstrumentation.reportError(null, $e, " + flagErrorOnTrace + "); throw $e; }", exClass);
        }
    }

    public static void reportError(String layerName, Throwable t) {
        reportError(layerName, t, false);
    }
    /*
    Callback for error reporting: layerName is optional (can be null)
     */
    public static void reportError(String layerName, Throwable t, boolean flagErrorOnTrace) {
        if (Context.getMetadata().isSampled()) {
            Event event = Context.createEvent();
            
            String message = t.getMessage() != null  ? t.getMessage() : "";
            
            event.addInfo("Label", "error",
                          "Spec", "error",
                          "ErrorClass", t.getClass().getName(),
                          "ErrorMsg" , message);
    
            if (layerName != null) {
                event.addInfo("Layer", layerName);
            }
            
            addBackTrace(event, t.getStackTrace());
    
            event.report();
        }

        if (Context.getMetadata().isValid() && flagErrorOnTrace) {
            Span activeSpan = ScopeManager.INSTANCE.activeSpan();
            if (activeSpan != null) {
                activeSpan.setTracePropertyValue(Span.TraceProperty.HAS_ERROR, true);
            }
        }
    }
    
    public static void reportError(Span span, Throwable t) {
        reportError(span, t, null);
    }
    
    public static void reportError(Span span, Throwable t, String message) {
        if (message == null && t != null) {
            message = t.getMessage() != null  ? t.getMessage() : "";
        }
        
        Map<String, Object> fields = new HashMap<String, Object>();
        
        if (t != null) {
            fields.put("ErrorClass", t.getClass().getName());
            fields.put("Backtrace", BackTraceUtil.backTraceToString(t.getStackTrace()));
        }
         
        if (message != null) {
            fields.put("ErrorMsg", message);
        }
        
        fields.put("Spec", "error");
        
        span.error(fields);
        
    }
    
    public static void reportError(Span span, String errorType, String message) {
        Map<String, Object> fields = new HashMap<String, Object>();
        
        if (errorType != null) {
            fields.put("ErrorClass", errorType);
        }
         
        if (message != null) {
            fields.put("ErrorMsg", message);
        }
        
        fields.put("Spec", "error");
        
        span.error(fields);
    }
    
    
    /**
     * Use {@link #addBackTrace(Event, int, Module)} instead
     * 
     * Since no core code is calling this anymore, any invocation should be from SDK 
     * @param event
     * @param skipElements
     * @deprecated
     */
    public static void addBackTrace(Event event, int skipElements) {
        addBackTrace(event, skipElements, Module.SDK);
    }
    /**
     * Adds Java stack trace to event, excluding skipElements stack trace elements from the beginning. Only add if the it is found in <code>backTraceModules</code>
     *  
     * @param event
     * @param skipElements
     * @param module  Module to be check whether back trace should be added. Only add if the it is found in <code>backTraceModules</code> or null (SDK)  
     */
    public static void addBackTrace(Event event, int skipElements, Module module) {
        if (module != null && !backTraceModules.contains(module)) {
            logger.debug("Skipping back trace as module " + module.toString() + " is not included in modules configured for back trace reporting");
            return;
        }
        
        
        addBackTrace(event, BackTraceUtil.getBackTrace(skipElements + 1));
    }
    
    /**
     * Adds Java stack trace to event, excluding skipElements stack trace elements from the beginning. Back trace will not be added if <code>enabledByDefault</code> is false and  
     * {@link ConfigProperty#AGENT_EXTENDED_BACK_TRACES} is not enabled
     *  
     * @param span
     * @param skipElements
     * @param module  Module to be check whether back trace should be added. Only add if the it is found in <code>backTraceModules</code> or null(backward SDK compatibility) 
     */
    public static void addBackTrace(Span span, int skipElements, Module module) {
        if (module != null && !backTraceModules.contains(module)) {
            logger.debug("Skipping back trace as module " + module.toString() + " is not included in modules configured for back trace reporting");
            return;
        }
        
        addBackTrace(span, BackTraceUtil.getBackTrace(skipElements + 1));
    }
    
    /**
     * Adds Java stack trace to event with the stackTrace provided in the parameter
     * @param event
     * @param stackTrace
     */
    public static void addBackTrace(Event event, StackTraceElement[] stackTrace) {
        event.addInfo("Backtrace", BackTraceUtil.backTraceToString(stackTrace));
    }
    
    /**
     * Adds Java stack trace to span with the stackTrace provided in the parameter
     * @param span
     * @param stackTrace
     */
    public static void addBackTrace(Span span, StackTraceElement[] stackTrace) {
        span.setTag("Backtrace", BackTraceUtil.backTraceToString(stackTrace));
    }
    
    /**
     * Adds Java stack trace to span with the stackTrace provided in the parameter
     * 
     * @deprecated use {@link ClassInstrumentation#addBackTrace(Span, StackTraceElement[])} instead
     * @param span
     * @param stackTrace
     */
    public static void addBackTrace(BaseSpan span, StackTraceElement[] stackTrace) {
        addBackTrace(((ActiveSpan) span).getWrapped().span(), stackTrace);
    }

    @Deprecated
    /**
     * Use {@link BackTraceUtil#getBackTrace(int)} instead
     *
     * Keeping for backwards compatibility for SDK
     */
    public static StackTraceElement[] getBackTrace(int skipElements) {
        return BackTraceUtil.getBackTrace(skipElements);
    }
    
    /**
     * Subclasses may override this to return an instrumentation-specific layer name (example: jdbc)
     * @return
     */
    protected String getLayerName(CtClass cc) {
        return (String) ConfigManager.getConfig(ConfigProperty.AGENT_LAYER);
    }

    /**
     * Determines if we should modify a method: generally they should be declared in this class and
     * not abstract.
     */
    public static boolean shouldModify(CtClass cc, CtBehavior m) {
        if (m.getDeclaringClass() != cc || Modifier.isAbstract(m.getModifiers())) {
            return false;
        }
        return true;
    }

    /**
     * Determines if class has a method with given name/signature
     */
    protected static boolean hasMethod(CtClass cc, String name, String signature) {
        boolean hasMethod;
        
        try {
            cc.getMethod(name, signature);
            hasMethod = true;
        } catch(NotFoundException ex) {
            hasMethod = false;
        }
    
        return hasMethod;
    }

    /**
     * Adds out interface to an existing class ("tags" it...) so we can access the modified
     * class during layer entry/exit
     * @param cc Javassist class
     * @param interfaceName Fully qualified package/class name of interface
     * @return false if already tagged, otherwise true
     * @throws NotFoundException
     */
    protected boolean tagInterface(CtClass cc, String interfaceName)
        throws NotFoundException {

        CtClass iface = classPool.getCtClass(interfaceName);
        for(CtClass i : cc.getInterfaces()) {
            if (i.equals(iface)) {
                return false; // already tagged
            }
        }

        cc.addInterface(iface);
        return true;    
    }
    
    
    /**
     * Add to the provided class the setter and getter methods for context as Object, such as the instance of the patched class can carry context object(x-trace id).
     * 
     * The class will then be tagged as {@link TvContextObjectAware}
     * 
     * The main usage of this is to store/restore x-trace context among different threads
     * 
     * Take note that this is actually preferred over {@link ClassInstrumentation#addTvContextAware} as keeping track of the Metadata object has the advantage of getting the most
     * up-to-dated x-trace id should there be any events generated before the forked entry event and async exit event
     *   
     * @param cc
     * @throws CannotCompileException
     * @throws NotFoundException
     */
    protected void addTvContextObjectAware(CtClass cc) throws CannotCompileException, NotFoundException {
        String metadataClassName = Metadata.class.getName();
        cc.addField(CtField.make("private " + metadataClassName + " tvContextObject;", cc));
        cc.addMethod(CtNewMethod.make("public void setTvContext(" + metadataClassName + " metadata) { tvContextObject = metadata; }", cc));
        cc.addMethod(CtNewMethod.make("public " + metadataClassName + " getTvContext() { return tvContextObject; }", cc));
        
        cc.addField(CtField.make("private " + metadataClassName + " tvClonedContext;", cc));
        cc.addMethod(CtNewMethod.make("public void tvSetClonedContext(" + metadataClassName + " metadata) { tvClonedContext = metadata; }", cc));
        cc.addMethod(CtNewMethod.make("public " + metadataClassName + " tvGetClonedContext() { return tvClonedContext; }", cc));
        
        cc.addField(CtField.make("private " + metadataClassName + " tvPreviousContextObject;", cc));
        cc.addMethod(CtNewMethod.make("public void setTvPreviousContext(" + metadataClassName + " metadata) { tvPreviousContextObject = metadata; }", cc));
        cc.addMethod(CtNewMethod.make("public " + metadataClassName + " getTvPreviousContext() { return tvPreviousContextObject; }", cc));
        
        if (!hasField(cc, "tvFromThreadId")) {
            cc.addField(CtField.make("private long tvFromThreadId;", cc));
        }
        cc.addMethod(CtNewMethod.make("public void setTvFromThreadId(long threadId) { tvFromThreadId = threadId; }", cc));
        cc.addMethod(CtNewMethod.make("public long getTvFromThreadId() { return tvFromThreadId; }", cc));
        
        cc.addField(CtField.make("private boolean tvRestored;", cc));
        cc.addMethod(CtNewMethod.make("public void setTvRestored(boolean restored) { tvRestored = restored; }", cc));
        cc.addMethod(CtNewMethod.make("public boolean tvRestored() { return tvRestored; }", cc));
        
        tagInterface(cc, TvContextObjectAware.class.getName());
    }
    
    private boolean hasField(CtClass cc, String fieldName) {
        try {
            cc.getDeclaredField(fieldName);
            return true;
        } catch (NotFoundException e) {
            return false;
        }
    }

    /**
     * Patches this class to add handles to store/get Span
     * @param cc
     * @return  true if the class is updated with the patch; false if the class was previously patched and no change is made on this current invocation
     * @throws CannotCompileException
     * @throws NotFoundException
     */
    protected boolean addSpanAware(CtClass cc) throws CannotCompileException, NotFoundException {
        String spanClassName = Span.class.getName();

        boolean isPatched = cc.subtypeOf(classPool.get(SpanAware.class.getName()));

        if (!isPatched) {
            cc.addField(CtField.make("private " + spanClassName + " tvSpan;", cc));
            cc.addMethod(CtNewMethod.make(
                    "public void tvSetSpan(" + spanClassName + " span) { "
                            + "    tvSpan = span; "
                            + "}", cc));
            cc.addMethod(CtNewMethod.make("public " + spanClassName + " tvGetSpan() { return tvSpan; }", cc));

            tagInterface(cc, SpanAware.class.getName());
            return true;
        } else {
            return false;
        }
    }
    
    public static void insertBefore(CtBehavior method, String source) throws CannotCompileException {
        insertBefore(method, source, true);
    }
    
    public static void insertBefore(CtBehavior method, String source, boolean contextCheck) throws CannotCompileException {
        if (contextCheck) {
            source = wrapWithContextCheck(source);
        }
        source = wrapWithCatch(source); //to avoid injected code throwing exception to client's code
        
        method.insertBefore(source);
    }
    
    
    public static void insertAfter(CtBehavior method, String source) throws CannotCompileException {
        insertAfter(method, source, false);
    }
    
    public static void insertAfter(CtBehavior method, String source, boolean asFinally) throws CannotCompileException {
        insertAfter(method, source, asFinally, true);
    }
    
    
    public static void insertAfter(CtBehavior method, String source, boolean asFinally, boolean contextCheck) throws CannotCompileException {
        if (contextCheck) {
            source = wrapWithContextCheck(source);
        }
        
        source = wrapWithCatch(source); //to avoid injected code throwing exception to client's code
        method.insertAfter(source, asFinally);
    }
    
    protected static void insertBeforeMethodCall(MethodCall methodCall, String source) throws CannotCompileException {
        insertBeforeMethodCall(methodCall, source, true);
    }
    
    protected static void insertBeforeMethodCall(MethodCall methodCall, String source, boolean contextCheck) throws CannotCompileException {
        if (contextCheck) {
            source = wrapWithContextCheck(source);
        }
        
        //TODO temporarily remove the wraps due to javassist restriction with using try/catch in replace() code
        //Filed an issue to javassist https://issues.jboss.org/browse/JASSIST-210
        //source = wrapWithCatch(source); //to avoid injected code throwing exception to client's code
        
        source = "{" + source + " $_ = $proceed($$); }";
        methodCall.replace(source);
    }
    
    protected static void insertAfterMethodCall(MethodCall methodCall, String source) throws CannotCompileException {
        insertAfterMethodCall(methodCall, source, true);
    }
    
    protected static void insertAfterMethodCall(MethodCall methodCall, String source, boolean contextCheck) throws CannotCompileException {
        if (contextCheck) {
            source = wrapWithContextCheck(source);
        }
        
        //TODO temporarily remove the wraps due to javassist restriction with using try/catch in replace() code
        //Filed an issue to javassist https://issues.jboss.org/browse/JASSIST-210
        //source = wrapWithCatch(source); //to avoid injected code throwing exception to client's code
        
        source = "{ $_ = $proceed($$); " + source + "}";
        methodCall.replace(source);
    }
    
    private static String wrapWithContextCheck(String source) {
        StringBuffer wrappedSrc = new StringBuffer();
        String contextClassName = Context.class.getName();
        wrappedSrc.append("if (" + contextClassName + ".getMetadata().isSampled()) {");
        wrappedSrc.append(source);
        wrappedSrc.append("}");
        
        return wrappedSrc.toString();
    }
    
    protected static String wrapWithCatch(String source) {
        StringBuffer wrappedSrc = new StringBuffer();
        wrappedSrc.append("try {");
        wrappedSrc.append(source);
        wrappedSrc.append("} catch (Throwable e) {");
        wrappedSrc.append("    try { "); //wrap again, just in case logThrowable throws exceptions
        wrappedSrc.append(         ClassInstrumentation.class.getName() + ".logThrowable(e);");
        wrappedSrc.append("    } catch (Throwable e) {"); //use the plain System.err if all fails
        wrappedSrc.append("        System.err.println(\"[" + Logger.APPOPTICS_TAG + "] Caught exception as below. Please take note that existing code flow should not be affected, this might only impact the instrumentation of current trace\"); ");
        wrappedSrc.append("        System.err.println(e.getMessage()); ");
        wrappedSrc.append("        e.printStackTrace(); ");
        wrappedSrc.append("    }");
        wrappedSrc.append("}");
            
        return wrappedSrc.toString();
    }

    public static void logThrowable(Throwable e) {
        logger.warn("Caught exception as below. Please take note that existing code flow should not be affected, this might only impact the instrumentation of current trace");
        logger.warn(e.getMessage(), e);
    }
    
    // Often needed during development to figure out method signatures:
    protected void dumpMethods(CtClass cc) {
        System.out.println("#### Class: " + cc.getName());
        for(CtMethod m: cc.getMethods()) {
            System.out.println("## Method: " + m.getName() + " Signature: " + m.getSignature() + 
                                " Public: " + Modifier.isPublic(m.getModifiers()) +
                                " Declaring class: " + m.getDeclaringClass().getName() +
                                " InThis: " + (cc == m.getDeclaringClass()));
        }

    }
    
    /**
     * @deprecated	use {@link ClassInstrumentation#startTraceAsSpan(String, Map, String)} instead 
     * @param layerName
     * @param xTraceHeaders
     * @return
     */
    protected static Event startTrace(String layerName, Map<XTraceHeader, String> xTraceHeaders) {
        return startTrace(layerName, xTraceHeaders, null);
    }
    
    
    /**
     * @deprecated	use {@link ClassInstrumentation#startTraceAsSpan(String, Map, String)} instead
     * @param layerName
     * @param xTraceHeaders
     * @param resource
     * @return
     */
    public static Event startTrace(String layerName, Map<XTraceHeader, String> xTraceHeaders, String resource) {
        Metadata contextMetadata = Context.getMetadata();
        String xtrace = xTraceHeaders.get(XTraceHeader.TRACE_ID);
        if (xtrace != null && !Metadata.isCompatible(xtrace)) { //ignore x-trace id if it's not compatible
            logger.debug("Not accepting X-Trace ID [" + xtrace + "] for trace continuation");
            xtrace = null;
        }
        
        //check if it's a trigger trace
        XTraceOptions xTraceOptions = XTraceOptions.getXTraceOptions(xTraceHeaders.get(XTraceHeader.TRACE_OPTIONS), xTraceHeaders.get(XTraceHeader.TRACE_OPTIONS_SIGNATURE));
        TraceDecision decision = TraceDecisionUtil.shouldTraceRequest(layerName, xtrace, xTraceOptions, resource);

        boolean startTrace = false;
        
        if (decision.isSampled()) {
            if (xtrace != null) {
                logger.debug("Continuing trace: " + xtrace);

                try {
                    contextMetadata.fromHexString(xtrace);
                } catch(OboeException ex) {
                    logger.debug("Invalid X-Trace header received: " + xtrace);
                }
            } else {
                startTrace = true;
                logger.debug("Starting new trace");
                
                //randomize the context metadata provided
                contextMetadata.randomize(true);
            }
        } else { //do not trace
            if (xtrace != null) {
                logger.debug("Propagation but not continuting trace: " + xtrace);
                try {
                    contextMetadata.fromHexString(xtrace);
                    contextMetadata.setSampled(false); //make it not sampled
                } catch(OboeException ex) {
                    logger.debug("Invalid X-Trace header received: " + xtrace);
                }
            } else {
                contextMetadata.randomize(false); //create a new x-trace ID but not sampled
            }
        }
        
        contextMetadata.setReportMetrics(decision.isReportMetrics());
        
        if (contextMetadata.isSampled()) {
            Event entryEvent = Context.createEventWithContext(contextMetadata, !startTrace); //do not add edge for trace start 
            
            if (startTrace && decision.getTraceConfig() != null) { //only add certain KVs if this is a start of a new trace
                TraceConfig config = decision.getTraceConfig();
                RequestType requestType = decision.getRequestType();
                entryEvent.addInfo("SampleRate", requestType.isTriggerTrace() ? -1 : config.getSampleRate());
                entryEvent.addInfo("SampleSource", requestType.isTriggerTrace() ? -1 : config.getSampleRateSourceValue());
                entryEvent.addInfo("BucketCapacity", config.getBucketCapacity(requestType.getBucketType()));
                entryEvent.addInfo("BucketRate", config.getBucketRate(requestType.getBucketType()));
                
                if (xTraceOptions != null) {
                    if (requestType.isTriggerTrace()) {
                        entryEvent.addInfo("TriggeredTrace", true);
                    }
                    Map<XTraceOption<String>, String> customKvs = xTraceOptions.getCustomKvs();
                    if (customKvs != null) {
                        for (Map.Entry<XTraceOption<String>, String> entry : customKvs.entrySet()) {
                            entryEvent.addInfo(entry.getKey().getKey(), entry.getValue());
                        }
                    }
                    
                    String pdKeys = xTraceOptions.getOptionValue(XTraceOption.PD_KEYS);
                    if (pdKeys != null) {
                        entryEvent.addInfo("PDKeys", pdKeys);
                    }
                }
            }
            
            return entryEvent;
        } else { //not sampled
            return null;
        }
    }
    
    /**
     * Convenient method to start a trace with an Span and is NOT tracked in scope/context
     *
     * @param layerName
     * @param xTraceHeaders
     * @param resource  Resource of the span, for example URL for web request or topic for rabbitMQs etc
     * @param triggerProfiling  whether this trace entry point is valid for triggering profiling
     * @return
     */
    public static Span startTraceAsSpan(String layerName, Map<XTraceHeader, String> xTraceHeaders, String resource, boolean triggerProfiling) {
        SpanBuilder builder = getStartTraceSpanBuilder(layerName, xTraceHeaders, resource, triggerProfiling);
        return builder.start();
    }
    
    /**
     * Fetch x-forwarded-* headers from a provided Function object and assign them into the span.
     * @param span
     * @param req
     */
    public static void setForwardedTags(Span span, HeaderExtractor<String, String> req) {
        for (String tag : forwardedTags) {
            String header = "x-" + tag.toLowerCase();
            String val = req.extract(header);
            if (val != null) {
                span.setTag(tag, val);
            }
        }
    }
    
    /**
     * Deprecated. A method to start a trace with an deprecated {@link ActiveSpan}
     *
     * Take note that ActiveSpan created will have profiler reporter attached
     * 
     * @deprecated use getStartTraceSpanBuilder and add tags there instead, this is only used by legacy SDK for now
     * @param layerName
     * @param xTraceHeaders
     * @param resource
     * @param tags  extra tags to be included in the span. Visible when the correspond {@link SpanReporter#reportOnStart} is invoked 
     * @return
     */
    public static ActiveSpan startTraceAsSpan(String layerName, Map<XTraceHeader, String> xTraceHeaders, String resource, Map<String, Object> tags) {
        SpanBuilder builder = getStartTraceSpanBuilder(layerName, xTraceHeaders, resource, true);
        if (tags != null && !tags.isEmpty()) {
            builder.withTags(tags);
        }
        
        return new ActiveSpan(builder.startActive());
    }
    
    /**
     * Convenient method to start a trace with a Scope with is tracked by context, ie the metadata stored in the wrapped span of this Scope is the same instance as the TLS context metadata
     *
     * @param layerName
     * @param xTraceHeaders
     * @param resource
     * @param triggerProfiling  whether this trace entry point is valid for triggering profiling
     * @return
     */
    public static Scope startTraceAsScope(String layerName, Map<XTraceHeader, String> xTraceHeaders, String resource, boolean triggerProfiling) {
        SpanBuilder builder = getStartTraceSpanBuilder(layerName, xTraceHeaders, resource, triggerProfiling);

        return builder.startActive();
    }

    /**
     * Convenient method to start a trace with a Scope with is tracked by context, ie the metadata stored in the wrapped span of this Scope is the same instance as the TLS context metadata
     * @param layerName
     * @param xTraceHeaders
     * @param resource
     * @param tags  extra tags to be included in the Scope. Visible when the correspond {@link SpanReporter#reportOnStart} is invoked
     * @param triggerProfiling  whether this trace entry point is valid for triggering profiling
     * @return
     */
    public static Scope startTraceAsScope(String layerName, Map<XTraceHeader, String> xTraceHeaders, String resource, Map<String, Object> tags, boolean triggerProfiling) {
        SpanBuilder builder = getStartTraceSpanBuilder(layerName, xTraceHeaders, resource, triggerProfiling);
        if (tags != null && !tags.isEmpty()) {
            builder.withTags(tags);
        }
        
        return builder.startActive();
    }

    /**
     * Convenient method to get a {@link SpanBuilder} to start a trace.
     *
     * Parameters required for making the trace decision is added by this method to the returned SpanBuilder
     *
     * Take note that SpanBuilder created will have profiler reporter attached
     *
     * @param layerName
     * @param xTraceHeaders
     * @param resource
     *
     * @return
     */
    public static SpanBuilder getStartTraceSpanBuilder(String layerName, Map<XTraceHeader, String> xTraceHeaders, String resource) {
        return getStartTraceSpanBuilder(layerName, xTraceHeaders, resource, true);
    }
    
    /**
     * Convenient method to get a {@link SpanBuilder} to start a trace. 
     * 
     * Parameters required for making the trace decision is added by this method to the returned SpanBuilder
     * 
     * @param layerName
     * @param xTraceHeaders
     * @param resource
     * @param triggerProfiling  whether this trace entry point is valid for triggering profiling
     * @return
     */
    public static SpanBuilder getStartTraceSpanBuilder(String layerName, Map<XTraceHeader, String> xTraceHeaders, String resource, boolean triggerProfiling) {
        SpanBuilder spanBuilder = Tracer.INSTANCE.buildSpan(layerName).withReporters(TraceEventSpanReporter.REPORTER, InboundMetricMeasurementSpanReporter.REPORTER, MetricHistogramSpanReporter.REPORTER);

        if (PROFILER_ENBALED && triggerProfiling) {
            spanBuilder = spanBuilder.withReporters(ProfilingSpanReporter.REPORTER);
        }

        spanBuilder = spanBuilder.withSpanProperty(SpanProperty.TRACE_DECISION_PARAMETERS, new TraceDecisionParameters(xTraceHeaders, resource));
        
        if (xTraceHeaders.containsKey(XTraceHeader.SPAN_ID)) {
            String spanIdString = xTraceHeaders.get(XTraceHeader.SPAN_ID);
            try {
                Span parentSpan = SpanDictionary.getSpan(Long.parseLong(spanIdString));
                if (parentSpan != null) {
                    spanBuilder = spanBuilder.asChildOf(parentSpan);
                } else { //could be coming from a proxy server, in this case the one that inserts the span is not on the same process as this
                    logger.debug("Do not continue with span id [" + spanIdString + "] as such a span is not found in the current process");
                }
            } catch (NumberFormatException e) { 
                logger.warn("Failed to continue on with span id [" + spanIdString + "] as the id is not a valid long");
            }
        }
        
        return spanBuilder;
    }

    public static SpanBuilder buildTraceEventSpan(String operationName) {
        return tracer.buildSpan(operationName).withReporters(TraceEventSpanReporter.REPORTER);
    }
    
    /**
     * Returns a Map with non abstract methods from the CtClass as keys, which match any of the methodMatchers provided.
     * 
     * The Map value contains the matching matcher's instType. 
     * 
     * Take note that a single method might match multiple matchers but only the first matcher will be taken with its instType included in the returned Map
     *  
     * @param cc
     * @param methodMatchers
     * @return  a Map with key as methods from the CtClass matches any of the methodMatchers provided, the Map value contains the matching matcher's instType.
     */
    protected <T> Map<CtMethod, T> findMatchingMethods(CtClass cc, List<MethodMatcher<T>> methodMatchers) {
        return findMatchingBehaviors(cc.getDeclaredMethods(), methodMatchers);
    }
    
    /**
     * Returns a Map with constructors from the CtClass as keys, which match any of the constructorMatchers provided
     * 
     * The Map value contains the matching matcher's instType.
     * 
     * Take note that a single constructor might match multiple matchers but only the first matcher will be taken with its instType included in the returned Map
     * 
     * @param cc
     * @param constructorMatchers
     * @return  a Map with key as constructors from the CtClass matches any of the constructorMatchers provided, the Map value contains the matching matcher's instType.
     */
    protected <T> Map<CtConstructor, T> findMatchingConstructors(CtClass cc, List<ConstructorMatcher<T>> constructorMatchers) {
        return findMatchingBehaviors(cc.getDeclaredConstructors(), constructorMatchers);
    }
    
    private <T, B extends CtBehavior, M extends MethodMatcher<T>> Map<B, T> findMatchingBehaviors(B[] availableBehaviors, List<M> matchers) {
        Map<String, List<M>> matchersByName = groupMatchersByName(matchers); //group matcher by name for better efficiency
        Map<B, T> matchingBehaviors = new LinkedHashMap<B, T>();
        
        for (B behavior : availableBehaviors) {
            if (Modifier.isAbstract(behavior.getModifiers())) { //do not include abstract methods, we cannot modify them anyway
                continue;
            }
            
            List<M> matchersByThisName;
            
            if (behavior instanceof CtConstructor) { //constructor has no name to match against
                matchersByThisName = matchers;
            } else {
                matchersByThisName = matchersByName.get(behavior.getName());
            }
            
            if (matchersByThisName != null) {
                for (MethodMatcher<T> matcher : matchersByThisName) {
                    if (matcher.matches(behavior, classPool)) {
                        matchingBehaviors.put(behavior, matcher.getInstType()); 
                        break; //take the first match in the list
                    }
                }
            }
        }
        
        return matchingBehaviors;
    }
    
    
    private static <I, M extends MethodMatcher<I>> Map<String, List<M>> groupMatchersByName(List<M> methodMatchers) {
        Map<String, List<M>> matchersByName = new HashMap<String, List<M>>();
        
        for (M methodMatcher : methodMatchers) {
            List<M> matchersWithThisName;
            if (!matchersByName.containsKey(methodMatcher.getMethodName())) {
                matchersWithThisName = new ArrayList<M>();
                matchersByName.put(methodMatcher.getMethodName(), matchersWithThisName);
            } else {
                matchersWithThisName = matchersByName.get(methodMatcher.getMethodName());
            }
            
            matchersWithThisName.add(methodMatcher);            
        }
        
        return matchersByName;
    }
    
    /**
     * Extract a header by its name.
     * @param <T>
     * @param <R>
     */
    public interface HeaderExtractor<T, R> {
        R extract(T t);
    }

    /**
     * Extract special x-trace headers from the extractor
     * @param       extractor that extracts the value from a String key and returns a String value if entry is found, null otherwise
     * @return      a Map with key of type XTraceHeader
     * @see         XTraceHeader
     */
    public static Map<XTraceHeader, String> extractXTraceHeaders(HeaderExtractor<String, String> extractor) {
        Map<XTraceHeader, String> headers = new HashMap<XTraceHeader, String>();

        for (Map.Entry<String, XTraceHeader> xtraceHeaderEntry : XTRACE_HTTP_HEADER_KEYS.entrySet()) {
            String headerValue;
            if ((headerValue = extractor.extract(xtraceHeaderEntry.getKey())) != null) {
                headers.put(xtraceHeaderEntry.getValue(), headerValue);
            }
        }

        return headers;
    }
}
