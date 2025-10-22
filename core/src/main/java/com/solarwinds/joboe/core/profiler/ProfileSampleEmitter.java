package com.solarwinds.joboe.core.profiler;

public interface ProfileSampleEmitter {
    default void beginEntryEmit() {

    }

    default boolean endEntryEmit() {
        return false;
    }

    default void beginExitEmit() {
    }

    default boolean endExitEmit() {
        return false;
    }

    default void beginSampleEmit() {

    }

    default boolean endSampleEmit() {
        return false;
    }

    default <T> void addAttribute(String key, T value) {

    }
}
