package com.tracelytics.joboe.rpc.thrift;

import com.appoptics.ext.org.apache.thrift.TException;
import com.appoptics.ext.org.apache.thrift.server.TServer;
import com.appoptics.ext.org.apache.thrift.server.TThreadPoolServer;
import com.appoptics.ext.org.apache.thrift.transport.TSSLTransportFactory;
import com.appoptics.ext.org.apache.thrift.transport.TSSLTransportFactory.TSSLTransportParameters;
import com.appoptics.ext.org.apache.thrift.transport.TServerTransport;
import com.appoptics.ext.org.apache.thrift.transport.TTransportFactory;
import com.appoptics.ext.org.apache.thrift.transport.TZlibTransport;
import com.appoptics.ext.thriftgenerated.Collector.Iface;
import com.appoptics.ext.thriftgenerated.Collector.Processor;
import com.appoptics.ext.thriftgenerated.*;
import com.tracelytics.joboe.rpc.ProtocolClientFactory;
import com.tracelytics.joboe.rpc.RpcClient;
import com.tracelytics.joboe.rpc.RpcClient.TaskType;
import com.tracelytics.joboe.rpc.RpcClientTest;
import com.tracelytics.joboe.rpc.Settings;
import com.tracelytics.joboe.settings.PollingSettingsFetcherTest;
import com.tracelytics.joboe.settings.SettingsArg;
import com.tracelytics.util.TimeUtils;

import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


//@RunWith(Parameterized.class)
public class ThriftClientTest extends RpcClientTest {
    private static final String TEST_SERVER_KEYSTORE_LOCATION = "src/test/java/com/tracelytics/joboe/rpc/thrift/test-collector-keystore.jks";
    private static final String TEST_SERVER_KEYSTORE_PASSWORD = "labrat1214";
    //private static final String TEST_SERVER_CERT_LOCATION = "src/test/java/com/tracelytics/joboe/rpc/thrift/test-collector-public.der";
    private static final List<OboeSetting> TEST_OBOE_SETTINGS = convertToOboeSettings(TEST_SETTINGS);


    private static List<OboeSetting> convertToOboeSettings(List<Settings> settings) {
        List<OboeSetting> oboeSettings = new ArrayList<OboeSetting>();

        for (Settings fromEntry : settings) {
            Map<String, ByteBuffer> arguments = new HashMap<String, ByteBuffer>();
            for (SettingsArg arg : SettingsArg.values()) {
                Object argValue = fromEntry.getArgValue(arg);
                if (argValue != null) {
                    arguments.put(arg.getKey(), arg.toByteBuffer(argValue));
                }
            }
            oboeSettings.add(new OboeSetting(OboeSettingType.findByValue(fromEntry.getType()), PollingSettingsFetcherTest.DEFAULT_FLAGS_STRING, TimeUtils.getTimestampMicroSeconds(), 1000000, "test-layer", arguments, 600));
        }
        return oboeSettings;
    }

    private static final RpcClient.RetryParamConstants QUICK_RETRY = new RpcClient.RetryParamConstants(100, 200, 3);

    private static class ThriftTestCollector implements TestCollector {
        private final ThriftHandler handler;
        private final TServer testServer;
        private ThriftTestCollector(int port, ThriftHandler handler) {
            this.handler = handler;
            this.testServer = startThriftCollectorServer(port, handler);
        }

