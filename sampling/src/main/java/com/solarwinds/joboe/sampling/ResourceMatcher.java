package com.solarwinds.joboe.sampling;

public interface ResourceMatcher {
    boolean matches(String signal);
}
