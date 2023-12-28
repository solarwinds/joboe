package com.solarwinds.joboe.core.util;

/**
 * The provider which offers concrete instance of the HostInfoReader interface. This keeps service loader away from
 * instantiating the concrete HostInfoReader classes.
 */
public interface HostInfoReaderProvider {
    HostInfoReader getHostInfoReader();
}
