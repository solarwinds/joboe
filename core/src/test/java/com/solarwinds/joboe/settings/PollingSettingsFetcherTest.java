package com.solarwinds.joboe.settings;

import com.solarwinds.joboe.Event;
import com.solarwinds.joboe.TraceDecisionUtil;
import com.solarwinds.joboe.rpc.*;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;


public class PollingSettingsFetcherTest {
    private static final int SAMPLE_RATE_FOR_DEFAULT_LAYER = 400000;
    private static final List<Settings> MOCK_SETTINGS = new ArrayList<Settings>();
    public static final short DEFAULT_FLAGS =
            Settings.OBOE_SETTINGS_FLAG_TRIGGER_TRACE_ENABLED |
            Settings.OBOE_SETTINGS_FLAG_SAMPLE_START |
            Settings.OBOE_SETTINGS_FLAG_SAMPLE_THROUGH |
            Settings.OBOE_SETTINGS_FLAG_SAMPLE_THROUGH_ALWAYS;
    
    public static final String DEFAULT_FLAGS_STRING = "TRIGGER_TRACE,SAMPLE_THROUGH_ALWAYS,SAMPLE_THROUGH,SAMPLE_START";
    
    private static final Map<String, ByteBuffer> ARGS = new HashMap<String, ByteBuffer>();
    private static final double BUCKET_RATE = 2;
    private static final double BUCKET_CAPACITY = 32;
    private static final int REFRESH_INTERVAL = 1;
    private static final int TTL = 5; //set TTL > READER_REFRESH_INTERVAL for expired settings test
    
    static {
//        MOCK_LAYER_SETTINGS.put("tomcat", 100000);
//        MOCK_LAYER_SETTINGS.put("jboss", 200000);
//        MOCK_LAYER_SETTINGS.put("java", 300000);
        
        ByteBuffer buffer;
        buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putDouble(BUCKET_CAPACITY);
        buffer.rewind();
        ARGS.put(SettingsArg.BUCKET_CAPACITY.getKey(), buffer);
        
        buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putDouble(BUCKET_RATE);
        buffer.rewind();
        ARGS.put(SettingsArg.BUCKET_RATE.getKey(), buffer);
        
//        for (Entry<String, Integer> layerSampleRate : MOCK_LAYER_SETTINGS.entrySet()) {
//            Map<String, ByteBuffer> args = new HashMap<String, ByteBuffer>();
//            for (Entry<String, ByteBuffer> arg : ARGS.entrySet()) {
//                args.put(arg.getKey(), arg.getValue().duplicate()); //duplicate share the same content...but we are read only...as far as they dun share the same pointer we are okay
//            }
//            //need to create deep clone for each buffer args
//            MOCK_SETTINGS.add(new com.tracelytics.joboe.rpc.Settings(Settings.OBOE_SETTINGS_TYPE_LAYER_SAMPLE_RATE, DEFAULT_FLAGS_STRING, Agent.currentTimeStamp(), layerSampleRate.getValue(), TTL, layerSampleRate.getKey(), args));
//        }
        
        MOCK_SETTINGS.add(new com.solarwinds.joboe.rpc.Settings(Settings.OBOE_SETTINGS_TYPE_DEFAULT_SAMPLE_RATE, DEFAULT_FLAGS_STRING, System.currentTimeMillis(), SAMPLE_RATE_FOR_DEFAULT_LAYER, TTL, "", ARGS));
    }



    @Test
    public void testSettings()
        throws Exception {
        Client client = new MockRpcClient(MOCK_SETTINGS);
        SettingsFetcher fetcher = getFetcher(client);
        
        Settings settings = null;
        
        // Test some values what we know are stored in the above file:
        settings = fetcher.getSettings();
        assertEquals(SAMPLE_RATE_FOR_DEFAULT_LAYER, (int)settings.getValue());
        assertEquals(DEFAULT_FLAGS, settings.getFlags());

//        settings = fetcher.getLayerSampleRate("jboss");
//        assertEquals(200000, (int)settings.getValue());
//        assertEquals(DEFAULT_FLAGS, settings.getFlags());
//
//        settings = fetcher.getLayerSampleRate("java");
//        assertEquals(300000, (int)settings.getValue());
//        assertEquals(DEFAULT_FLAGS, settings.getFlags());
//
//        // This should use the default:
//        settings = fetcher.getLayerSampleRate("this_does_not_exist");
//        assertEquals(400000, (int)settings.getValue());
//        assertEquals(DEFAULT_FLAGS, settings.getFlags());

        
        //old settings should not return bucket capacity nor rate
        assertEquals(BUCKET_CAPACITY, settings.getArgValue(SettingsArg.BUCKET_CAPACITY));
        assertEquals(BUCKET_RATE, settings.getArgValue(SettingsArg.BUCKET_RATE));
        
        fetcher.close();
    }

