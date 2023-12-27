package com.solarwinds.monitor;

import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.Map.Entry;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.JMRuntimeException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import com.solarwinds.joboe.config.ConfigContainer;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.metrics.MetricKey;
import com.solarwinds.metrics.MetricsEntry;
import com.solarwinds.metrics.measurement.SimpleMeasurementMetricsEntry;
import com.solarwinds.monitor.jmx.processor.AttributeProcessor;
import com.solarwinds.monitor.jmx.processor.AttributeProcessorLocator;
import com.solarwinds.shaded.org.json.JSONArray;
import com.solarwinds.shaded.org.json.JSONException;
import com.solarwinds.shaded.org.json.JSONObject;

/**
 * Sub metrics collector that collects metrics from JMX's mBeans. Take note that each {@link #collect()} call would return information collected from each of the {@link JMXScope} passed in via the constructor
 * as {@code ConfigProperty.MONITOR_JMX_SCOPES} in {@link ConfigContainer}
 * 
 * @author Patson Luk
 *
 */
class JMXCollector extends AbstractMetricsEntryCollector {
   
    private final List<JMXScope> scopes;  //scopes that this collector should collect information from
    
    static final int DEFAULT_MAX_ENTRY_COUNT = 100; //entries allowed per collection cycle
    private int maxEntryCount = DEFAULT_MAX_ENTRY_COUNT;
    
    
    static final String JMX_LABEL = "trace.jvm";
    private static final String DEFAULT_MBEAN_DOMAIN = "java.lang";

    public JMXCollector(ConfigContainer configs) throws InvalidConfigException {
        //determine information groups to collect
        String scopesString = (String) configs.get(ConfigProperty.MONITOR_JMX_SCOPES);
        if (scopesString == null) {
            throw new InvalidConfigException("No JMX scope was defined! No JMX metrics would be collected");
        } else {
            scopes = parseScopesString(scopesString);
        }
        
      //Set the max entry count if provided
        if (configs.containsProperty(ConfigProperty.MONITOR_JMX_MAX_ENTRY)) {
            int maxEntryCount = (Integer)configs.get(ConfigProperty.MONITOR_JMX_MAX_ENTRY);
            if (maxEntryCount < 0) {
                throw new InvalidConfigException(ConfigProperty.MONITOR_JMX_MAX_ENTRY + " should not be negative but found: " + maxEntryCount);
            }
            this.maxEntryCount = maxEntryCount;
        }
        
    }
    
    /**
     * Parse the scopes string from external input
     * @param scopesString
     * @return a List of <code>JMXScope</code> based on the input argument
     * @throws InvalidConfigException 
     */
    private static List<JMXScope> parseScopesString(String scopesString) throws InvalidConfigException {
        List<JMXScope> scopes = new ArrayList<JMXScope>();
        try {
            JSONObject object = new JSONObject(scopesString);
            for (Object keyAsObject : object.keySet()) {
                String objectName = (String) keyAsObject;
                
                Object attributeObj = object.get(objectName);
                
                String[] attributes;
                
                if (attributeObj instanceof String) {
                    String attributeString = (String) attributeObj;
                    attributeString = attributeString.trim();
                    
                    if ("".equals(attributeString) || "*".equals(attributeString)) {
                        attributes = new String[0];
                    } else {
                        attributes = new String[] { attributeString };
                    }
                } else if (attributeObj instanceof JSONArray){
                    JSONArray attributeArray = (JSONArray) attributeObj;
                    
                    attributes = new String[attributeArray.length()];
                    for (int i = 0 ; i < attributeArray.length(); i++) {
                        attributes[i] = attributeArray.getString(i);
                    }
                } else {
                    logger.warn("Unexpected jmx scope value of type [" + attributeObj.getClass().getName() + "]");
                    return Collections.emptyList();
                }
                
                scopes.add(new JMXScope(objectName, attributes));
            }
        } catch (JSONException e) {
            logger.warn("JSON Exception encountered! " + e.getMessage());
            throw new InvalidConfigException("Error parsing scope for JMX : " + e.getMessage(), e);
        }
        
        Collections.sort(scopes, (o1, o2) -> {
            if (o1.getObjectName() == null) {
                return o2.getObjectName() == null ? 0 : -1;
            }
            return o1.getObjectName().compareTo(o2.getObjectName());
        });
        
        return scopes;
    }
    
    
    /**
     * Collects information from the JMX mBeans
     * @return  a Map with key as the attribute name/key and value as the attribute values from the JMX mBeans
     */
    public Map<MetricKey, Number> collect() throws IntrospectionException, InstanceNotFoundException, ReflectionException, AttributeNotFoundException, MBeanException {
        ClassLoader existingContextLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        try {
            if (systemClassLoader != null && existingContextLoader == null) {
                Thread.currentThread().setContextClassLoader(systemClassLoader); //set system class loader to context, it should have better access to classes than the null (bootstrap) loader
            }
            return collect(ManagementFactory.getPlatformMBeanServer());
        } catch (JMRuntimeException e) {
            //known issue for some app server, which the JMX class might not be available yet during startup see https://github.com/librato/joboe/issues/675
            if (e.getCause() instanceof ClassNotFoundException) {
                logger.debug("Cannot load JMX impl class: " + e.getMessage() + " the class might be available later on", e);
                return Collections.emptyMap();
            }
            throw e;
        } finally {
            Thread.currentThread().setContextClassLoader(existingContextLoader); //reset back to whatever it was
        }
    }

