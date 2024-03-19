package com.solarwinds.joboe.logging;

class SystemErrStream implements LoggerStream {
    static final SystemErrStream INSTANCE = new SystemErrStream();

    SystemErrStream() {
    }

    @Override
    public void println(String value) {
        System.err.println(value);
    }

    @Override
    public void printStackTrace(Throwable throwable) {
        throwable.printStackTrace(System.err);
    }
}