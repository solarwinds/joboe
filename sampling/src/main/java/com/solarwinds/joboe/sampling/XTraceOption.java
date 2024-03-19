package com.solarwinds.joboe.sampling;

import java.util.HashMap;
import java.util.Map;

import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import lombok.Getter;

/**
 * An option key within X-Trace-Options header. This does NOT store the option value
 * @param <V>   The value type of the option
 *
 * @see XTraceOptions
 */
@Getter
public class XTraceOption<V> {

    private static final Logger LOGGER = LoggerFactory.getLogger();
    private static final Map<String, XTraceOption<?>> keyLookup = new HashMap<String, XTraceOption<?>>();
    public static final XTraceOption<Boolean> TRIGGER_TRACE = new XTraceOption<Boolean>("trigger-trace", null, false);
    public static final XTraceOption<String> SW_KEYS = new XTraceOption<String>("sw-keys", ValueParser.STRING_VALUE_PARSER);
    public static final XTraceOption<Long> TS = new XTraceOption<Long>("ts", ValueParser.LONG_VALUE_PARSER);
    public static final String CUSTOM_KV_PREFIX = "custom-";


    private final V defaultValue;
    @Getter
    private final String key;
    private final ValueParser<V> parser;
    private boolean isCustomKv = false;

    /**
     *
     * @param key
     * @param parser    null parser indicates that this is a key only option
     */
    private XTraceOption(String key, ValueParser<V> parser) {
        this(key, parser, null);
    }
    private XTraceOption(String key, ValueParser<V> parser, V defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.parser = parser;

        keyLookup.put(key, this);
    }



    public static XTraceOption<?> fromKey(String key) {
        if (key.contains(" ")) { //invalid key if it contains any space. Not using regex here as it could be pretty slow
            return null;
        }

        XTraceOption<?> option = keyLookup.get(key);
        if (option != null) {
            return option;
        } else if (isCustomKv(key)) {
            option = new XTraceOption<>(key, ValueParser.STRING_VALUE_PARSER);
            option.isCustomKv = true;
            return option;
        } else {
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        XTraceOption<?> that = (XTraceOption<?>) o;

        return key.equals(that.key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    /**
     * Whether this option is a custom one that starts with {@link XTraceOption#CUSTOM_KV_PREFIX}
     * @return
     */
    public boolean isCustomKv() {
        return this.isCustomKv;
    }

    /**
     * Whether this option should appear in key-value pair or not.
     *
     * @return
     */
    public boolean isKeyOnlyOption() {
        return parser == null;
    }

    private static boolean isCustomKv(String key) {
        return key.startsWith(CUSTOM_KV_PREFIX);
    }

    private interface ValueParser<V> {
        V parse(String stringValue) throws IllegalArgumentException;
//        static final CustomTagsValueParser CUSTOM_TAGS_VALUE_PARSER = new CustomTagsValueParser();
        BooleanValueParser BOOLEAN_VALUE_PARSER = new BooleanValueParser();
        StringValueParser STRING_VALUE_PARSER = new StringValueParser();
        LongValueParser LONG_VALUE_PARSER = new LongValueParser();
    }

    public V parseValueFromString(String value) throws XTraceOptions.InvalidValueXTraceOptionException {
        try {
            return parser != null ? parser.parse(value) : null;
        } catch (IllegalArgumentException e) {
            throw new XTraceOptions.InvalidValueXTraceOptionException(this, value);
        }
    }

    private static class BooleanValueParser implements ValueParser<Boolean> {
        @Override
        public Boolean parse(String stringValue) {
            return "1".equals(stringValue) || Boolean.valueOf(stringValue);
        }
    }

    private static class StringValueParser implements ValueParser<String> {
        @Override
        public String parse(String stringValue) {
            return stringValue;
        }

    }

    private static class LongValueParser implements ValueParser<Long> {
        @Override
        public Long parse(String stringValue) throws NumberFormatException {
            return Long.parseLong(stringValue);
        }
    }
}

