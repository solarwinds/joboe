package com.solarwinds.joboe.core.util;

import com.solarwinds.joboe.config.JavaVersionComparator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JavaVersionComparatorTest {
    @Test
    public void testVersionCompare() {
        assertTrue(JavaVersionComparator.compare("1.8.0", "17.0.1") < 0);
        assertTrue(JavaVersionComparator.compare("1.8.0", "1.8.0_252") < 0);
        assertTrue(JavaVersionComparator.compare("1.8.0_252", "1.8.0_332") < 0);
        assertTrue(JavaVersionComparator.compare("1.8.0", "1.8.1") < 0);
        assertTrue(JavaVersionComparator.compare("1.8.0", "1.9.1") < 0);
        assertTrue(JavaVersionComparator.compare("1.8.2", "1.10.1") < 0);
        assertEquals(0, JavaVersionComparator.compare("1.8.0", "1.8.0"));
        assertTrue(JavaVersionComparator.compare("16.8.2", "17.0.1") < 0);
    }
}
