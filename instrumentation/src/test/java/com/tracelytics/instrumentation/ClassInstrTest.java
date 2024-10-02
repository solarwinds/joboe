package com.tracelytics.instrumentation;

import com.tracelytics.joboe.*;
import com.tracelytics.joboe.config.ConfigContainer;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.joboe.config.InvalidConfigException;
import com.tracelytics.joboe.settings.Settings;
import com.tracelytics.joboe.settings.SettingsArg;
import com.tracelytics.joboe.settings.TestSettingsReader;
import com.tracelytics.joboe.settings.TestSettingsReader.SettingsMockupBuilder;
import com.tracelytics.joboe.span.impl.Scope;
import com.tracelytics.joboe.span.impl.ScopeManager;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.joboe.span.impl.Span.SpanProperty;
import com.tracelytics.util.TestUtils;
import junit.framework.TestCase;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Used to be named ClassInstrumentationTest, but renamed to ClassInstrTest.
 * 
 * In order to have this to run as a part of normal unit test case instead of "instrumentation-test" with runs with the agent jar, this class name cannot
 * contain phase "InstrumentationTest" in it
 * 
 * @author pluk
 *
 */
public class ClassInstrTest extends TestCase {
    private static final Settings SAMPLED_SETTINGS =  new SettingsMockupBuilder().withFlags(true, false, true, true, false).withSampleRate(1000000).withSettingsArg(SettingsArg.BUCKET_CAPACITY, 16.0).withSettingsArg(SettingsArg.BUCKET_RATE, 8.0).build(); //ALWAYS sample rate = 100%
    private static final Settings NOT_SAMPLED_SETTINGS =  new SettingsMockupBuilder().withFlags(true, false, true, true, false).withSampleRate(0).build(); //Sampling enabled but sample rate = 0 . Metrics only then
    private static final Settings NOT_TRACED_SETTINGS = new SettingsMockupBuilder().withFlags(false, false, false, false, false).withSampleRate(0).build(); //all tracing (sampling/metrics) disabled
    private static final TestSettingsReader testSettingsReader = TestUtils.initSettingsReader();
    private EventReporter originalReporter;


    @Override
    protected void setUp() throws Exception {
        super.setUp();
        testSettingsReader.put(TestUtils.getDefaultSettings());
        originalReporter = EventImpl.getEventReporter();
        EventImpl.setDefaultReporter(TestUtils.initTraceReporter());
    }

    @Override
    protected void tearDown() throws Exception {
        testSettingsReader.reset();
        ScopeManager.INSTANCE.removeAllScopes();
        Context.clearMetadata();
        TraceDecisionUtil.reset();
        EventImpl.setDefaultReporter(originalReporter);
        super.tearDown();
    }


    public void testAddBackTrace1() throws BsonBufferException {
        Context.startTrace(); //initialize metadata
        Event event;
        
        //Test with normal case
        event = Context.createEvent();
        ClassInstrumentation.addBackTrace(event, 0, null);
        
        //get current number of stack trace elements
        int traceElementCountOnEnclosingMethod = Thread.currentThread().getStackTrace().length -1; //minus the getStackTrace() call from this line
        
        int expectedTraceLineCount = traceElementCountOnEnclosingMethod; 
        assertEquals(expectedTraceLineCount, getLineCount((String)TestingUtil.getValueFromEvent(event, "Backtrace")));
        
        
        //Test with case with more than 200 stack trace elements
        event = Context.createEvent();
        recursiveCall(event, 0, null);
        
        expectedTraceLineCount = Constants.MAX_BACK_TRACE_LINE_COUNT + 1; //+1 because we added an info line about the skipping
        assertEquals(expectedTraceLineCount, getLineCount((String)TestingUtil.getValueFromEvent(event, "Backtrace")));
        
        Context.clearMetadata();
    }
    
