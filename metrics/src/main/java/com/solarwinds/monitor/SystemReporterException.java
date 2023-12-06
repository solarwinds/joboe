package com.solarwinds.monitor;

/**
 * Used to raise exception for System monitor module
 * @author Patson Luk
 *
 */
public class SystemReporterException extends Exception {
    public SystemReporterException(String message) {
        super(message);
    }
}
