package com.solarwinds.joboe.core.metrics.histogram;

public class HistogramException extends Exception {

    HistogramException(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }

    HistogramException(String arg0) {
        super(arg0);
    }

    HistogramException(Throwable arg0) {
        super(arg0);
    }
    
}
