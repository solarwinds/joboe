package com.tracelytics.joboe.span.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import com.tracelytics.ext.google.common.cache.Cache;
import com.tracelytics.ext.google.common.cache.CacheBuilder;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.joboe.settings.SettingsArg;
import com.tracelytics.joboe.settings.SettingsArgChangeListener;
import com.tracelytics.joboe.settings.SettingsManager;
import com.tracelytics.joboe.span.impl.Span.SpanProperty;
import com.tracelytics.joboe.span.impl.Span.TraceProperty;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;

/**
 * Manages and generates transaction name based on various rules.
 * 
 * For transaction name management:
 * <ol>
 *  <li> getTransactionName generates a transaction name, the result will be recorded if not null, if more than 200 transaction name is recorded, it throws a LimitExceededException
 *  <li> addTransactionName allows adding a new transaction name explicitly, if more than 200 transaction name is recorded, it throws a LimitExceededException
 *  <li> Provides a handle to clear the recorded transaction names
 * </ol>
 * 
 * For transaction name generation:
 * <ol>
 *  <li>transaction.namePattern if defined, value is the pattern used to generate a transaction name off a URL by concatenating matching tokens with '.'. The pattern is a list of tokens separated by comma, valid tokens are host and p1, p2, ... pn. For example value "host,p2" on URL http://localhost:8080/test-api/action, will generate transaction name "localhost.action"</li>
 *  <li>controller.action</li>
 *  <li>default transaction name pattern to be applied to URL as defined in <code>DEFAULT_TRANSACTION_NAME_PATTERN</code></li>
 * </ol>
 * 
 * @author pluk
 *
 */
public final class TransactionNameManager {
    private static final Logger logger = LoggerFactory.getLogger();
    private static final String[] DEFAULT_TRANSACTION_NAME_PATTERN = {"p1", "p2"};
    private static final String CUSTOM_TRANSACTION_NAME_PATTERN_SEPARATOR = ".";
    private static final String DEFAULT_TRANSACTION_NAME_PATTERN_SEPARATOR = "/";
    private static final String DOMAIN_PREFIX_SEPARATOR = "/";
    public static final String OVER_LIMIT_TRANSACTION_NAME = "other";
    public static final String UNKNOWN_TRANSACTION_NAME = "unknown";
    public static final int DEFAULT_MAX_NAME_COUNT = 200;
    public static final int MAX_TRANSACTION_NAME_LENGTH = 255;
    public static final String TRANSACTION_NAME_ELLIPSIS = "...";
    public static final Pattern REPLACE_PATTERN = Pattern.compile("[^-.:_\\\\\\/\\w\\? ]");
    public static final String DEFAULT_SDK_TRANSACTION_NAME_PREFIX = "custom-";
    
    static String[] customTransactionNamePattern;
    static final Cache<String, String> urlTransactionNameCache = CacheBuilder.newBuilder().maximumSize(1000).expireAfterAccess(1200, TimeUnit.SECONDS).<String, String>build(); //20 mins cache
    
    private static final Set<String> existingTransactionNames = new HashSet<String>();
    private static boolean limitExceeded;
    private static int maxNameCount = DEFAULT_MAX_NAME_COUNT;
    
    static boolean domainPrefixedTransactionName;
    
    static {
        customTransactionNamePattern = getTransactionNamePattern();
        addNameCountChangeListener();
        
        Boolean domainPrefixedTransactionNameObject  = (Boolean) ConfigManager.getConfig(ConfigProperty.AGENT_DOMAIN_PREFIXED_TRANSACTION_NAME);
        domainPrefixedTransactionName = domainPrefixedTransactionNameObject != null && domainPrefixedTransactionNameObject; //only set it to true if the flag present and is set to true
    }
    
    private TransactionNameManager() { //forbid instantiation
    }

    private static void addNameCountChangeListener() {
        SettingsManager.registerListener(new SettingsArgChangeListener<Integer>(SettingsArg.MAX_TRANSACTIONS) {
            @Override
            public void onChange(Integer newValue) {
                if (newValue != null) {
                    if (newValue > maxNameCount) {
                        limitExceeded = false; //reset the exceed flag
                    }
                    maxNameCount = newValue;
                } else { //reset to default
                    maxNameCount = DEFAULT_MAX_NAME_COUNT;
                }
            }
        });
        
    }

    private static String[] getTransactionNamePattern() {
        String pattern = (String) ConfigManager.getConfig(ConfigProperty.AGENT_TRANSACTION_NAME_PATTERN);
        return pattern != null ? parseTransactionNamePattern(pattern) : null;
    }
    
