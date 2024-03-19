package com.solarwinds.joboe.logging;

class SystemOutStream implements LoggerStream {
    static final SystemOutStream INSTANCE = new SystemOutStream();

    SystemOutStream() {
    }

    @Override
    public void println(String value) {
        System.out.println(value);
    }

    @Override
    public void printStackTrace(Throwable throwable) {
        throwable.printStackTrace(System.out);
    }
}