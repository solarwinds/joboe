package com.tracelytics.joboe.settings;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.tracelytics.joboe.HostId;
import com.tracelytics.joboe.rpc.Client;
import com.tracelytics.joboe.rpc.ClientLoggingCallback;
import com.tracelytics.joboe.rpc.ResultCode;
import com.tracelytics.joboe.rpc.SettingsResult;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;
import com.tracelytics.util.HostInfoUtils;

/**
 * Reads {@link Settings} from the source provided by the rpc {@link Client} specified during instantiation
 *
 */
public class RpcSettingsReader implements SettingsReader {
    private static final Logger logger = LoggerFactory.getLogger();
    
   
    private Client rpcClient;
    private ClientLoggingCallback<SettingsResult> loggingCallback = new ClientLoggingCallback<SettingsResult>("get service settings");

    private static final String SSL_CLIENT_VERSION = "2";
    
    /**
     * 
     * @param rpcClient
     * @param hostname
     * @param addresses
     * @param uuid
     *  
     */
    RpcSettingsReader(Client rpcClient) {
        this.rpcClient = rpcClient;
    }
    
    
    
    /* (non-Javadoc)
     * @see com.tracelytics.joboe.SettingsReader#getLayerSampleRate(java.lang.String)
     */
    public Map<String, Settings> getSettings() throws OboeSettingsException {
        SettingsResult result;
        try {
            result = rpcClient.getSettings(SSL_CLIENT_VERSION, loggingCallback).get();
        } catch (Exception e) {
            throw new OboeSettingsException("Exception from RPC call: " + e.getMessage(), e);
        }
        if (result.getResultCode() == ResultCode.OK) {
            Map<String, Settings> updatedSettings = new LinkedHashMap<String, Settings>();
            for (Settings settingsForLayer : result.getSettings()) {
                updatedSettings.put(settingsForLayer.getLayer(), settingsForLayer);
            }
            return updatedSettings;
        } else {
            throw new OboeSettingsException("Rpc call returns non OK status code: " + result.getResultCode());
        }
    }
    
    public void close() {
        if (rpcClient != null) {
            rpcClient.close();
        }
    }
}
