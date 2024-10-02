package com.tracelytics.util;

import com.tracelytics.joboe.Constants;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class BackTraceUtil {
    private static final Logger logger = LoggerFactory.getLogger();
    public static StackTraceElement[] getBackTrace(int skipElements) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        int startPosition = 2 + skipElements; // Starts with 2: To exclude the getStackTrace() and addBackTrace calls themselves. Also adds the number of skipElements provided in the argument to skip elements

        if (startPosition >= stackTrace.length) {
            logger.warn("Attempt to skip [" + skipElements + "] elements in addBackTrace is valid, no stack trace element is left!");
            return null;
        }

        int targetStackTraceLength = stackTrace.length - startPosition;
        StackTraceElement[] targetStackTrace = new StackTraceElement[targetStackTraceLength];
        System.arraycopy(stackTrace, startPosition, targetStackTrace, 0, targetStackTraceLength);

        return targetStackTrace;
    }


    public static String backTraceToString(StackTraceElement[] stackTrace) {
        List<StackTraceElement> wrappedStackTrace = Arrays.asList(stackTrace); //wrap it so hashCode and equals work

        String cachedValue = BackTraceCache.getBackTraceString(wrappedStackTrace);
        if (cachedValue != null) {
            return cachedValue;
        }

        StringBuffer st = new StringBuffer();

        if (stackTrace.length > Constants.MAX_BACK_TRACE_LINE_COUNT) { //then we will have to skip some lines
            appendStackTrace(stackTrace, 0 , Constants.MAX_BACK_TRACE_TOP_LINE_COUNT, st); //add the top lines

            st.append("...Skipped " + (stackTrace.length - Constants.MAX_BACK_TRACE_LINE_COUNT) + " line(s)\n");

            appendStackTrace(stackTrace, stackTrace.length - Constants.MAX_BACK_TRACE_BOTTOM_LINE_COUNT, Constants.MAX_BACK_TRACE_BOTTOM_LINE_COUNT, st); //add the bottom lines
        } else {
            appendStackTrace(stackTrace, 0 , stackTrace.length, st); //add everything
        }

        String value = st.toString();

        BackTraceCache.putBackTraceString(wrappedStackTrace, value);

        return value;
    }

    /**
     * Build the stackTrace output and append the result to the buffer provided
     * @param stackTrace The source of the stack trace array
     * @param startPosition
     * @param lineCount
     * @param buffer The buffer that stores the result
     */
    private static void appendStackTrace(StackTraceElement[] stackTrace, int startPosition, int lineCount, StringBuffer buffer) {
        for (int i = startPosition; i < startPosition + lineCount && i < stackTrace.length; i++) {
            buffer.append(stackTrace[i].toString());
            buffer.append("\n");
        }
    }

}