    @Test
    public void testSettingsCache() throws Exception {
        Client client = new OneHitWonderClient(MOCK_SETTINGS);
        SettingsFetcher fetcher = getFetcher(client);
        
        Settings settings = fetcher.getSettings(); //first hit should be okay
        assertEquals(SAMPLE_RATE_FOR_DEFAULT_LAYER, (int)settings.getValue());

        
        settings = fetcher.getSettings(); //second hit
        assertEquals(SAMPLE_RATE_FOR_DEFAULT_LAYER, (int)settings.getValue());
        
        fetcher.close();
    }

    @Test
    public void testSettingsCacheExpired() throws Exception {
        Client client = new OneHitWonderClient(MOCK_SETTINGS);
        SettingsFetcher fetcher = getFetcher(client);
        
        Settings settings = fetcher.getSettings(); //cached Settings not yet expired 
        assertEquals(SAMPLE_RATE_FOR_DEFAULT_LAYER, (int)settings.getValue());
        
        int wait = TTL - 1; //wait shorter than TTL so settings should not yet expire on dead client
        
        TimeUnit.SECONDS.sleep(wait);
        
        settings = fetcher.getSettings();  //cached Settings not yet expired
        assertEquals(SAMPLE_RATE_FOR_DEFAULT_LAYER, (int)settings.getValue());
        
        TimeUnit.SECONDS.sleep(3); //sleep for 3 more seconds, now it's TTL + 2 (and 2 is bigger than REFRESH_INTERVAL), hence the Settings should be expired
        System.out.println("waking up");
        settings = fetcher.getSettings(); //second hit after record expired, should return null
        assertNull(settings);
        
        fetcher.close();
    }

    @Test
    public void testInvalidArgs() throws Exception {
        SettingsFetcher fetcher;
        Settings settings;
        com.solarwinds.joboe.rpc.Settings sourceSettings;
        
        //test remote settings that give empty map for args
        sourceSettings = new com.solarwinds.joboe.rpc.Settings(Settings.OBOE_SETTINGS_TYPE_LAYER_SAMPLE_RATE, DEFAULT_FLAGS_STRING, System.currentTimeMillis(), 100000, TTL, "", Collections.emptyMap());
        Client client = new MockRpcClient(Collections.singletonList(sourceSettings));
        fetcher = getFetcher(client);
        settings = fetcher.getSettings();
        assertEquals(100000, (int)settings.getValue());
        assertEquals(DEFAULT_FLAGS, settings.getFlags());
        assertNull(settings.getArgValue(SettingsArg.BUCKET_CAPACITY));
        assertNull(settings.getArgValue(SettingsArg.BUCKET_RATE));
        
        //test remote settings that give empty values for args that triggers BufferUnderflow
        Map<String, ByteBuffer> args = new HashMap<String, ByteBuffer>();
        args.put(SettingsArg.BUCKET_CAPACITY.getKey(), ByteBuffer.allocate(0));
        args.put(SettingsArg.BUCKET_RATE.getKey(), ByteBuffer.allocate(0));
        args.put(SettingsArg.METRIC_FLUSH_INTERVAL.getKey(), ByteBuffer.allocate(0));
        sourceSettings = new com.solarwinds.joboe.rpc.Settings(Settings.OBOE_SETTINGS_TYPE_LAYER_SAMPLE_RATE, DEFAULT_FLAGS_STRING, System.currentTimeMillis(), 100000, TTL, "", args);
        fetcher = getFetcher(new MockRpcClient(Collections.singletonList(sourceSettings)));
        settings = fetcher.getSettings();
        assertEquals(100000, (int)settings.getValue());
        assertEquals(DEFAULT_FLAGS, settings.getFlags());
        assertNull(settings.getArgValue(SettingsArg.BUCKET_CAPACITY));
        assertNull(settings.getArgValue(SettingsArg.BUCKET_RATE));
        assertNull(settings.getArgValue(SettingsArg.METRIC_FLUSH_INTERVAL));
        
        //test remote settings that give valid values for args
        args = new HashMap<String, ByteBuffer>();
        ByteBuffer buffer;
        buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putDouble(1);
        buffer.rewind();
        args.put(SettingsArg.BUCKET_CAPACITY.getKey(), buffer);
        buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putDouble(2);
        buffer.rewind();
        args.put(SettingsArg.BUCKET_RATE.getKey(), buffer);
        buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(3);
        buffer.rewind();
        args.put(SettingsArg.METRIC_FLUSH_INTERVAL.getKey(), buffer);
        sourceSettings = new com.solarwinds.joboe.rpc.Settings(Settings.OBOE_SETTINGS_TYPE_LAYER_SAMPLE_RATE, DEFAULT_FLAGS_STRING, System.currentTimeMillis(), 100000, TTL, "", args);
        fetcher = getFetcher(new MockRpcClient(Collections.singletonList(sourceSettings)));
        settings = fetcher.getSettings();
        assertEquals(100000, (int)settings.getValue());
        assertEquals(DEFAULT_FLAGS, settings.getFlags());
        assertEquals(1.0, settings.getArgValue(SettingsArg.BUCKET_CAPACITY));
        assertEquals(2.0, settings.getArgValue(SettingsArg.BUCKET_RATE));
        assertEquals((Integer) 3, settings.getArgValue(SettingsArg.METRIC_FLUSH_INTERVAL));
        
        fetcher.close();
    }

