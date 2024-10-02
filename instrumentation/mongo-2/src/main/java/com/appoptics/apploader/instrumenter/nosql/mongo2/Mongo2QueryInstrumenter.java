package com.appoptics.apploader.instrumenter.nosql.mongo2;

import com.appoptics.instrumentation.nosql.mongo2.Mongo2BaseInstrumentation;
import com.appoptics.instrumentation.nosql.mongo2.Mongo2DbCursorInstrumentation;

public class Mongo2QueryInstrumenter {
    private static final Mongo2Sanitizer SANITIZER = Mongo2Sanitizer.getSanitizer();

    public static String getQuery() {
        Mongo2DbCursorInstrumentation.CursorOpInfo cursorOp = Mongo2DbCursorInstrumentation.getCurrentCursorOp();
        if (cursorOp != null) {
            return getQuery(cursorOp.getQuery());
        }
        return null;
    }

    public static String getQuery(Object queryObject) {
        if (queryObject != null) {
            if (Mongo2BaseInstrumentation.isEmptyQuery(queryObject.toString())) {
                return "all";
            } else {
                return SANITIZER.sanitize(queryObject);
            }
        }
        return null;

    }


}
