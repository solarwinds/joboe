package com.tracelytics.agent.config;

import com.tracelytics.instrumentation.Module;
import com.tracelytics.joboe.config.ConfigParser;
import com.tracelytics.joboe.config.InvalidConfigException;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ModulesParser implements ConfigParser<String[], List<Module>> {
    private Logger logger = LoggerFactory.getLogger();
    public static final ModulesParser INSTANCE = new ModulesParser();

    @Override
    public List<Module> convert(String[] input) throws InvalidConfigException {
        List<Module> modules = new ArrayList<Module>();
        for (String moduleString : input) {
            if ("ALL".equals(moduleString)) {
                return Arrays.asList(Module.values());
            }

            try {
                Module module = Module.valueOf(moduleString);
                modules.add(module);
            } catch (IllegalArgumentException e) {
                logger.warn("Failed to parse module string [" + moduleString + "] as it is not a known module");
                throw new InvalidConfigException(e);
            }
        }

        return modules;
    }

}
