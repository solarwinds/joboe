package com.solarwinds.util;


import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class BackTraceCache {
    private static Cache<List<StackTraceElement>, String> backTraceCache = null; //delay building cache as it causes problem in jboss see https://github.com/librato/joboe/issues/594
    private static volatile boolean enabled = false;

    static String getBackTraceString(List<StackTraceElement> stackTrace) {
        Cache<List<StackTraceElement>, String> cache = getCache();
        return cache != null ? cache.getIfPresent(stackTrace) : null;
    }

    static void putBackTraceString(List<StackTraceElement> stackTrace, String stackTraceString) {
        Cache<List<StackTraceElement>, String> cache = getCache();
        if (cache != null) {
            cache.put(stackTrace, stackTraceString);
        }
    }

    public static final void enable() {
        enabled = true;
    }

    private static Cache<List<StackTraceElement>, String> getCache() {
        if (backTraceCache == null && enabled) {
            backTraceCache = CacheBuilder.newBuilder().maximumSize(20).expireAfterAccess(3600, TimeUnit.SECONDS).build(); //1 hour cache
        }
         
        return backTraceCache;
    }
    
     
}