    @Test
    public void testInvalidSampleRate() throws Exception {
        SettingsFetcher fetcher;
        Settings settings;
        com.solarwinds.joboe.rpc.Settings sourceSettings;
        
        //test remote settings that gives sample rate that is greater than 1000000
        sourceSettings = new com.solarwinds.joboe.rpc.Settings(Settings.OBOE_SETTINGS_TYPE_LAYER_SAMPLE_RATE, DEFAULT_FLAGS_STRING, System.currentTimeMillis(), 1111111, TTL, "", ARGS);
        Client client = new MockRpcClient(Collections.singletonList(sourceSettings));
        fetcher = getFetcher(client);
        settings = fetcher.getSettings();
        assertEquals(TraceDecisionUtil.SAMPLE_RESOLUTION, (int)settings.getValue()); //should be adjusted to 1000000
        assertEquals(DEFAULT_FLAGS, settings.getFlags());
        assertEquals(BUCKET_CAPACITY, settings.getArgValue(SettingsArg.BUCKET_CAPACITY));
        assertEquals(BUCKET_RATE, settings.getArgValue(SettingsArg.BUCKET_RATE));
        
        //test remote settings that gives sample rate that is negative
        sourceSettings = new com.solarwinds.joboe.rpc.Settings(Settings.OBOE_SETTINGS_TYPE_LAYER_SAMPLE_RATE, DEFAULT_FLAGS_STRING, System.currentTimeMillis(), -1, TTL, "", ARGS);
        fetcher = getFetcher(new MockRpcClient(Collections.singletonList(sourceSettings)));
        settings = fetcher.getSettings();
        assertEquals(0, (int)settings.getValue()); //should be adjusted to 0
        assertEquals(DEFAULT_FLAGS, settings.getFlags());
        assertEquals(BUCKET_CAPACITY, settings.getArgValue(SettingsArg.BUCKET_CAPACITY));
        assertEquals(BUCKET_RATE, settings.getArgValue(SettingsArg.BUCKET_RATE));
        
        fetcher.close();
    }

    @Test
    public void testExecutionException()  throws Exception {
        Client client = new ExecutionExceptionClient();
        SettingsFetcher fetcher = getFetcher(client);
        
        Settings settings = null;
        
        settings = fetcher.getSettings();
        assertNull(settings);
                
        fetcher.close();
    }

