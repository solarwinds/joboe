package com.tracelytics.agent.config;

import com.tracelytics.ext.json.JSONArray;
import com.tracelytics.ext.json.JSONException;
import com.tracelytics.instrumentation.Module;
import com.tracelytics.instrumentation.config.HideParamsConfig;
import com.tracelytics.joboe.config.ConfigParser;
import com.tracelytics.joboe.config.InvalidConfigException;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class HideParamsConfigParser implements ConfigParser<String, HideParamsConfig> {
    private static final Logger logger = LoggerFactory.getLogger();

    public HideParamsConfig convert(String stringValue) throws InvalidConfigException {
        Set<Module> hideParamsModules = new HashSet<Module>();
        try {
            JSONArray hideParamsArray = new JSONArray(stringValue);

            for (int i = 0 ; i < hideParamsArray.length(); i++) {
                String hideParamsModule = (String) hideParamsArray.get(i);

                if ("ALL".equals(hideParamsModule)) {
                    return new HideParamsConfig(true); //short cut out, hiding all modules
                }

                hideParamsModules.add(Module.valueOf(hideParamsModule));
            }

            return new HideParamsConfig(hideParamsModules);
        } catch (JSONException e) {
            logger.warn(e.getMessage());
            throw new InvalidConfigException(e);
        } catch (IllegalArgumentException e) {
            throw new InvalidConfigException(e);
        }

    }

}
