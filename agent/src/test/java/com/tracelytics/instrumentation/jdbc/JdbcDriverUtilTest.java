package com.tracelytics.instrumentation.jdbc;

import junit.framework.TestCase;

public class JdbcDriverUtilTest extends TestCase {

    public void testParseFlavorNameFromPackageName() {
        assertEquals("mydriver", JdbcDriverUtil.parseFlavorNameFromPackageName("com.myDriver.statement"));
        assertEquals("mydriver", JdbcDriverUtil.parseFlavorNameFromPackageName("com.myDriver"));
        assertEquals(null, JdbcDriverUtil.parseFlavorNameFromPackageName("ru.myDriver"));
        assertEquals(null, JdbcDriverUtil.parseFlavorNameFromPackageName("myDriver"));
        assertEquals(null, JdbcDriverUtil.parseFlavorNameFromPackageName(null));
    }
}
