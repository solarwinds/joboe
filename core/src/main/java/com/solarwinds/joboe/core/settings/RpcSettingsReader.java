package com.solarwinds.joboe.core.settings;

import com.solarwinds.joboe.core.rpc.Client;
import com.solarwinds.joboe.core.rpc.ClientLoggingCallback;
import com.solarwinds.joboe.core.rpc.ResultCode;
import com.solarwinds.joboe.core.rpc.SettingsResult;
import com.solarwinds.joboe.core.logging.Logger;
import com.solarwinds.joboe.core.logging.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reads {@link Settings} from the source provided by the rpc {@link Client} specified during instantiation
 *
 */
public class RpcSettingsReader implements SettingsReader {
    private static final Logger logger = LoggerFactory.getLogger();


    private final Client rpcClient;
    private final ClientLoggingCallback<SettingsResult> loggingCallback = new ClientLoggingCallback<SettingsResult>("get service settings");

    private static final String SSL_CLIENT_VERSION = "2";

    /**
     *
     * @param rpcClient client to use for retrieving settings
     *
     */
    RpcSettingsReader(Client rpcClient) {
        this.rpcClient = rpcClient;
    }



    /* (non-Javadoc)
     * @see com.solarwinds.joboe.core.SettingsReader#getLayerSampleRate(java.lang.String)
     */
    @Override
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
                logger.debug("Got settings from collector: " + settingsForLayer);
                updatedSettings.put(settingsForLayer.getLayer(), settingsForLayer);
            }
            return updatedSettings;
        } else {
            throw new OboeSettingsException("Rpc call returns non OK status code: " + result.getResultCode());
        }
    }

    @Override
    public void close() {
        if (rpcClient != null) {
            rpcClient.close();
        }
    }
}
