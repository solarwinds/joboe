package com.appoptics.apploader.instrumenter.nosql.mongo3;

import com.tracelytics.joboe.config.ConfigContainer;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.joboe.config.InvalidConfigException;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonString;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class Mongo3SanitizerTest {
    private static final Mongo3Sanitizer SANITIZER = Mongo3Sanitizer.getSanitizer();

    @Test
    public void testSanitizationSimple() {
        BsonDocument testObject = new BsonDocument();

        testObject.put("k1", new BsonInt32(1));
        testObject.put("k2", new BsonString("a"));
        testObject.put("create", new BsonString("b"));

        BsonDocument expectedObject = new BsonDocument();
        expectedObject.put("k1", new BsonString("?"));
        expectedObject.put("k2", new BsonString("?"));
        expectedObject.put("create", new BsonString("b"));

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
        BsonDocument testObject = new BsonDocument();
        BsonArray testArray1 = new BsonArray();
        testArray1.add(new BsonString("a"));
        testArray1.add(new BsonString("b"));
        BsonArray testArray2 = new BsonArray();
        testArray2.add(new BsonString("c"));
        testArray2.add(new BsonString("d"));
        testObject.put("k1", testArray1);
        testObject.put("create", testArray2);

        BsonDocument expectObject = new BsonDocument();
        BsonArray expectArray = new BsonArray();
        expectArray.add(new BsonString("?"));
        expectArray.add(new BsonString("?"));
        expectObject.put("k1", expectArray);
        expectObject.put("create", expectArray);


        String result = SANITIZER.sanitize(testObject);
        assertEquals(expectObject.toString(), result);
    }

    @Test
    public void testSanitizationComplexObject() {
        BsonDocument testObject = new BsonDocument();
        BsonArray testArray = new BsonArray();
        testArray.add(new BsonDocument("k1", new BsonInt32(1)));
        testArray.add(new BsonDocument("k2", new BsonString("test")));
        testArray.add(new BsonString("c"));
        testArray.add(new BsonInt32(2));
        testObject.put("k3", testArray);

        BsonDocument expectObject = new BsonDocument();
        BsonArray expectArray = new BsonArray();
        expectArray.add(new BsonDocument("k1", new BsonString("?")));
        expectArray.add(new BsonDocument("k2", new BsonString("?")));
        expectArray.add(new BsonString("?"));
        expectArray.add(new BsonString("?"));
        expectObject.put("k3", expectArray);

        String result = SANITIZER.sanitize(testObject);
        assertEquals(expectObject.toString(), result);
    }
}