    static String[] parseTransactionNamePattern(String pattern) {
        String[] tokens = pattern.split(",");
        for (int i = 0 ; i < tokens.length; i ++) {
            tokens[i] = tokens[i].trim();
        }
        return tokens;
    }
    
    /**
     * Gets a transaction name based on information provided in a span and domainPrefixedTransactionName flag,
     * the result will be recorded if not null.
     *  
     * If more than <code>MAX_NAME_COUNT</code> transaction name is recorded, "other" will be returned.
     * If the logic fails to extract a transaction from the given span, "unknown" will be returned. 
     * @param span
     * @return
     */
    public static String getTransactionName(Span span) {
        String transactionName;
        if ((transactionName = span.getTracePropertyValue(TraceProperty.CUSTOM_TRANSACTION_NAME)) == null && //transaction name not set explicitly by SDK
            (transactionName = span.getTracePropertyValue(TraceProperty.TRANSACTION_NAME)) == null) { //transaction name not set explicitly by instrumentation
            transactionName = buildTransactionName(span); //try to build the transaction name
        }
        
        if (transactionName != null) {
            if (domainPrefixedTransactionName) {
                transactionName = prefixTransactionNameWithDomainName(transactionName, span);
            }
          
            transactionName = transformTransactionName(transactionName); 
            return addTransactionName(transactionName) ? transactionName : OVER_LIMIT_TRANSACTION_NAME; //check the transaction name limit;
        } else {
            return UNKNOWN_TRANSACTION_NAME; //unable to build the transaction name
        }
    }
    
    private static String prefixTransactionNameWithDomainName(String transactionName, Span span) {
        Object httpHostValue = span.getTag("HTTP-Host");
        if (httpHostValue instanceof String && !"".equals(httpHostValue)) {
            String domain = (String) httpHostValue;
            
            if (transactionName.startsWith("/")) {
                return domain + transactionName;
            } else {
                return domain + DOMAIN_PREFIX_SEPARATOR + transactionName; 
            }
        }
        
        return transactionName;
    }

    /**
     * Transform the transaction name according to https://github.com/librato/gotv/blob/376240c5fcce883f37a5358cb30ac39ab9283c7e/collector/agentmetrics/tags.go#L41-L52 and
     * https://github.com/librato/jackdaw/blob/0930023a2d30dc42e58ed45cc05df9b46e2b7da1/src/main/java/com/librato/jackdaw/ingress/IngressMeasurement.java#L28
     * 
     * @param inputTransactionName
     * @return
     */
    static String transformTransactionName(String inputTransactionName) {
        String transactionName = inputTransactionName;
        
        if (transactionName.length() > MAX_TRANSACTION_NAME_LENGTH) {
            transactionName = transactionName.substring(0, MAX_TRANSACTION_NAME_LENGTH - TRANSACTION_NAME_ELLIPSIS.length()) + TRANSACTION_NAME_ELLIPSIS;
        } else if ("".equals(transactionName)) {
            transactionName = " "; //ensure that it at least has 1 character 
        }
        
        
        
        transactionName = REPLACE_PATTERN.matcher(transactionName).replaceAll("_"); 
        
        transactionName = transactionName.toLowerCase();
        
        if (!transactionName.equalsIgnoreCase(inputTransactionName)) {
            logger.debug("Transaction name [" + inputTransactionName + "] has been transformed to [" + transactionName + "]");
        }
        
        return transactionName;
    }

