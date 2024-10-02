package com.tracelytics.instrumentation.http;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tracelytics.joboe.*;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.metrics.MetricKey;
import com.tracelytics.metrics.histogram.Histogram;
import com.tracelytics.metrics.measurement.SummaryLongMeasurement;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.tracelytics.AnyValueValidator;
import com.tracelytics.ExpectedEvent;
import com.tracelytics.ValueValidator;
import com.tracelytics.instrumentation.AbstractInstrumentationTest;
import com.tracelytics.joboe.TestReporter.DeserializedEvent;
import com.tracelytics.joboe.settings.SettingsArg;
import com.tracelytics.joboe.settings.TestSettingsReader.SettingsMockupBuilder;
import com.tracelytics.joboe.span.impl.InboundMetricMeasurementSpanReporter;
import com.tracelytics.joboe.span.impl.MetricHistogramSpanReporter;
import com.tracelytics.joboe.span.impl.MetricMeasurementSpanReporter;
import com.tracelytics.joboe.span.impl.ScopeManager;

public class ServletInstrumentationTest extends AbstractInstrumentationTest<ServletInstrumentation>{
    private MockHttpServletRequest requestWithNoHeader;
    private MockHttpServletRequest requestWithXTrace;
    private MockHttpServletRequest requestWithXTraceIncompatible;
    private MockHttpServletRequest requestWithXTraceNotSampled;
    
    private HttpServlet servlet;

    private TracingMode originalTracingMode;
    private Integer originalSampleRate;
    
    
    private List<ExpectedEvent> expectedNewEvents = new ArrayList<ExpectedEvent>(); //expected events for new trace
    private List<ExpectedEvent> expectedContinueEvents = new ArrayList<ExpectedEvent>(); //expected events for continue trace
    
    private String X_TRACE_ID = getXTraceId(Metadata.CURRENT_VERSION, true);
    private String X_TRACE_ID_INCOMPATIBLE = getXTraceId(Metadata.CURRENT_VERSION + 1, true);
    private String X_TRACE_ID_NOT_SAMPLED = getXTraceId(Metadata.CURRENT_VERSION, false);
    
    private String MOCKED_URL = "/test/1/2/3";
    private String MOCKED_HOST = "localhost:8080";
    private String MOCKED_METHOD = "GET";
    private static final int MOCKED_STATUS_CODE = 200;
    
    //use the thread local reporter
    
    private TestReporter threadLocalReporter;
    private EventReporter originalReporter;
    

