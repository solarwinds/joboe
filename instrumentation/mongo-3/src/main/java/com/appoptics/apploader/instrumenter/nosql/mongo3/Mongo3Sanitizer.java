package com.appoptics.apploader.instrumenter.nosql.mongo3;


import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;
import org.bson.*;

import java.util.Arrays;
import java.util.List;

public class Mongo3Sanitizer {
    private static final Mongo3Sanitizer SINGLETON = new Mongo3Sanitizer(); //it's a singleton for now
    private static final List<String> SAFE_KEYS = Arrays.asList( "ordered", "insert", "count", "find", "create");
    private static final BsonString REPLACEMENT_CHAR = new BsonString("?"); //used to replace the sensitive values
    private static final Logger LOGGER = LoggerFactory.getLogger();
    private static final boolean ENABLED = ConfigManager.getConfigOptional(ConfigProperty.AGENT_MONGO_SANITIZE, true);

    private Mongo3Sanitizer() {

    }

    public static Mongo3Sanitizer getSanitizer() {
        return SINGLETON;
    }

    private BsonValue sanitizeBson(BsonValue inputBsonNode) {
        if (inputBsonNode.isDocument()) {
            return sanitizeBson(inputBsonNode.asDocument());
        } else if (inputBsonNode.isArray()) {
            return sanitizeBson(inputBsonNode.asArray());
        } else {
            return REPLACEMENT_CHAR;
        }
    }

    private BsonDocument sanitizeBson(BsonDocument inputBsonNode) {
        BsonDocument outputNode = new BsonDocument();
        for (String key : inputBsonNode.keySet()) {
            BsonValue inputChildNode = inputBsonNode.get(key);
            if (SAFE_KEYS.contains(key) && inputChildNode.isString()) { //then safe to insert directly
                outputNode.put(key, inputChildNode);
            } else {
                outputNode.put(key, sanitizeBson(inputChildNode));
            }
        }
        return outputNode;
    }

    private BsonArray sanitizeBson(BsonArray inputBsonNode) {
        BsonArray outputNode = new BsonArray();
        for (BsonValue bsonValue : inputBsonNode) {
            outputNode.add(sanitizeBson(bsonValue));
        }

        return outputNode;
    }

    public String sanitize(Object input) {
        if (!ENABLED) {
            return input != null ? input.toString() : null;
        }

        if (input instanceof BsonValue) {
            return SINGLETON.sanitizeBson((BsonValue) input).toString();
        } else {
            LOGGER.warn("Expect " + BsonValue.class.getName() + " instance but found " + input + " of class " + (input != null ? input.getClass().getName() : "null"));
            return null;
        }
    }
}