    public void testAddBackTrace2() throws BsonBufferException {
        Context.startTrace(); //initialize metadata
        Event event;
        
        //Test with normal case
        event = Context.createEvent();
        Throwable exception = new RuntimeException("test");
        ClassInstrumentation.addBackTrace(event, exception.getStackTrace());
        
        assertEquals(exception.getStackTrace().length, getLineCount((String)TestingUtil.getValueFromEvent(event, "Backtrace")));
        
        //Test with case with more than 200 stack trace elements
        event = Context.createEvent();
        recursiveCallException(event, 0, null);
        
        int expectedTraceLineCount = Constants.MAX_BACK_TRACE_LINE_COUNT + 1; //+1 because we added an info line about the skipping
        assertEquals(expectedTraceLineCount, getLineCount((String)TestingUtil.getValueFromEvent(event, "Backtrace")));
        
        Context.clearMetadata();
        
    }
    
    public void testAddBackTraceByConfig() throws Exception {
        Context.startTrace(); //initialize metadata
        Event event;

        //test no EXTEND_BACK_TRACES flag (so it is false)
        event = Context.createEvent();
        ClassInstrumentation.addBackTrace(event, 0, Module.X_MEMCACHED); //check against SERVLET module

        //should no longer report a backtrace as it's NOT enabled by default
        try {
            TestingUtil.getValueFromEvent(event, "Backtrace");
            fail("getting backtrace should have thrown IllegalArgumentException but it didn't");
        } catch (IllegalArgumentException e) {
            //expected as Backtrace should be missing
        }

        ConfigContainer configContainer;
        //set config EXTEND_BACK_TRACES to true
        configContainer = new ConfigContainer();
        configContainer.put(ConfigProperty.AGENT_EXTENDED_BACK_TRACES, true);
        ClassInstrumentation.initBackTraceModules(configContainer);
        
        int expectedTraceLineCount;
        
        event = Context.createEvent();
        ClassInstrumentation.addBackTrace(event, 0, Module.X_MEMCACHED); 
        
        //get current number of stack trace elements should still have back trace as AGENT_EXTENDED_BACK_TRACES was set as true
        expectedTraceLineCount = Thread.currentThread().getStackTrace().length -1; //minus the getStackTrace() call from this line 
        assertEquals(expectedTraceLineCount, getLineCount((String)TestingUtil.getValueFromEvent(event, "Backtrace")));

        //set config EXTEND_BACK_TRACES to false
        configContainer = new ConfigContainer();
        configContainer.put(ConfigProperty.AGENT_EXTENDED_BACK_TRACES, false);
        ClassInstrumentation.initBackTraceModules(configContainer);
        
        event = Context.createEvent();
        ClassInstrumentation.addBackTrace(event, 0, Module.X_MEMCACHED); 
        
        //should no longer report a backtrace as it's NOT enabled by default
        try {
            TestingUtil.getValueFromEvent(event, "Backtrace");
            fail("getting backtrace should have thrown IllegalArgumentException but it didn't");
        } catch (IllegalArgumentException e) { 
            //expected as Backtrace should be missing
        }
        
        //test module specific flag on X_MEMCACHED
        configContainer = new ConfigContainer();
        configContainer.put(ConfigProperty.AGENT_BACKTRACE_MODULES, Collections.singletonList(Module.X_MEMCACHED));
        ClassInstrumentation.initBackTraceModules(configContainer);
        
        event = Context.createEvent();
        ClassInstrumentation.addBackTrace(event, 0, Module.X_MEMCACHED); //check against SERVLET module
        
      //get current number of stack trace elements should still have back trace as it's enabled by the AGENT_EXTENDED_BACK_TRACES_BY_MODULE
        expectedTraceLineCount = Thread.currentThread().getStackTrace().length -1; //minus the getStackTrace() call from this line 
        assertEquals(expectedTraceLineCount, getLineCount((String)TestingUtil.getValueFromEvent(event, "Backtrace")));

        Context.clearMetadata();
    }
    
