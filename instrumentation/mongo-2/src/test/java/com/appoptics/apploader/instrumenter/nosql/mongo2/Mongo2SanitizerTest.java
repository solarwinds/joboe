package com.appoptics.apploader.instrumenter.nosql.mongo2;

import com.tracelytics.joboe.config.ConfigContainer;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.joboe.config.InvalidConfigException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.junit.Test;

import static org.junit.Assert.*;


public class Mongo2SanitizerTest {
    private static final Mongo2Sanitizer SANITIZER = Mongo2Sanitizer.getSanitizer();

    @Test
    public void testSanitizationSimple() {
        BSONObject testObject = new BasicBSONObject();

        testObject.put("k1", 1);
        testObject.put("k2", "a");
        testObject.put("create", "b");

        BSONObject expectedObject = new BasicBSONObject();
        expectedObject.put("k1", "?");
        expectedObject.put("k2", "?");
        expectedObject.put("create", "b");

        String result = SANITIZER.sanitize(testObject);
        assertEquals(expectedObject.toString(), result);
    }

    @Test
    public void testSanitizationInvalidObject() {
        String result = SANITIZER.sanitize(new Object());
        assertEquals(null, result);
    }


    @Test
    public void testSanitizationArray() {
        BSONObject testObject = new BasicBSONObject();
        BasicBSONList testArray1 = new BasicBSONList();
        testArray1.add("a");
        testArray1.add("b");
        BasicBSONList testArray2 = new BasicBSONList();
        testArray2.add("c");
        testArray2.add("d");
        testObject.put("k1", testArray1);
        testObject.put("create", testArray2);

        BSONObject expectObject = new BasicBSONObject();
        BasicBSONList expectArray = new BasicBSONList();
        expectArray.add("?");
        expectArray.add("?");
        expectObject.put("k1", expectArray);
        expectObject.put("create", expectArray);


        String result = SANITIZER.sanitize(testObject);
        assertEquals(expectObject.toString(), result);
    }

    @Test
    public void testSanitizationComplexObject() {
        BSONObject testObject = new BasicBSONObject();
        BasicBSONList testArray = new BasicBSONList();
        testArray.add(new BasicBSONObject("k1", 1));
        testArray.add(new BasicBSONObject("k2", "test"));
        testArray.add("c");
        testArray.add(2);
        testObject.put("k3", testArray);

        BSONObject expectObject = new BasicBSONObject();
        BasicBSONList expectArray = new BasicBSONList();
        expectArray.add(new BasicBSONObject("k1", "?"));
        expectArray.add(new BasicBSONObject("k2", "?"));
        expectArray.add("?");
        expectArray.add("?");
        expectObject.put("k3", expectArray);

        String result = SANITIZER.sanitize(testObject);
        assertEquals(expectObject.toString(), result);
    }
}
