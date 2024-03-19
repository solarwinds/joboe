package com.solarwinds.joboe.sampling;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;


@ExtendWith(MockitoExtension.class)
public class MetadataTest {

    @Captor
    private ArgumentCaptor<SettingsArgChangeListener<Integer>> listenerArgumentCaptor;

    @Test
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

    @Test
    public void testRandomization() {

        // Make sure IDs are unique:
        Metadata md1 = new Metadata();
        md1.randomize();
       
        Metadata md2 = new Metadata();
        md2.randomize();

        assertNotEquals(md1.toHexString(), md2.toHexString());
    
        String hex2 = md2.toHexString();
        
        md2.randomizeOpID();
        assertNotEquals(md2.toHexString(), hex2);
        
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

    @Test
    public void testCompatibility() {
        //should not accept trace id from different version
        String v1Id = "01-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01";
        String v0Id = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01";

        assertFalse(Metadata.isCompatible(v1Id));
        assertTrue(Metadata.isCompatible(v0Id));
    }

    @Test
    public void testSampled() throws SamplingException {
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

    @Test
    public void testInit() {

        // Test initialization
        Metadata md = new Metadata();
        assertFalse(md.isValid());
        
        md.randomizeOpID();
        assertFalse(md.isValid());
        
        md.randomizeTaskID();
        assertTrue(md.isValid());
    }

    @Test
    public void testTtlChange() {
        assertEquals(Metadata.DEFAULT_TTL, Metadata.getTtl());
        MockedStatic<SettingsManager> settingsManagerMock = mockStatic(SettingsManager.class);
        settingsManagerMock.verify(() -> SettingsManager.registerListener(listenerArgumentCaptor.capture()));

        int newTtl = 10;
        listenerArgumentCaptor.getAllValues().forEach(integerSettingsArgChangeListener -> integerSettingsArgChangeListener.onChange(newTtl));
        assertEquals(newTtl * 1000, Metadata.getTtl());  //sec to millisec

        //revert to default
        listenerArgumentCaptor.getAllValues().forEach(integerSettingsArgChangeListener -> integerSettingsArgChangeListener.onChange(null));
        assertEquals(Metadata.DEFAULT_TTL, Metadata.getTtl());
        settingsManagerMock.close();
    }

    @Test
    public void testMaxEventsChange() {
        assertEquals(Metadata.DEFAULT_MAX_EVENTS, Metadata.getMaxEvents());
        MockedStatic<SettingsManager> settingsManagerMock = mockStatic(SettingsManager.class);
        settingsManagerMock.verify(() -> SettingsManager.registerListener(listenerArgumentCaptor.capture()));
        
        int newMaxEvents = 100;
        listenerArgumentCaptor.getAllValues().forEach(integerSettingsArgChangeListener -> integerSettingsArgChangeListener.onChange(newMaxEvents));
        assertEquals(newMaxEvents, Metadata.getMaxEvents());
        
        //revert to default
        listenerArgumentCaptor.getAllValues().forEach(integerSettingsArgChangeListener -> integerSettingsArgChangeListener.onChange(null));
        
        assertEquals(Metadata.DEFAULT_MAX_EVENTS, Metadata.getMaxEvents());
        settingsManagerMock.close();
    }


    @Test
    public void testMaxBacktracesChange() {
        assertEquals(Metadata.DEFAULT_MAX_BACKTRACES, Metadata.getMaxBacktraces());
        MockedStatic<SettingsManager> settingsManagerMock = mockStatic(SettingsManager.class);
        settingsManagerMock.verify(() -> SettingsManager.registerListener(listenerArgumentCaptor.capture()));
        
        int newMaxBacktraces = 100;
        listenerArgumentCaptor.getAllValues().forEach(integerSettingsArgChangeListener -> integerSettingsArgChangeListener.onChange(newMaxBacktraces));
        assertEquals(newMaxBacktraces, Metadata.getMaxBacktraces());
        
        //revert to default
        listenerArgumentCaptor.getAllValues().forEach(integerSettingsArgChangeListener -> integerSettingsArgChangeListener.onChange(null));
        assertEquals(Metadata.DEFAULT_MAX_BACKTRACES, Metadata.getMaxBacktraces());
        settingsManagerMock.close();
    }

    @Test
    public static String getXTraceid(int version, boolean sampled) {
        Metadata metadata = new Metadata();
        metadata.randomize(sampled);
        return metadata.toHexString(version);
    }
}