    public void testReportError() throws Exception {
        EventReporter existingDefaultReporter = null;
        try {
            TestReporter tracingReporter = TestUtils.initTraceReporter();
            //set it as default reporter for EventImpl
            existingDefaultReporter = EventImpl.setDefaultReporter(tracingReporter);

            Context.getMetadata().randomize(true); //create valid context


            Map<String, Object> sentKvs;
            Exception testException = new RuntimeException("testing");

            ClassInstrumentation.reportError("layer-1", testException);
            assertEquals(1, tracingReporter.getSentEvents().size());
            sentKvs = tracingReporter.getSentEvents().get(0).getSentEntries();
            tracingReporter.reset();

            assertEquals("error", sentKvs.get("Spec"));
            assertEquals("error", sentKvs.get("Label"));
            assertEquals("layer-1", sentKvs.get("Layer"));
            assertEquals(testException.getClass().getName(), sentKvs.get("ErrorClass"));
            assertEquals(testException.getMessage(), sentKvs.get("ErrorMsg"));
            assertEquals(true, sentKvs.containsKey("Backtrace"));

            Span span = ClassInstrumentation.startTraceAsSpan("layer-2", Collections.EMPTY_MAP, null, false);
            tracingReporter.reset(); //removes the entry event

            ClassInstrumentation.reportError(span, testException);
            assertEquals(1, tracingReporter.getSentEvents().size());
            sentKvs = tracingReporter.getSentEvents().get(0).getSentEntries();
            tracingReporter.reset();

            assertEquals("error", sentKvs.get("Spec"));
            assertEquals("error", sentKvs.get("Label"));
            assertEquals("layer-2", sentKvs.get("Layer"));
            assertEquals(testException.getClass().getName(), sentKvs.get("ErrorClass"));
            assertEquals(testException.getMessage(), sentKvs.get("ErrorMsg"));
            assertEquals(true, sentKvs.containsKey("Backtrace"));

            ClassInstrumentation.reportError(span, testException, "another message");
            assertEquals(1, tracingReporter.getSentEvents().size());
            sentKvs = tracingReporter.getSentEvents().get(0).getSentEntries();
            tracingReporter.reset();

            assertEquals("error", sentKvs.get("Spec"));
            assertEquals("error", sentKvs.get("Label"));
            assertEquals("layer-2", sentKvs.get("Layer"));
            assertEquals(testException.getClass().getName(), sentKvs.get("ErrorClass"));
            assertEquals("another message", sentKvs.get("ErrorMsg"));
            assertEquals(true, sentKvs.containsKey("Backtrace"));
        } finally {
            //revert the default reporter for EventImpl
            EventImpl.setDefaultReporter(existingDefaultReporter);
        }
    }
    
    public void testGetBackTraceModulesFromConfigs() throws InvalidConfigException {
        Set<Module> expectedModules;
        ConfigContainer configs;
        
        configs = new ConfigContainer(); //by default no config at all, it should enable all modules - extended ones
        expectedModules = new HashSet<Module>(Arrays.asList(Module.values()));
        expectedModules.removeAll(ClassInstrumentation.allExtendedBackTraceModules);
        assertEquals(expectedModules, ClassInstrumentation.getBackTraceModulesFromConfigs(configs));
        
        configs = new ConfigContainer();
        configs.put(ConfigProperty.AGENT_BACKTRACE_MODULES, Arrays.asList(Module.SERVLET, Module.NETTY));
        expectedModules = new HashSet<Module>(Arrays.asList(Module.SERVLET, Module.NETTY));
        assertEquals(expectedModules, ClassInstrumentation.getBackTraceModulesFromConfigs(configs));
        
        configs = new ConfigContainer();
        configs.put(ConfigProperty.AGENT_BACKTRACE_MODULES, Collections.emptyList());
        expectedModules = new HashSet<Module>();
        assertEquals(expectedModules, ClassInstrumentation.getBackTraceModulesFromConfigs(configs));
        
        //legacy parameter AGENT_EXTENDED_BACK_TRACES
        configs = new ConfigContainer();
        configs.put(ConfigProperty.AGENT_EXTENDED_BACK_TRACES, true); //equivalent to all modules
        expectedModules = new HashSet<Module>(Arrays.asList(Module.values()));
        assertEquals(expectedModules, ClassInstrumentation.getBackTraceModulesFromConfigs(configs));
        
        //legacy parameter AGENT_EXTENDED_BACK_TRACES_BY_MODULE
        configs = new ConfigContainer();
        configs.put(ConfigProperty.AGENT_EXTENDED_BACK_TRACES_BY_MODULE, Arrays.asList(Module.PLAY));
        expectedModules = new HashSet<Module>(Arrays.asList(Module.values()));
        expectedModules.removeAll(ClassInstrumentation.allExtendedBackTraceModules);
        expectedModules.add(Module.PLAY);
        assertEquals(expectedModules, ClassInstrumentation.getBackTraceModulesFromConfigs(configs));
    }
    
