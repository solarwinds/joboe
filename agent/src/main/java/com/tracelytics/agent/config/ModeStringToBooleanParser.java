package com.tracelytics.agent.config;

import com.tracelytics.joboe.config.ConfigParser;
import com.tracelytics.joboe.config.InvalidConfigException;

public class ModeStringToBooleanParser implements ConfigParser<String, Boolean> {
    public static final ModeStringToBooleanParser INSTANCE = new ModeStringToBooleanParser();

    private ModeStringToBooleanParser() {

    }

    @Override
    public Boolean convert(String input) throws InvalidConfigException {
        if ("enabled".equals(input)) {
            return true;
        } else if ("disabled".equals(input)) {
            return false;
        } else {
            throw new InvalidConfigException("Expected value [enabled] or [disabled] but found [" + input + "]");
        }
    }
}