    /**
     * Collects information from the JMX mBeans
     * @return  a Map with key as the attribute name/key and value as the numeric attribute values from the JMX mBeans
     */
    Map<MetricKey, Number> collect(MBeanServer mbs) throws IntrospectionException, InstanceNotFoundException, ReflectionException, AttributeNotFoundException, MBeanException {
        Map<MetricKey, Number> information = new LinkedHashMap<MetricKey, Number>();
        
        for (JMXScope scope : scopes) {
            logger.trace("Collecting JMX scope [" + scope + "]");
        
            try {
                @SuppressWarnings("unchecked")
                Set<ObjectName> names = mbs.queryNames(new ObjectName(scope.getObjectName()), null); //query a list of mBean names based on the input <code>ObjectName<code>
            
                Map<MetricKey, Number> entriesFromScope = new LinkedHashMap<MetricKey, Number>();
                for (ObjectName name : names) { //iterate through each mBean
        
                    logger.trace("Procesing MBean with ObjectName [" + name.toString() + "]");
        
                    List<String> targetAttributes;
                    
                    if (scope.getAttributes() == null) { //if no specific attributes have been defined, then find all attributes
                        targetAttributes = new ArrayList<String>();
                        
                        MBeanInfo info = mbs.getMBeanInfo(name);
                        for (MBeanAttributeInfo attributeInfo : info.getAttributes()) { //iterate through each attribute under the mBean
                            targetAttributes.add(attributeInfo.getName());
                        }
                    } else {
                        targetAttributes = scope.getAttributes();
                    }
                    
                    for (String targetAttribute : targetAttributes) { //iterate the target attribute by names
                        logger.trace("Processing MBean attribute with name [" + targetAttribute + "]");
                        try {
                            entriesFromScope.putAll(processMBeanAttribute(name, targetAttribute, mbs));
                        } catch (Exception e) {
                            logger.warn("JMX Attribute error [" + e.getClass().getName() + "]" + e.getMessage() + " from " + name.getCanonicalName());
                        }
                        
                    }
                }
                
                if (names.isEmpty()) {
                    logger.debug("Cannot find any mBeans under scope [" + scope.getObjectName() + "]");
                }
                
                if (information.size() + entriesFromScope.size() <= maxEntryCount) {
                    information.putAll(entriesFromScope);
                } else if (information.size() < maxEntryCount) {
                    List<MetricKey> mapKeys = new ArrayList<MetricKey>(entriesFromScope.keySet());
                    int remainingCapacity = maxEntryCount - information.size();
                    entriesFromScope.keySet().retainAll(mapKeys.subList(0, remainingCapacity));
                    information.putAll(entriesFromScope);
                    logger.warn("Stop collecting JXM entries as the max entries [" + maxEntryCount + "] has reached");
                } else {
                    logger.warn("Stop collecting JXM entries as the max entries [" + maxEntryCount + "] has reached");
                    break;
                }
            } catch (MalformedObjectNameException e) {
                logger.warn("[" + getClass().getName() + "]" + e.getMessage());
            } catch (NullPointerException e) {
                logger.warn("[" + getClass().getName() + "]" + e.getMessage());
            }
        }
        return information;
    }
    
   
    /**
     * Processes the mBean Attribute and returns a Map filled with keys and values. Take note that although the input is one parameter, the 
     * result could be variable in size.
     * 
     * For example, if the processing could not find the attribute, it might be an empty map. And if the attribute is a <code>CompositeData<code>, the result map might
     * contain multiple keys and values of the flattened composite data
     * 
     * Only value that is of `java.lang.Number` will be collected
     * 
     * @param mBeanName
     * @param attributeName
     * @param mbs 
     * @return keys and values after processing the mBean attribute. Take note that although the input is one parameter, the result could be variable in size.
     */
    private static Map<MetricKey, Number> processMBeanAttribute(ObjectName mBeanName, String attributeName, MBeanServer mbs) throws Exception {
        String prefix = (JMX_LABEL + "." + getMetricNameSegmentFromObjectName(mBeanName) + "." + attributeName).replace(' ', '_');
        
        AttributeProcessor processor = AttributeProcessorLocator.getProcessor(mBeanName, attributeName, mbs);

        Map<String, Object> processedData = processor.process(mBeanName, attributeName);
        
        Map<MetricKey, Number> result = new LinkedHashMap<MetricKey, Number>();
        
        for (Entry<String, Object> entry : processedData.entrySet()) {
            String metricName;
            if (entry.getKey().length() == 0) {
                metricName = prefix;
            } else {
                metricName = prefix + entry.getKey();
            }
            
            Object value = entry.getValue();
            if (value instanceof Number) {
                MetricKey metricKey = new MetricKey(metricName, getMetricTagsFromObjectName(mBeanName));
                logger.trace("Collected JMX metric entry [" + metricKey + "]");
                result.put(metricKey, (Number) value);
            } else {
                logger.debug("Skipping JMX entry [" + metricName + "] in metrics reporting, as its value [" + value + "] is not a number!");
            }
        }
               
        
        return result;
    }