    public void testStartTraceAsScopeSampled() {
        testSettingsReader.put(SAMPLED_SETTINGS);
        Map<String, Object> tags = Collections.<String, Object>singletonMap("test-tag", "test-value");
        Scope scope = ClassInstrumentation.startTraceAsScope("test-layer", Collections.<XTraceHeader, String>emptyMap(), null, tags, false);
        Span span = scope.span();
        
        //assert TLS context is set properly
        assertEquals(true, Context.getMetadata().isValid());
        assertEquals(true, Context.getMetadata().isSampled());
        
        
        //assert the span has correct properties
        assertEquals("test-layer", span.getOperationName());
        assertTrue(span.getTags().keySet().containsAll(tags.keySet()));
        assertTrue(span.getSpanPropertyValue(SpanProperty.TRACE_DECISION).getTraceConfig() != null);
        assertTrue(Context.getMetadata() == span.context().getMetadata()); //should be the same instance as this is an active span
    }
    
    public void testStartTraceAsScopeNotSampled() {
        testSettingsReader.put(NOT_SAMPLED_SETTINGS);
        Map<String, Object> tags = Collections.<String, Object>singletonMap("test-tag", "test-value");
        Scope scope = ClassInstrumentation.startTraceAsScope("test-layer", Collections.<XTraceHeader, String>emptyMap(), null, tags, false);
        Span span = scope.span();
        
        //assert TLS context is set properly
        assertEquals(true, Context.getMetadata().isValid());
        assertEquals(false, Context.getMetadata().isSampled());
        
        
        //assert the span has correct properties
        assertEquals("test-layer", span.getOperationName());
        assertTrue(span.getTags().keySet().containsAll(tags.keySet()));
        List<String> entryServiceKvs = new ArrayList<String>(Arrays.asList("SampleRate", "SampleSource", "BucketCapacity", "BucketRate"));
        entryServiceKvs.retainAll(span.getTags().keySet());
        assertTrue(entryServiceKvs.isEmpty()); //should NOT contain any of the entry service keys, not sampled
        
        assertTrue(Context.getMetadata() == span.context().getMetadata()); //should be the same instance as this is an active span
    }
    
    public void testStartTraceAsScopeNotTraced() {
        testSettingsReader.put(NOT_TRACED_SETTINGS);
        Map<String, Object> tags = Collections.<String, Object>singletonMap("test-tag", "test-value");
        Scope scope = ClassInstrumentation.startTraceAsScope("test-layer", Collections.<XTraceHeader, String>emptyMap(), null, tags, false);
        Span span = scope.span();
        
        //assert TLS context is set properly
        assertEquals(true, Context.getMetadata().isValid()); //even when it's completely not traced, we still want a valid context (isSampled set to false)
        assertEquals(false, Context.getMetadata().isSampled());
        
        
        //assert the span has correct properties
        assertEquals("test-layer", span.getOperationName());
        assertTrue(span.getTags().keySet().containsAll(tags.keySet()));
        List<String> entryServiceKvs = new ArrayList<String>(Arrays.asList("SampleRate", "SampleSource", "BucketCapacity", "BucketRate"));
        entryServiceKvs.retainAll(span.getTags().keySet());
        assertTrue(entryServiceKvs.isEmpty()); //should NOT contain any of the entry service keys, not sampled
        assertTrue(Context.getMetadata() == span.context().getMetadata()); //should be the same instance as this is an active span
    }
    