    /**
     * Builds a transaction name based on information provided in a span
     * @param span
     * @return  a transaction name built based on the span, null if no transaction name can be built
     */
    static String buildTransactionName(Span span) {
        String url = (String) span.getTags().get("URL");
        if (url != null) {
            int queryStart = url.indexOf('?');
            if (queryStart > -1) {
                url = url.substring(0, queryStart);
            }
        }
        
        String host = (String) span.getTags().get("HTTP-Host");
        
        if (customTransactionNamePattern != null) { //try forming transaction name by the custom configured pattern
            String transactionName = getTransactionNameByUrlAndPattern(host, url, customTransactionNamePattern, false, CUSTOM_TRANSACTION_NAME_PATTERN_SEPARATOR); 
            
            if (transactionName != null) {
                return transactionName;
            }
        }
        
        //try controller/action
        String controller = span.getTracePropertyValue(TraceProperty.CONTROLLER);
        String action = span.getTracePropertyValue(TraceProperty.ACTION);
        if (controller != null && !"".equals(controller) && action != null) { //controller should not be null nor empty
            //do NOT add to cache as this transaction name is not extracted from URL. ie same URL might map to multiple controller/action combinations
            if ("".equals(action)) { //if action is empty string, use the controller name only to avoid trailing dot
                return controller;
            } else {
                return controller + "." + action;
            }
        }
        
        //try the default token name pattern
        String transactionNameByUrl = getTransactionNameByUrlAndPattern(host, url, DEFAULT_TRANSACTION_NAME_PATTERN, true, DEFAULT_TRANSACTION_NAME_PATTERN_SEPARATOR);
        if (transactionNameByUrl != null) {
            return transactionNameByUrl;
        }
        
        if (span.getSpanPropertyValue(SpanProperty.IS_SDK) && span.getOperationName() != null) {
             return DEFAULT_SDK_TRANSACTION_NAME_PREFIX + span.getOperationName();
        }
        
        return null; //cannot build name based on the span info
    }
    
    /**
     * Gets transaction name based on host, URL and provided name pattern. It might look up and update the urlTransactionNameCache
     *  
     * @param host
     * @param url   url that must NOT contains query param
     * @param transactionNamePattern
     * @return
     */
    static String getTransactionNameByUrlAndPattern(String host, String url, String[] transactionNamePattern, boolean separatorAsPrefix, String separator) {
        if (url == null) {
            return null;
        }
        
        String transactionName = urlTransactionNameCache.getIfPresent(url);
        if (transactionName == null) {
            transactionName = buildTransactionNameByUrlAndPattern(host, url, transactionNamePattern, separatorAsPrefix, separator);
            if (transactionName != null) {
                urlTransactionNameCache.put(url, transactionName);
            }
        }
        
        return transactionName;
    }
    
    /**
     * Generates a transaction name by concatenating matching pattern tokens with '.' 
     * 
     * The valid tokens are host and p1, p2, ... pn. For example a token list of ["host", "p2"] on URL http://localhost:8080/test-api/action/1, will generate transaction name "localhost.action"</li>
     * 
     * @param host
     * @param url                       url that must NOT contains query param
     * @param transactionNamePattern    the token pattern as an array
     * @param separator 
     * @param separatorAsPrefix 
     * @return
     */
    static String buildTransactionNameByUrlAndPattern(String host, String url, String[] transactionNamePattern, boolean separatorAsPrefix, String separator) {
        Map<String, String> urlTokenMap = new HashMap<String, String>();
        if (host != null) {
          //remove port?
            int colonIndex = host.indexOf(":");
            if (host.indexOf(":") != -1) {
                host = host.substring(0, colonIndex);
            }
            urlTokenMap.put("host", host);
        }
        
        int counter = 1;
        for (String token : url.split("/")) {
            if (!"".equals(token)) {
                String tokenName = "p" + counter ++;
                urlTokenMap.put(tokenName, token);
            }
        }
        
        String transactionName = separatorAsPrefix ? separator : "";
        boolean isFirstToken = true;
        for (String patternToken : transactionNamePattern) {
            if (urlTokenMap.containsKey(patternToken)) {
                if (isFirstToken) {
                    transactionName += urlTokenMap.get(patternToken);
                    isFirstToken = false;
                } else {
                    transactionName += separator + urlTokenMap.get(patternToken);
                }
            }
        }
        return transactionName;
    }

    /**
     * Adds a transaction name to the tracking set
     * @param transactionName   the name to be added, should NOT be null
     * @return  true if transactionName is already existed or added successfully; false otherwise (limit exceeded)
     */
    public static boolean addTransactionName(String transactionName) {
        synchronized(existingTransactionNames) {
            if (!existingTransactionNames.contains(transactionName)) {
                if (existingTransactionNames.size() < maxNameCount ) {
                    existingTransactionNames.add(transactionName);
                    return true;
                }
            } else { //the name already exists, so it's not over the limit
                return true;
            }
        }
        limitExceeded = true; //toggle the flag
        return false;
    }
    
    public static boolean isLimitExceeded() {
        return limitExceeded;
    }
    
    public static void clearTransactionNames() {
       synchronized(existingTransactionNames) {
           existingTransactionNames.clear();

           limitExceeded = false;
       }
    }
    
    /**
     * For internal testing usage only
     */
    static void reset() { 
        clearTransactionNames();
        urlTransactionNameCache.invalidateAll();
        maxNameCount = DEFAULT_MAX_NAME_COUNT;
    }
}