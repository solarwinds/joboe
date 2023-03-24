package com.tracelytics.joboe.config;

import com.tracelytics.joboe.SampleRateSource;
import com.tracelytics.joboe.TraceConfig;
import junit.framework.TestCase;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TraceConfigsTest extends TestCase {
    private final ResourceMatcher resourceMatcherStub = url -> true;

    private final TraceConfig testConfig = new TraceConfig(0, SampleRateSource.DEFAULT, (short) 0);

    class EntryStub implements Map.Entry<ResourceMatcher, TraceConfig> {
        int count = 0;

        @Override
        public ResourceMatcher getKey() {
            count++;
            return resourceMatcherStub;
        }

        @Override
        public TraceConfig getValue() {
            return testConfig;
        }

        @Override
        public TraceConfig setValue(TraceConfig value) {
            return value;
        }
    }

    private final EntryStub entryStub = new EntryStub();

    private final Set<Map.Entry<ResourceMatcher, TraceConfig>> setStub = new HashSet<java.util.Map.Entry<ResourceMatcher,
            TraceConfig>>() {{
        add(entryStub);
    }};

    private final HashMap<ResourceMatcher, TraceConfig> mapStub = new HashMap<ResourceMatcher, TraceConfig>() {

        {
            put(resourceMatcherStub, testConfig);
        }

        @Override
        public Set<Entry<ResourceMatcher, TraceConfig>> entrySet() {
            return setStub;
        }
    };

    private TraceConfigs tested;

    @Override
    protected void setUp() throws Exception {
        tested = new TraceConfigs(mapStub);
    }

    public void testVerifyThatSearchExitsOnFirstMatch() {
        tested.getTraceConfig(Arrays.asList("hello", "world"));
        assertEquals(1, entryStub.count);
    }
}