    /**
     * Test startTraceAsSpan with trace mode always and an incoming x-trace ID
     */
    public void testStartTraceAsScopeSampledContinue() {
        testSettingsReader.put(SAMPLED_SETTINGS);
        Map<String, Object> tags = Collections.<String, Object>singletonMap("test-tag", "test-value");
        Metadata incomingMetadata = new Metadata();
        incomingMetadata.randomize(true);
        String xTraceId = incomingMetadata.toHexString();
        Scope scope = ClassInstrumentation.startTraceAsScope("test-layer", Collections.<XTraceHeader, String>singletonMap(XTraceHeader.TRACE_ID, xTraceId), null, tags, false);
        Span span = scope.span();
        
        //assert TLS context is set properly
        assertEquals(true, Context.getMetadata().isValid());
        assertEquals(true, Context.getMetadata().isSampled());
        assertEquals(incomingMetadata.taskHexString(), Context.getMetadata().taskHexString()); //same task
        assertTrue(!incomingMetadata.opHexString().equals(Context.getMetadata().opHexString())); //not the same op as the entry event of this span is reported
        
        
        //assert the span has correct properties
        assertEquals("test-layer", span.getOperationName());
        assertTrue(span.getTags().keySet().containsAll(tags.keySet()));
        List<String> entryServiceKvs = new ArrayList<String>(Arrays.asList("SampleRate", "SampleSource", "BucketCapacity", "BucketRate"));
        entryServiceKvs.retainAll(span.getTags().keySet());
        assertTrue(entryServiceKvs.isEmpty()); //should NOT contain any of the entry service keys, as this is NOT the entry service
        assertTrue(Context.getMetadata() == span.context().getMetadata()); //should be the same instance as this is an active span
    }
    
    /**
     * Test startTraceAsSpan with trace mode always and an incoming x-trace ID
     */
    public void testStartTraceAsScopeNotTracedContinue() {
        testSettingsReader.put(NOT_TRACED_SETTINGS);
        Map<String, Object> tags = Collections.<String, Object>singletonMap("test-tag", "test-value");
        Metadata incomingMetadata = new Metadata();
        incomingMetadata.randomize(true);
        String xTraceId = incomingMetadata.toHexString();
        Scope scope = ClassInstrumentation.startTraceAsScope("test-layer", Collections.<XTraceHeader, String>singletonMap(XTraceHeader.TRACE_ID, xTraceId), null, tags, false);
        Span span = scope.span();
        
        //assert TLS context is set properly
        assertEquals(true, Context.getMetadata().isValid());
        assertEquals(false, Context.getMetadata().isSampled()); //should no longer be sampled/traced as never mode flips the decision
        assertEquals(incomingMetadata.taskHexString(), Context.getMetadata().taskHexString());
        assertEquals(incomingMetadata.opHexString(), Context.getMetadata().opHexString()); //same op as no entry event is generated
        assertTrue(incomingMetadata.isSampled() != Context.getMetadata().isSampled());
        
        
        //assert the span has correct properties
        assertEquals("test-layer", span.getOperationName());
        assertTrue(span.getTags().keySet().containsAll(tags.keySet()));
        List<String> entryServiceKvs = new ArrayList<String>(Arrays.asList("SampleRate", "SampleSource", "BucketCapacity", "BucketRate"));
        entryServiceKvs.retainAll(span.getTags().keySet());
        assertTrue(entryServiceKvs.isEmpty()); //should NOT contain any of the entry service keys, as this is NOT the entry service
        assertTrue(Context.getMetadata() == span.context().getMetadata()); //should be the same instance as this is an active span
    }
    
    private void recursiveCall(Event event, int count, Module module) {
        if (count < 300) { //to ensure we exceed the max stack line
            recursiveCall(event, ++count, module);
        } else {
            ClassInstrumentation.addBackTrace(event, 0, module);
        }
    }
    
    private void recursiveCallException(Event event, int count, Module module) {
        if (count < 300) { //to ensure we exceed the max stack line
            recursiveCall(event, ++count, module);
        } else {
            Throwable exception = new RuntimeException("test");
            ClassInstrumentation.addBackTrace(event, exception.getStackTrace());
        }
    }
    
    private static int getLineCount(String input) {
        return input.split("\n").length;
    }
}