    private static Map<String, String> getMetricTagsFromObjectName(ObjectName mBeanName) {
       Map<String, String> tags = new HashMap<String, String>(mBeanName.getKeyPropertyList());
       tags.remove("type");
       tags.remove("j2eeType");
       tags.remove("name");
       return tags;
    }

    private static String getMetricNameSegmentFromObjectName(ObjectName mBeanName) {
        String domain = mBeanName.getDomain();
        StringBuilder result = new StringBuilder();
        
        if (!DEFAULT_MBEAN_DOMAIN.equals(domain)) {
            result.append(domain);
        }
        
        String typeProperty = mBeanName.getKeyProperty("type");
        if (typeProperty == null) {
            typeProperty = mBeanName.getKeyProperty("j2eeType");
        }
        if (typeProperty != null) {
            appendSegment(result, typeProperty);
        }
        String nameProperty = mBeanName.getKeyProperty("name");
        if (nameProperty != null) {
            appendSegment(result, nameProperty);
        }
        
        return result.toString();
    }
    
    private static void appendSegment(StringBuilder existingBuilder, String newSegment) {
        if (existingBuilder.length() > 0) {
            existingBuilder.append('.');
        }
        existingBuilder.append(newSegment);
    }

    @Override
    List<? extends MetricsEntry<?>> collectMetricsEntries() throws IntrospectionException, InstanceNotFoundException, AttributeNotFoundException, ReflectionException, MBeanException {
        Map<MetricKey, Number> data = collect();
        List<SimpleMeasurementMetricsEntry> metricsEntries = new ArrayList<SimpleMeasurementMetricsEntry>();
        for (Entry<MetricKey, Number> entry : data.entrySet()) {
            metricsEntries.add(new SimpleMeasurementMetricsEntry(entry.getKey(), entry.getValue()));
        }
        return metricsEntries;
    }
    

   
}