    @Test
    public void testSettingsListener() throws InterruptedException {
        Settings settings;
        settings = new com.solarwinds.joboe.rpc.Settings(Settings.OBOE_SETTINGS_TYPE_LAYER_SAMPLE_RATE, DEFAULT_FLAGS_STRING, System.currentTimeMillis(), 1, TTL, "", ARGS);
        Client client = new MockRpcClient(Collections.singletonList(settings));
        
        SettingsFetcher fetcher = new PollingSettingsFetcher(new RpcSettingsReader(client), 1); //refresh every second
        
        TestSettingsListener testSettingsListener = new TestSettingsListener();
        fetcher.registerListener(testSettingsListener);
        
        TimeUnit.SECONDS.sleep(2); //should be enough time for an update
        
        assertNotNull(testSettingsListener.settingsRetrieved);
        
        fetcher.close();
    }
    
    private static class TestSettingsListener implements SettingsListener {
        private Settings settingsRetrieved;
        @Override
        public void onSettingsRetrieved(Settings newSettings) {
            settingsRetrieved = newSettings;
        }
    }
    
    
    
    
    private SettingsFetcher getFetcher(Client rpcClient) {
        RpcSettingsReader reader = new RpcSettingsReader(rpcClient);
        try {
            SettingsFetcher fetcher = new PollingSettingsFetcher(reader, REFRESH_INTERVAL);
            fetcher.isSettingsAvailableLatch().await(10, TimeUnit.SECONDS);
            return fetcher;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
            
        
    }

    private abstract static class TestClient implements Client {
        private final List<? extends Settings> settings;
        
        protected TestClient(List<? extends Settings> settings) {
            this.settings = settings;
        }
        
        @Override
        public final Future<Result> postEvents(List<Event> events, Callback<Result> callback) throws ClientException {
            return null; //not used
        }

        @Override
        public final Future<Result> postMetrics(List<Map<String, Object>> messages, Callback<Result> callback) throws ClientException {
            return null; //not used
        }

        @Override
        public final Future<Result> postStatus(List<Map<String, Object>> messages, Callback<Result> callback) throws ClientException {
            return null; //not used
        }
        
        /**
         * Clone the settings as the buffer within the original settings cannot be shared
         * @return
         */
        protected List<Settings> getClonedSettings() {
            List<Settings> clonedSettings = new ArrayList<Settings>();
            for (Settings settingsEntry : settings) {
                clonedSettings.add(new com.solarwinds.joboe.rpc.Settings((com.solarwinds.joboe.rpc.Settings) settingsEntry, System.currentTimeMillis())); //create clones with the current timestamp
            }
            
            return clonedSettings;  
        }
    }

    private static class MockRpcClient extends TestClient {
        private final ExecutorService service = Executors.newSingleThreadExecutor();
        
        private MockRpcClient(List<? extends Settings> settings) {
            super(settings);
        }

        @Override
        public Future<SettingsResult> getSettings(String version, Callback<SettingsResult> callback) throws ClientException {
            return service.submit(() -> {
                //need to create deep clone for each buffer args
                return new SettingsResult(ResultCode.OK, "", "", getClonedSettings());
            });
        }
        
        @Override
        public void close() {
            service.shutdown();
        }
        
        @Override
        public Status getStatus() {
            return Status.OK;
        }
    }
    
    private static class OneHitWonderClient extends TestClient {
        private boolean hasHit = false;
        private final ExecutorService service = Executors.newSingleThreadExecutor();

        protected OneHitWonderClient(List<Settings> settings) {
            super(settings);
        }

        @Override
        public Future<SettingsResult> getSettings(String version, Callback<SettingsResult> callback) {
            if (!hasHit) {
                hasHit = true; //hit once! next one should return exception
                return service.submit(() -> new SettingsResult(ResultCode.OK, "", "", getClonedSettings()));
            } else {
                return service.submit(() -> {
                    throw new RuntimeException("Testing exception");
                });
            }
        }

        @Override
        public void close() {
            service.shutdown();
        }
        
        @Override
        public Status getStatus() {
            return Status.OK;
        }
    }
    
    private static class ExecutionExceptionClient extends TestClient {
        private final ExecutorService service = Executors.newSingleThreadExecutor();

        protected ExecutionExceptionClient() {
            super(null);
        }

        @Override
        public Future<SettingsResult> getSettings(String version, Callback<SettingsResult> callback) {
            return getExecutionException();
        }
        
        private Future<SettingsResult> getExecutionException() { //could have used CompletableFuture but it's jdk 8...
           return service.submit(() -> {
               throw new Exception("test");
           });
        }

        @Override
        public void close() {
            service.shutdown();
        }
        
        @Override
        public Status getStatus() {
            return Status.OK;
        }
    }
    
    
    
}
