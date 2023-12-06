package com.solarwinds.joboe.config;

public interface ResourceMatcher {
    boolean matches(String signal);
}
