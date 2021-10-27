package com.tracelytics.joboe;

import com.tracelytics.util.TestUtils;
import com.tracelytics.joboe.settings.SettingsArg;
import com.tracelytics.joboe.settings.SettingsManager;
import com.tracelytics.joboe.settings.SimpleSettingsFetcher;
import com.tracelytics.joboe.settings.TestSettingsReader;
import com.tracelytics.joboe.settings.TestSettingsReader.SettingsMockupBuilder;
import junit.framework.TestCase;

/** Tests for Metadata */
public class MetadataTest extends TestCase {

    private final TestSettingsReader testSettingsReader = TestUtils.initSettingsReader();

    public void testHexEncode()
        throws Exception {
        // Make sure we can encode and decode hex strings
        Metadata md1 = new Metadata();
        md1.randomize();
        
        String hex1 = md1.toHexString();
        
        Metadata md2 = new Metadata();
        md2.fromHexString(hex1);

        assertEquals(md1, md2);
    }
    
    public void testRandomization() {

        // Make sure IDs are unique:
        Metadata md1 = new Metadata();
        md1.randomize();
       
        Metadata md2 = new Metadata();
        md2.randomize();
        
        assertFalse(md1.toHexString().equals(md2.toHexString()));
    
        String hex2 = md2.toHexString();
        
        md2.randomizeOpID();
        assertFalse(md2.toHexString().equals(hex2));
        
        Metadata md3 = new Metadata(md2);
        assertEquals(md2, md3);

        // Make sure flag is set properly
        Metadata md4 = new Metadata();
        md4.randomize(true);
        assertTrue(md4.isSampled());
        
        Metadata md5 = new Metadata();
        md5.randomize(false);
        assertFalse(md5.isSampled());
    }
    
    public void testCompatibility() {
        //should not accept trace id from different version
        String v1Id = "01-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01";
        String v0Id = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01";

        assertFalse(Metadata.isCompatible(v1Id));
        assertTrue(Metadata.isCompatible(v0Id));
    }
    
    public void testSampled() throws OboeException {
        String sampledId = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01";
        String notSampledId = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-00";
        
        assertTrue(new Metadata(sampledId).isSampled());
        assertFalse(new Metadata(notSampledId).isSampled());
        
        Metadata md = new Metadata();
        md.setSampled(true);
        assertTrue(md.isSampled());
        md.setSampled(false);
        assertFalse(md.isSampled());
    }
    
    public void testInit() {

        // Test initialization
        Metadata md = new Metadata();
        assertFalse(md.isValid());
        
        md.randomizeOpID();
        assertFalse(md.isValid());
        
        md.randomizeTaskID();
        assertTrue(md.isValid());
    }
    
    public void testTtlChange() {
        assertEquals(Metadata.DEFAULT_TTL, Metadata.getTtl());
        
        int newTtl = 10;
        SimpleSettingsFetcher fetcher = (SimpleSettingsFetcher) SettingsManager.getFetcher();
        testSettingsReader.put(new SettingsMockupBuilder().withFlags(TracingMode.ALWAYS).withSampleRate(TraceDecisionUtil.SAMPLE_RESOLUTION).withSettingsArg(SettingsArg.MAX_CONTEXT_AGE, newTtl).build());
        
        assertEquals(newTtl * 1000, Metadata.getTtl());  //sec to millisec
        
        //revert to default
        testSettingsReader.put(new SettingsMockupBuilder().withFlags(TracingMode.ALWAYS).withSampleRate(TraceDecisionUtil.SAMPLE_RESOLUTION).build());
        
        assertEquals(Metadata.DEFAULT_TTL, Metadata.getTtl());
    }
    
    public void testMaxEventsChange() {
        assertEquals(Metadata.DEFAULT_MAX_EVENTS, Metadata.getMaxEvents());
        
        int newMaxEvents = 100;
        SimpleSettingsFetcher fetcher = (SimpleSettingsFetcher) SettingsManager.getFetcher();
        testSettingsReader.put(new SettingsMockupBuilder().withFlags(TracingMode.ALWAYS).withSampleRate(TraceDecisionUtil.SAMPLE_RESOLUTION).withSettingsArg(SettingsArg.MAX_CONTEXT_EVENTS, newMaxEvents).build());
        
        assertEquals(newMaxEvents, Metadata.getMaxEvents());
        
        //revert to default
        testSettingsReader.put(new SettingsMockupBuilder().withFlags(TracingMode.ALWAYS).withSampleRate(TraceDecisionUtil.SAMPLE_RESOLUTION).build());
        
        assertEquals(Metadata.DEFAULT_MAX_EVENTS, Metadata.getMaxEvents());
    }
    
    
    public void testMaxBacktracesChange() {
        assertEquals(Metadata.DEFAULT_MAX_BACKTRACES, Metadata.getMaxBacktraces());
        
        int newMaxBacktraces = 100;
        SimpleSettingsFetcher fetcher = (SimpleSettingsFetcher) SettingsManager.getFetcher();
        testSettingsReader.put(new SettingsMockupBuilder().withFlags(TracingMode.ALWAYS).withSampleRate(TraceDecisionUtil.SAMPLE_RESOLUTION).withSettingsArg(SettingsArg.MAX_CONTEXT_BACKTRACES, newMaxBacktraces).build());
        
        assertEquals(newMaxBacktraces, Metadata.getMaxBacktraces());
        
        //revert to default
        testSettingsReader.put(new SettingsMockupBuilder().withFlags(TracingMode.ALWAYS).withSampleRate(TraceDecisionUtil.SAMPLE_RESOLUTION).build());
        
        assertEquals(Metadata.DEFAULT_MAX_BACKTRACES, Metadata.getMaxBacktraces());
    }
    
    public static String getXTraceid(int version, boolean sampled) {
        Metadata metadata = new Metadata();
        metadata.randomize(sampled);
        return metadata.toHexString(version);
    }
}
