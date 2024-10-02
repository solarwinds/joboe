package com.tracelytics.agent.config;

import java.util.Arrays;
import java.util.Collections;

import com.tracelytics.agent.config.ModulesParser;
import com.tracelytics.joboe.config.InvalidConfigException;
import org.junit.Test;

import com.tracelytics.instrumentation.Module;

import junit.framework.TestCase;

public class ModuleParserTest extends TestCase {
    @Test
    public void testConvert() throws Exception {
        ModulesParser parser = ModulesParser.INSTANCE;
        
        assertEquals(Arrays.asList(Module.values()), parser.convert(new String[] { "ALL" }));
        assertEquals(Arrays.asList(Module.values()), parser.convert(new String[] { "JDBC", "ALL"}));
        
        assertEquals(Arrays.asList(new Module[] { Module.JDBC, Module.SERVLET}), parser.convert(new String[] { "JDBC", "SERVLET"}));
        
        try {
            parser.convert(new String[] { "JDBC", "INVALID"}); //invalid value fails the whole list
            fail("converting invalid module should throw IllegalConfigException, but it did not");
        } catch (InvalidConfigException e) {
            //expected;
        }
        
        assertEquals(Collections.EMPTY_LIST, parser.convert(new String[0]));
    }
}
