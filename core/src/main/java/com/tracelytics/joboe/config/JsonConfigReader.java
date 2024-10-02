package com.tracelytics.joboe.config;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.tracelytics.ext.json.JSONException;
import com.tracelytics.ext.json.JSONObject;
import com.tracelytics.ext.json.JSONTokener;

/**
 * A Reader that reads the input config file in JSON
 * @author Patson Luk
 *
 */
public class JsonConfigReader extends ConfigReader {
    private final InputStream configStream;

    /**
     * 
     * @param configStream    The input stream of the configuration file
     */
    public JsonConfigReader(InputStream configStream) {
        super(ConfigSourceType.JSON_FILE);
        this.configStream = configStream;
    }
    
    /**
     * 
     */
    public void read(ConfigContainer container) throws InvalidConfigException {
        if (configStream == null) {
            throw new InvalidConfigException("Cannot find any valid configuration for agent");
        }

        List<InvalidConfigException> exceptions = new ArrayList<InvalidConfigException>();
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(new JSONTokener(configStream));

            for (Object keyAsObject : jsonObject.keySet()) {
                ConfigProperty key = ConfigProperty.fromConfigFileKey((String) keyAsObject); // attempt to retrieve the corresponding ConfigProperty as key

                if (key == null) {
                    exceptions.add(new InvalidConfigException("Invalid line in configuration file : key [" + keyAsObject + "] is invalid"));
                } else {
                    try {
                        Object value = jsonObject.get((String) keyAsObject);
                        if (value != null) {
                            container.putByStringValue(key, value.toString());
                        } else {
                            // should not be null since it is read from JSON file
                            exceptions.add(new InvalidConfigException(key, "Unexpected null value", null));
                        }
                    } catch (JSONException e) {
                        exceptions.add(new InvalidConfigException("Json exception while processing config for key [" + keyAsObject + "] : " + e.getMessage(), e));
                    }
                }
            }
        } catch (JSONException e) {
            exceptions.add(new InvalidConfigException("Json exception while processing config : " + e.getMessage(), e));
        }

        if (!exceptions.isEmpty()) {
            logger.warn("Found " + exceptions.size() + " exception(s) while reading config from config file");
            throw exceptions.get(0); //report the first exception encountered
        }
    }
}