    public ServletInstrumentationTest() throws Exception {
        requestWithNoHeader = new MockHttpServletRequest(null, MOCKED_METHOD, MOCKED_URL);
        requestWithXTrace = new MockHttpServletRequest(null, MOCKED_METHOD, MOCKED_URL);
        requestWithXTraceIncompatible = new MockHttpServletRequest(null, MOCKED_METHOD, MOCKED_URL);
        requestWithXTraceNotSampled = new MockHttpServletRequest(null, MOCKED_METHOD, MOCKED_URL);
        
        requestWithNoHeader.addHeader("host", MOCKED_HOST);
        
        requestWithXTrace.addHeader("host", MOCKED_HOST);
        requestWithXTrace.addHeader("X-Trace", X_TRACE_ID);
        
        requestWithXTraceIncompatible.addHeader("host", MOCKED_HOST);
        requestWithXTraceIncompatible.addHeader("X-Trace", X_TRACE_ID_INCOMPATIBLE);
        
        requestWithXTraceNotSampled.addHeader("host", MOCKED_HOST);
        requestWithXTraceNotSampled.addHeader("X-Trace", X_TRACE_ID_NOT_SAMPLED);
        
        servlet = new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            }
            
        };
        
        originalTracingMode = (TracingMode) ConfigManager.getConfig(ConfigProperty.AGENT_TRACING_MODE);
        originalSampleRate = (Integer) ConfigManager.getConfig(ConfigProperty.AGENT_SAMPLE_RATE);
        
        expectedNewEvents.add(new ExpectedEvent("Label", "entry", 
                                             "Layer", "java", 
                                             "SampleRate", new AnyValueValidator(), 
                                             "SampleSource", new AnyValueValidator(),
                                             "BucketRate", new AnyValueValidator(),
                                             "BucketCapacity", new AnyValueValidator(),
                                             "Edge", null)); //new trace should not have an edge
        expectedNewEvents.add(new ExpectedEvent("Label", "info",
                                             "Layer", "java",
                                             "Servlet-Class", new AnyValueValidator(),
                                             "URL", MOCKED_URL));
        expectedNewEvents.add(new ExpectedEvent("Label", "exit",
                                             "Layer", "java",
                                             "Spec", "ws",
                                             "HTTPMethod", MOCKED_METHOD,
                                             "Status", MOCKED_STATUS_CODE,
                                             "Remote-Host",  new AnyValueValidator(),
                                             "URL", MOCKED_URL,
                                             "HTTP-Host", MOCKED_HOST,
                                             "TransactionName", "/test/1"));
        
        expectedContinueEvents.add(new ExpectedEvent("Label", "entry", 
                                                "Layer", "java",
                                                "Edge", new EdgeValidator(Collections.singletonList(X_TRACE_ID)))); //continue trace does not have bucket params nor sample rate KVs
        
        expectedContinueEvents.add(new ExpectedEvent("Label", "info",
                                                "Layer", "java",
                                                "Servlet-Class", new AnyValueValidator(),
                                                "URL", MOCKED_URL));
        expectedContinueEvents.add(new ExpectedEvent("Label", "exit",
                                                "Layer", "java",
                                                "Spec", "ws",
                                                "HTTPMethod", MOCKED_METHOD,
                                                "Status", MOCKED_STATUS_CODE,
                                                "Remote-Host",  new AnyValueValidator(),
                                                "URL", MOCKED_URL,
                                                "HTTP-Host", MOCKED_HOST,
                                                "TransactionName", "/test/1"));
    }
    
    private static javax.servlet.http.HttpServletResponse getResponse() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(MOCKED_STATUS_CODE);
        
        return response;
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ConfigManager.setConfig(ConfigProperty.AGENT_SAMPLE_RATE, TraceDecisionUtil.SAMPLE_RESOLUTION);

        threadLocalReporter = ReporterFactory.getInstance().buildTestReporter(true);
        originalReporter = EventImpl.setDefaultReporter(threadLocalReporter);

        MetricHistogramSpanReporter.REPORTER.consumeMetricEntries(); //reset metrics
        InboundMetricMeasurementSpanReporter.REPORTER.consumeMetricEntries(); //reset metrics
        
        Context.clearMetadata();
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        ConfigManager.reset();
        Context.clearMetadata();
        TraceDecisionUtil.reset();

        EventImpl.setDefaultReporter(originalReporter);

        MetricHistogramSpanReporter.REPORTER.consumeMetricEntries(); //reset metrics
        InboundMetricMeasurementSpanReporter.REPORTER.consumeMetricEntries(); //reset metrics
    }   

    
    public void testServiceAlwaysInstrumentation() throws Exception {
        //use a default settings from remote source
        testSettingsReader.put(new SettingsMockupBuilder().withFlags(TracingMode.ALWAYS).withSampleRate(1000000).build());
        //set tracing mode to ALWAYS;
        ConfigManager.setConfig(ConfigProperty.AGENT_TRACING_MODE, TracingMode.ALWAYS);

        //request with no header, should be traced
        servlet.service(requestWithNoHeader, getResponse());
        assertEvents(expectedNewEvents);
        resetReporter();
        
        //request with X-trace header, should be traced
        servlet.service(requestWithXTrace, getResponse());
        assertEvents(expectedContinueEvents);
        resetReporter();
        
        //request with Incompatible X-trace header, should still be traced, but it should NOT continue with that X-trace ID
        servlet.service(requestWithXTraceIncompatible, getResponse());
        assertEvents(expectedNewEvents);
        resetReporter();
        
        //request with not sampled X-trace header, should not be traced
        servlet.service(requestWithXTraceNotSampled, getResponse());
        assertEvents(Collections.<ExpectedEvent>emptyList()); 
        resetReporter();
        
        assertSpanMetrics(4);
    }
    
    public void testServiceNeverInstrumentation() throws Exception {
        //use a default settings from remote source
        testSettingsReader.put(new SettingsMockupBuilder().withFlags(TracingMode.ALWAYS).withSampleRate(1000000).build());
        //set tracing mode to NEVER;
        ConfigManager.setConfig(ConfigProperty.AGENT_TRACING_MODE, TracingMode.NEVER);
        
        //NEVER mode, should never be traced
        servlet.service(requestWithNoHeader, getResponse());
        assertEvents(Collections.<ExpectedEvent>emptyList());
        resetReporter();
        
        //NEVER mode, should never be traced
        servlet.service(requestWithXTrace, getResponse());
        assertEvents(Collections.<ExpectedEvent>emptyList());
        resetReporter();
        
        //NEVER mode, should never be traced
        servlet.service(requestWithXTraceIncompatible, getResponse());
        assertEvents(Collections.<ExpectedEvent>emptyList());
        resetReporter();
        
        //NEVER mode, should never be traced
        servlet.service(requestWithXTraceNotSampled, getResponse());
        assertEvents(Collections.<ExpectedEvent>emptyList()); 
        resetReporter();
        
        //no metrics for never mode
        Map<MetricKey, Histogram> histograms = MetricHistogramSpanReporter.REPORTER.consumeHistograms();
        assertEquals(0, histograms.size());
        
        Map<MetricKey, SummaryLongMeasurement> measurements = InboundMetricMeasurementSpanReporter.REPORTER.consumeMeasurements();
        assertEquals(0, measurements.size());
    }
    
    /**
     * Ensure no span remains after instrumentation
     * @throws Exception
     */
    public void testContext() throws Exception {
    	ExecutorService service = Executors.newFixedThreadPool(3);
    	
    	
    	//use a default settings from remote source
    	testSettingsReader.put(new SettingsMockupBuilder().withFlags(TracingMode.ALWAYS).withSampleRate(1000000).withSettingsArg(SettingsArg.BUCKET_CAPACITY, 1000.0).withSettingsArg(SettingsArg.BUCKET_RATE, 1000.0).build());
        //set tracing mode to ALWAYS;
        ConfigManager.setConfig(ConfigProperty.AGENT_TRACING_MODE, TracingMode.ALWAYS);
        
        List<Future<Object>> futures = new ArrayList<Future<Object>>();
        
        
        
        //test on request with no header, trace mode ALWAYS
        for (int i = 0 ; i < 300; i++) {
    		futures.add(service.submit(new Callable<Object>() {
    			public Object call() throws ServletException, IOException {
    				//ensure no dangling span from previous processing
    				assertNull(ScopeManager.INSTANCE.active());
    				 //request with no header, should be traced
    	    		servlet.service(requestWithNoHeader, getResponse());
    	    		//ensure no dangling span from this processing
    				assertNull(ScopeManager.INSTANCE.active());
    	    		
    	            assertEvents(expectedNewEvents);
    	            resetReporter();
    	            return null;
    			}
    		}));
    	}
        
        for (Future<Object> future : futures) {
        	future.get();
        }
        
        //test on request with x-trace header, trace mode ALWAYS
        for (int i = 0 ; i < 300; i++) {
    		futures.add(service.submit(new Callable<Object>() {
    			public Object call() throws ServletException, IOException {
    				//ensure no dangling span from previous processing
    			    assertNull(ScopeManager.INSTANCE.active());
    				 //request with no header, should be traced
    	    		servlet.service(requestWithXTrace, getResponse());
    	    		//ensure no dngaling span from this processing
    	    		assertNull(ScopeManager.INSTANCE.active());
    	    		
    	    		assertEvents(expectedContinueEvents);
    	            resetReporter();
    	            return null;
    			}
    		}));
    	}
        for (Future<Object> future : futures) {
        	future.get();
        }
        
        //set tracing mode to NEVER;
        TraceDecisionUtil.reset();
        ConfigManager.setConfig(ConfigProperty.AGENT_TRACING_MODE, TracingMode.NEVER);
        //test on request with no header, trace mode NEVER
        for (int i = 0 ; i < 300; i++) {
    		futures.add(service.submit(new Callable<Object>() {
    			public Object call() throws ServletException, IOException {
    				//ensure no dangling span from previous processing
    			    assertNull(ScopeManager.INSTANCE.active());
    				 //request with no header, should be traced
    	    		servlet.service(requestWithNoHeader, getResponse());
    	    		//ensure no dngaling span from this processing
    	    		assertNull(ScopeManager.INSTANCE.active());
    	    		
    	            assertEvents(Collections.<ExpectedEvent>emptyList());
    	            resetReporter();
    	            return null;
    			}
    		}));
    	}
        for (Future<Object> future : futures) {
        	future.get();
        }
    }
    
    private static class EdgeValidator implements ValueValidator<Object> {
    	private final List<String> expectedEdges = new ArrayList<String>();
    	private EdgeValidator(List<String> xTraceIds) throws OboeException {
    		for (String xTraceId : xTraceIds) {
    			expectedEdges.add(new Metadata(xTraceId).opHexString());
    		}
    	}

		public String getValueString() {
			return expectedEdges.toString();
		}

		public boolean isValid(Object actualEdgesObject) { 
			List<String> actualEdges;
			if (actualEdgesObject instanceof String) { //if it's size 1, it appears as String instead of a list
				actualEdges = Collections.singletonList((String) actualEdgesObject);
			} else {
				actualEdges = (List<String>) actualEdgesObject;
			}
			
			return expectedEdges.equals(actualEdges);
		}
    	
    }
    
    private void assertSpanMetrics(long countOnServiceName) {
        MetricKey metricKey;
        
        Map<MetricKey, Histogram> histograms = MetricHistogramSpanReporter.REPORTER.consumeHistograms();
        //service level histogram
        metricKey = new MetricKey(InboundMetricMeasurementSpanReporter.TRANSACTION_LATENCY_METRIC_NAME, null);
        assertNotNull(histograms.get(metricKey));
        assertEquals(countOnServiceName, histograms.get(metricKey).getTotalCount());
        
        //transaction level histogram
        metricKey = new MetricKey(InboundMetricMeasurementSpanReporter.TRANSACTION_LATENCY_METRIC_NAME, Collections.singletonMap(MetricMeasurementSpanReporter.TRANSACTION_NAME_TAG_KEY, "/test/1"));
        assertNotNull(histograms.get(metricKey));
        assertEquals(countOnServiceName, histograms.get(metricKey).getTotalCount());
        
        
        Map<MetricKey, SummaryLongMeasurement> measurements = InboundMetricMeasurementSpanReporter.REPORTER.consumeMeasurements();
        
        //transaction level measurement
        Map<String, String> tags;
        tags = new HashMap<String, String>();
        tags.put(MetricMeasurementSpanReporter.TRANSACTION_NAME_TAG_KEY, "/test/1");
        tags.put("HttpStatus", String.valueOf(MOCKED_STATUS_CODE));
        metricKey = new MetricKey(InboundMetricMeasurementSpanReporter.TRANSACTION_LATENCY_METRIC_NAME, tags);
        assertNotNull(measurements.get(metricKey));
        assertEquals(countOnServiceName, measurements.get(metricKey).getCount());
        
        tags = new HashMap<String, String>();
        tags.put(MetricMeasurementSpanReporter.TRANSACTION_NAME_TAG_KEY, "/test/1");
        tags.put("HttpMethod", MOCKED_METHOD);
        metricKey = new MetricKey(InboundMetricMeasurementSpanReporter.TRANSACTION_LATENCY_METRIC_NAME, tags);
        assertNotNull(measurements.get(metricKey));
        assertEquals(countOnServiceName, measurements.get(metricKey).getCount());
    }

    @Override
    protected void resetReporter() {
        threadLocalReporter.reset();
    }
    
    @Override
    protected void assertEvents(List<ExpectedEvent> expectedEvents) {
        List<DeserializedEvent> sentEvents = threadLocalReporter.getSentEvents();
        assertEvents(expectedEvents, sentEvents);
    }

    private static String getXTraceId(int version, boolean sampled) {
        Metadata metadata = new Metadata();
        metadata.randomize(sampled);
        return metadata.toHexString(version);
    }
   
}
