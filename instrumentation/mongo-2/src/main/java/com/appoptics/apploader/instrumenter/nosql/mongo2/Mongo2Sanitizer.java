package com.appoptics.apploader.instrumenter.nosql.mongo2;

import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import java.util.Arrays;
import java.util.List;

public class Mongo2Sanitizer {
    private static final Mongo2Sanitizer SINGLETON = new Mongo2Sanitizer(); //it's a singleton for now
    private static final List<String> SAFE_KEYS = Arrays.asList( "ordered", "insert", "count", "find", "create");
    private static final String REPLACEMENT_CHAR = "?"; //used to replace the sensitive values
    private static final Logger LOGGER = LoggerFactory.getLogger();
    private static final boolean ENABLED = ConfigManager.getConfigOptional(ConfigProperty.AGENT_MONGO_SANITIZE, true);

    private Mongo2Sanitizer() {

    }

    public static Mongo2Sanitizer getSanitizer() {
        return SINGLETON;
    }

    private void sanitize(BSONObject inputBsonNode, BSONObject outputBsonNode) {
        for (String key : inputBsonNode.keySet()) {
            Object inputChildNode = inputBsonNode.get(key);

            if (SAFE_KEYS.contains(key) && !(inputChildNode instanceof BSONObject)) { //then safe to insert directly
                outputBsonNode.put(key, inputBsonNode.get(key));
            } else if (inputChildNode instanceof BSONObject) {
                BSONObject outputChildNode;
                if (inputChildNode instanceof BasicBSONList) {
                    outputChildNode = new BasicBSONList();
                    sanitize((BasicBSONList) inputChildNode, outputChildNode);
                } else {
                    outputChildNode = new BasicBSONObject();
                    sanitize((BSONObject) inputChildNode, outputChildNode);
                }
                outputBsonNode.put(key, outputChildNode);
            } else {
                outputBsonNode.put(key, REPLACEMENT_CHAR); //replacement
            }
        }
    }

    public String sanitize(Object input) {
        if (!ENABLED) {
            return input != null ? input.toString() : null;
        }

        if (input instanceof BSONObject) {
            BSONObject inputBson = (BSONObject) input;
            BSONObject outputBson = new BasicBSONObject();
            sanitize(inputBson, outputBson);

            return outputBson.toString();
        } else {
            LOGGER.warn("Expect " + BSONObject.class.getName() + " instance but found " + input + " of class " + (input != null ? input.getClass().getName() : "null"));
            return null;
        }
    }
}