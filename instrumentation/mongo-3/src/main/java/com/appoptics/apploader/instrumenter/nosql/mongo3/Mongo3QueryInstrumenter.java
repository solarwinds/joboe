package com.appoptics.apploader.instrumenter.nosql.mongo3;

public class Mongo3QueryInstrumenter {
    private static final Mongo3Sanitizer SANITIZER = Mongo3Sanitizer.getSanitizer();

    public static String sanitize(Object input) {
        return SANITIZER.sanitize(input);
    }
}