        @Override
        public synchronized List<ByteBuffer> stop() {
            testServer.stop();
            try {
                while (testServer.isServing()) { //might have to wait for a while as the server is not stopped right the way
                   TimeUnit.SECONDS.sleep(1);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            List<ByteBuffer> events = flush();
            return events;
        }

        @Override
        public synchronized List<ByteBuffer> flush() {
            return handler.flush();
        }

        @Override
        public Map<TaskType, Long> getCallCountStats() {
            return handler.getCallCountStats();
        }
    }

    @Override
    protected TestCollector startCollector(int port) {
        return new ThriftTestCollector(port, new ThriftHandler());
    }

    @Override
    protected TestCollector startRedirectCollector(int port, String redirectArg) {
        return new ThriftTestCollector(port, new ThriftRedirectHandler(redirectArg));
    }

    @Override
    protected TestCollector startRatedCollector(int port, int processingTimePerMessage, com.tracelytics.joboe.rpc.ResultCode limitExceededCode) {
        return new ThriftTestCollector(port, new ThriftRatedHandler(processingTimePerMessage, ResultCode.valueOf(limitExceededCode.name())));
    }

    @Override
    protected TestCollector startBiasedTestCollector(int port, Map<TaskType, com.tracelytics.joboe.rpc.ResultCode> taskToResponseCode) {
        return new ThriftTestCollector(port, new ThriftBaisedHandler(Collections.singletonMap(TaskType.POST_METRICS, ResultCode.TRY_LATER)));
    }

    @Override
    protected TestCollector startErroneousTestCollector(int port, double errorPercentage) {
        return new ThriftTestCollector(port, new ThriftErroneousHandler(errorPercentage));
    }

    @Override
    protected TestCollector startSoftDisabledTestCollector(int port, String warning) throws IOException {
        return new ThriftTestCollector(port, new ThriftHandler(new ResultMessage(ResultCode.OK, "", warning)));
    }

    private static TServer startThriftCollectorServer(final int serverPort, final Iface handler) {
        final Processor<Iface> processor = new Processor(handler);

        try {
            //TServerSocket tServerSocket = new TServerSocket(TEST_SERVER_PORT);
            TSSLTransportParameters params = new TSSLTransportParameters();
            // The Keystore contains the private key
            
            params.setKeyStore(TEST_SERVER_KEYSTORE_LOCATION, TEST_SERVER_KEYSTORE_PASSWORD, null, null);

            /*
             * Use any of the TSSLTransportFactory to get a server transport with the appropriate
             * SSL configuration. You can use the default settings if properties are set in the command line.
             * Ex: -Djavax.net.ssl.keyStore=.keystore and -Djavax.net.ssl.keyStorePassword=thrift
             * 
             * Note: You need not explicitly call open(). The underlying server socket is bound on return
             * from the factory class. 
             */
            TServerTransport serverTransport = TSSLTransportFactory.getServerSocket(serverPort, 0, null, params);
            TTransportFactory tTransportFactory = new TZlibTransport.Factory();
            
            
            //final TServer server = new TSimpleServer(new TServer.Args(serverTransport).transportFactory(tTransportFactory).processor(processor));
            final TServer server = new TThreadPoolServer(new TThreadPoolServer.Args(serverTransport).transportFactory(tTransportFactory).processor(processor));
            

            new Thread("server-thread-port-" + serverPort) {
                public void run() {
                    System.out.println("Starting server on port " + serverPort + " handler class: " + handler.getClass().getName());
                    server.serve();
                    System.out.println("Stopping server on port " + serverPort);
                }
            }.start();

            //wait until the server is start up. Otherwise there could be concurrent server start that races for the same port
            while (!server.isServing()) {
                try {
                    TimeUnit.MILLISECONDS.sleep(10);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            System.out.println("Test server on " + serverPort + " is ready!");
            
            return server;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected ProtocolClientFactory<?> getProtocolClientFactory(URL certUrl) throws IOException, GeneralSecurityException {
        return new ThriftClient.ThriftProtocolClientFactory(certUrl, null);
    }

    private static class ThriftHandler implements Iface{
        private static final ResultMessage DEFAULT_MESSAGE = new ResultMessage(ResultCode.OK, "", "");
        private final ResultMessage resultMessage;
        
        protected List<ByteBuffer> buffer = new ArrayList<ByteBuffer>();
        private Map<TaskType, Long> callCountStats = new HashMap<TaskType, Long>();

        private ThriftHandler() {
            this(DEFAULT_MESSAGE);
        }

        /**
         * Provides the result message - take note that this does NOT affect ping, which always return the DEFAULT_MESSAGE
         * @param resultMessage
         */
        public ThriftHandler(ResultMessage resultMessage) {
            this.resultMessage = resultMessage;
        }

        public List<ByteBuffer> flush() {
            List<ByteBuffer> messages = new ArrayList<ByteBuffer>();
            
            for (ByteBuffer messageByteBuffer : buffer) {
                messages.add(messageByteBuffer);
            }
            
            buffer.clear();

            return messages;
        }

        public ResultMessage postEvents(String apiKey, List<ByteBuffer> messages, EncodingType enc, HostID hostId)
            throws TException {
            buffer.addAll(messages);
            incrementCallCountStats(TaskType.POST_EVENTS);
            return resultMessage;
        }

        public ResultMessage postMetrics(String apiKey, List<ByteBuffer> messages, EncodingType enc, HostID hostId)
            throws TException {
            buffer.addAll(messages);
            incrementCallCountStats(TaskType.POST_METRICS);
            return resultMessage;
        }

        public ResultMessage postStatus(String apiKey, List<ByteBuffer> messages, EncodingType enc, HostID hostId)
            throws TException {
            buffer.addAll(messages);
            incrementCallCountStats(TaskType.POST_STATUS);
            return resultMessage;
        }

        public SettingsResult getSettings(String apiKey, HostID identity, String tracelyzerVersion)
            throws TException {
            incrementCallCountStats(TaskType.GET_SETTINGS);
            return new SettingsResult(ResultCode.OK, resultMessage.getArg(), TEST_OBOE_SETTINGS,  resultMessage.getWarning());
        }


        public ResultMessage ping(String apiKey) throws TException {
            return DEFAULT_MESSAGE; //ping always return the same message - no warning
        }

        private void incrementCallCountStats(TaskType taskType) {
            Long existingCount = callCountStats.get(taskType);
            if (existingCount == null) {
                existingCount = 0L;
            }
            callCountStats.put(taskType, ++ existingCount);
        }

        public Map<TaskType, Long> getCallCountStats() {
            return callCountStats;
        }
    }
    
    private static class ThriftBaisedHandler extends ThriftHandler {
        private final Map<TaskType, ResultCode> resultCodeByTaskType;

        public ThriftBaisedHandler(Map<TaskType, ResultCode> resultCodeByTaskType) {
            super();
            this.resultCodeByTaskType = resultCodeByTaskType;
        }
                
        @Override
        public ResultMessage postEvents(String apiKey, List<ByteBuffer> messages, EncodingType enc, HostID hostId) throws TException {
            if (resultCodeByTaskType.containsKey(TaskType.POST_EVENTS)) {
                return new ResultMessage(resultCodeByTaskType.get(TaskType.POST_EVENTS), "", "");
            }
            return super.postEvents(apiKey, messages, enc, hostId);
        }
        
        @Override
        public ResultMessage postMetrics(String apiKey, List<ByteBuffer> messages, EncodingType enc, HostID hostId) throws TException {
            if (resultCodeByTaskType.containsKey(TaskType.POST_METRICS)) {
                return new ResultMessage(resultCodeByTaskType.get(TaskType.POST_METRICS), "", "");
            }
            return super.postMetrics(apiKey, messages, enc, hostId);
        }
        
        @Override
        public ResultMessage postStatus(String apiKey, List<ByteBuffer> messages, EncodingType enc, HostID hostId) throws TException {
            if (resultCodeByTaskType.containsKey(TaskType.POST_STATUS)) {
                return new ResultMessage(resultCodeByTaskType.get(TaskType.POST_STATUS), "", "");
            }
            return super.postStatus(apiKey, messages, enc, hostId);
        }
        
        @Override
        public SettingsResult getSettings(String apiKey, HostID identity, String tracelyzerVersion) throws TException {
            if (resultCodeByTaskType.containsKey(TaskType.GET_SETTINGS)) {
                return new SettingsResult(resultCodeByTaskType.get(TaskType.GET_SETTINGS), "", TEST_OBOE_SETTINGS, "");
            }
            return super.getSettings(apiKey, identity, tracelyzerVersion);
        }
        
    }
    
    
    
    private static class ThriftRedirectHandler extends ThriftHandler {
        private String redirectArg;
        
        public ThriftRedirectHandler(String redirectArg) {
            super();
            this.redirectArg = redirectArg;
        }

        public ResultMessage postEvents(String apiKey, List<ByteBuffer> messages, EncodingType enc, HostID hostId)
            throws TException {
            return new ResultMessage(ResultCode.REDIRECT, redirectArg, "");
        }

        public ResultMessage postMetrics(String apiKey, List<ByteBuffer> messages, EncodingType enc, HostID hostId)
            throws TException {
            return new ResultMessage(ResultCode.REDIRECT, redirectArg, "");
        }

        public ResultMessage postStatus(String apiKey, List<ByteBuffer> messages, EncodingType enc, HostID hostId)
            throws TException {
            return new ResultMessage(ResultCode.REDIRECT, redirectArg, "");
        }
        
        public void setRedirectArg(String redirectArg) {
            this.redirectArg = redirectArg;
        }

        public SettingsResult getSettings(String apiKey, HostID identity, String tracelyzerVersion)
            throws TException {
            return new SettingsResult(ResultCode.REDIRECT, redirectArg, null, "");
        }

        public ResultMessage ping(String apiKey) throws TException {
            return new ResultMessage(ResultCode.REDIRECT, redirectArg, "");
        }


        
    }
    
    private static class ThriftRatedHandler extends ThriftHandler {
        private int processingSpeedPerMessage;
        private ResultCode fullResultCode;
        private AtomicBoolean isProcessingAtomic = new AtomicBoolean(false);

        private ThriftRatedHandler(int processingTimePerMessage, ResultCode fullResultCode) {
            this.processingSpeedPerMessage = processingTimePerMessage;
            this.fullResultCode = fullResultCode;
        }
        
        public ResultMessage postEvents(String apiKey, List<ByteBuffer> messages, EncodingType enc)
            throws TException {
            return processMessage(apiKey, messages, enc);
        }

        public ResultMessage postMetrics(String apiKey, List<ByteBuffer> messages, EncodingType enc)
            throws TException {
            return processMessage(apiKey, messages, enc);
        }

        public ResultMessage postStatus(String apiKey, List<ByteBuffer> messages, EncodingType enc)
            throws TException {
            return processMessage(apiKey, messages, enc);
        }

        public SettingsResult getSettings(String apiKey, HostID identity, String tracelyzerVersion)
            throws TException {
            // TODO Auto-generated method stub
            return null;
        }
        
        private ResultMessage processMessage(String apiKey, final List<ByteBuffer> messages, EncodingType enc) {
            boolean alreadyProcessing = isProcessingAtomic.getAndSet(true);
            if (alreadyProcessing) {
                return new ResultMessage(fullResultCode, "", "");
            } else {
                buffer.addAll(messages);
                
                new Thread() { //since the server is blocking, we have to return the result but delay setting back the isProcessingAtomic flag to indicate that it's busy
                    public void run() {
                        try {
                            Thread.sleep(messages.size() * processingSpeedPerMessage);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        isProcessingAtomic.set(false);
                    }
                }.start();
                
                return new ResultMessage(ResultCode.OK, "", "");
            } 
        }

        public ResultMessage ping(String apiKey) throws TException {
            return processMessage(apiKey, Collections.EMPTY_LIST, null);
        }
    }

    private static class ThriftErroneousHandler extends ThriftHandler {
        private final ErrorState errorState;

        public ThriftErroneousHandler(double errorPercentage) {
            super();
            this.errorState = new ErrorState(errorPercentage);
        }

        @Override
        public ResultMessage postEvents(String apiKey, List<ByteBuffer> messages, EncodingType enc, HostID hostId) throws TException {
            checkError();
            return super.postEvents(apiKey, messages, enc, hostId);
        }

        @Override
        public ResultMessage postMetrics(String apiKey, List<ByteBuffer> messages, EncodingType enc, HostID hostId) throws TException {
            checkError();
            return super.postMetrics(apiKey, messages, enc, hostId);
        }

        @Override
        public ResultMessage postStatus(String apiKey, List<ByteBuffer> messages, EncodingType enc, HostID hostId) throws TException {
            checkError();
            return super.postStatus(apiKey, messages, enc, hostId);
        }

        @Override
        public SettingsResult getSettings(String apiKey, HostID identity, String tracelyzerVersion) throws TException {
            checkError();
            return super.getSettings(apiKey, identity, tracelyzerVersion);
        }

        private void checkError() throws RuntimeException {
            if (errorState.isNextAsError()) {
                throw new RuntimeException("Test exception from erroneous server");
            }
        }
    }


}
