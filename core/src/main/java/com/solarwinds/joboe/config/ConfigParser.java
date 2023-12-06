package com.solarwinds.joboe.config;

public interface ConfigParser<T, R> {
    /**
     * Convert the input of type T into result of R. If there are any error during the conversion, it should throw InvalidConfigException
     * @param input
     * @return
     * @throws InvalidConfigException 
     */
    R convert(T input) throws InvalidConfigException;
}

