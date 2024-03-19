package com.solarwinds.joboe.logging;

interface LoggerStream {
    void println(String value);

    void printStackTrace(Throwable throwable);
}