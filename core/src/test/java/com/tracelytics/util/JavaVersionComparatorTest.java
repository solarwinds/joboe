package com.tracelytics.util;

import junit.framework.TestCase;

public class JavaVersionComparatorTest extends TestCase {
    public void testVersionCompare() {
        assertTrue(JavaVersionComparator.compare("1.8.0", "17.0.1") < 0);
        assertTrue(JavaVersionComparator.compare("1.8.0", "1.8.0_252") < 0);
        assertTrue(JavaVersionComparator.compare("1.8.0", "1.8.1") < 0);
        assertTrue(JavaVersionComparator.compare("1.8.0", "1.9.1") < 0);
        assertTrue(JavaVersionComparator.compare("1.8.2", "1.10.1") < 0);
        assertTrue(JavaVersionComparator.compare("1.8.0", "1.8.0") == 0);
        assertTrue(JavaVersionComparator.compare("16.8.2", "17.0.1") < 0);
    }
}
