package com.tracelytics.monitor;

import com.tracelytics.joboe.config.ConfigContainer;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.joboe.config.InvalidConfigException;
import com.tracelytics.metrics.MetricKey;
import com.tracelytics.monitor.JMXCollector;
import org.junit.jupiter.api.Test;

import javax.management.*;
import javax.management.loading.ClassLoaderRepository;
import java.io.ObjectInputStream;
import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class JMXCollectorTest {
    /**
     * Test with valid config
     * @throws InvalidConfigException 
     */
    @Test
    public void testBuildCollector1() throws Exception {
        ConfigContainer configs = new ConfigContainer();
        
        configs.putByStringValue(ConfigProperty.MONITOR_JMX_SCOPES, "{\"java.lang:type=MemoryPool,*\":[\"Usage\"],\"java.lang:type=Memory\":[\"HeapMemoryUsage\",\"NonHeapMemoryUsage\"]}");
        
        JMXCollector collector = new JMXCollector(configs);
        
        Field maxEntryCountField = JMXCollector.class.getDeclaredField("maxEntryCount");
        maxEntryCountField.setAccessible(true);
        
        assertEquals(JMXCollector.DEFAULT_MAX_ENTRY_COUNT, maxEntryCountField.getInt(collector));
        
        Field scopesField = JMXCollector.class.getDeclaredField("scopes");
        scopesField.setAccessible(true);
        
        Map<String, List<String>> scopesByObjectName = new HashMap<String, List<String>>();
        for (JMXScope scope : (List<JMXScope>) scopesField.get(collector)) {
            scopesByObjectName.put(scope.getObjectName(), scope.getAttributes());
        }
        
        assertEquals(2, scopesByObjectName.size());
        
        assertTrue(scopesByObjectName.containsKey("java.lang:type=MemoryPool,*"));
        assertEquals(Arrays.asList("Usage"), scopesByObjectName.get("java.lang:type=MemoryPool,*"));
        
        assertTrue(scopesByObjectName.containsKey("java.lang:type=Memory"));
        assertEquals(Arrays.asList("HeapMemoryUsage", "NonHeapMemoryUsage"), scopesByObjectName.get("java.lang:type=Memory"));
    }
    
    /**
     * Test with config that disables the JMX monitoring
     * @throws InvalidConfigException 
     */
    @Test
    public void testBuildCollectors2() throws Exception {
        ConfigContainer configs = new ConfigContainer();
        
        configs.putByStringValue(ConfigProperty.MONITOR_JMX_MAX_ENTRY, "200");
        configs.putByStringValue(ConfigProperty.MONITOR_JMX_SCOPES, "{\"java.lang:type=MemoryPool,*\":[\"Usage\"],\"java.lang:type=Memory\":[\"HeapMemoryUsage\",\"NonHeapMemoryUsage\"]}");
        
        JMXCollector collector = new JMXCollector(configs);
        
        Field maxEntryCountField = JMXCollector.class.getDeclaredField("maxEntryCount");
        maxEntryCountField.setAccessible(true);
        
        assertEquals(200, maxEntryCountField.getInt(collector));
    }
    
    /**
     * Test with config with invalid values
     */
    @Test
    public void testBuildCollectors3() throws Exception {
        ConfigContainer configs = new ConfigContainer();
        
        configs.putByStringValue(ConfigProperty.MONITOR_JMX_MAX_ENTRY, "-1");
        configs.putByStringValue(ConfigProperty.MONITOR_JMX_SCOPES, "{\"java.lang:type=MemoryPool,*\":[\"Usage\"],\"java.lang:type=Memory\":[\"HeapMemoryUsage\",\"NonHeapMemoryUsage\"]}");

        try {
            JMXCollector collector = new JMXCollector(configs);
            fail("Expect " + InvalidConfigException.class.getName());
        } catch (InvalidConfigException e) {
            //expected
        }
    }
    
    /**
     * Test with config with invalid scope
     */
    @Test
    public void testBuildCollectors4() throws Exception {
        ConfigContainer configs = new ConfigContainer();
        
        configs.putByStringValue(ConfigProperty.MONITOR_JMX_SCOPES, "{"); //invalid json format

        try {
            JMXCollector collector = new JMXCollector(configs);
            fail("Expect " + InvalidConfigException.class.getName());
        } catch (InvalidConfigException e) {
            //expected
        }
    }
    
    /**
     * Tests JMXCollector with valid scopes
     * @throws InvalidConfigException 
     */
    @Test
    public void testCollectInformation1() throws InvalidConfigException {
        ConfigContainer configs = new ConfigContainer();
        
        //configs.putByStringValue(ConfigProperty.MONITOR_JMX_SCOPES, "java.lang:type=MemoryPool,name=PS Eden Space[Usage];java.lang:type=Memory[HeapMemoryUsage,NonHeapMemoryUsage]");
        configs.putByStringValue(ConfigProperty.MONITOR_JMX_SCOPES, "{\"java.lang:type=MemoryPool,*\":[\"Usage\"],\"java.lang:type=Memory\":[\"HeapMemoryUsage\",\"NonHeapMemoryUsage\"]}");
        
        
        JMXCollector collector = new JMXCollector(configs);
        
        try {
            Map<MetricKey, Number> information = collectAll(collector);
            
//            assertTrue(information.containsKey("JMX.java.lang:type=MemoryPool,name=PS_Eden_Space.Usage.committed"));
//            assertFalse(information.containsKey("JMX.java.lang:type=MemoryPool,name=PS_Survivor_Space.Usage.committed"));
//            assertTrue(information.containsKey("JMX.java.lang:type=Memory.NonHeapMemoryUsage.committed"));
//            assertFalse(information.containsKey("JMX.java.lang:type=OperatingSystem.ProcessCpuLoad"));
            assertFalse(information.isEmpty()); //data returned could be different JVM from JVM and also newer jre 1.7 version seems to use different name of MemoryPoll (Eden Space VS PS Eden Space)
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Tests JMXCollector with no scopes
     */
    @Test
    public void testCollectInformation2() {
        try {
            JMXCollector collector = new JMXCollector(new ConfigContainer()); //test defaults
            
            fail("Except InvalidConfigException to be thrown. Empty scope is not allowed");
        } catch (InvalidConfigException e) {
            //expected
        }
    }
    
    /**
     * Tests JMXCollector with Invalid scopes
     * @throws InvalidConfigException 
     */
    @Test
    public void testCollectInformation3() throws InvalidConfigException {
        ConfigContainer configs = new ConfigContainer();
        
        configs.putByStringValue(ConfigProperty.MONITOR_JMX_SCOPES, "{\"XYZ;ZZZ\":\"*\"}");
        
        JMXCollector collector = new JMXCollector(configs);
        try {
            Map<MetricKey, Number> information = collectAll(collector);
            
            assertTrue(information.isEmpty());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Tests JMXCollector with wild card
     * @throws InvalidConfigException 
     */
    @Test
    public void testCollectInformation4() throws InvalidConfigException {
        ConfigContainer configs = new ConfigContainer();
        
        configs.putByStringValue(ConfigProperty.MONITOR_JMX_SCOPES, "{\"*:*\":\"*\"}");
        
        JMXCollector collector = new JMXCollector(configs);
        try {
            Map<MetricKey, Number> information = collectAll(collector);
            
            assertFalse(information.isEmpty());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Test with default scopes
     * @throws Exception
     */
    @Test
    public void testCollectInformation5() throws Exception {
        ConfigContainer configs = new ConfigContainer();
        
        //configs.putByStringValue(ConfigProperty.MONITOR_JMX_SCOPES, "java.lang:type=MemoryPool,name=PS Eden Space[Usage];java.lang:type=Memory[HeapMemoryUsage,NonHeapMemoryUsage]");
        configs.putByStringValue(ConfigProperty.MONITOR_JMX_SCOPES, "{" +
                "\"java.lang:type=MemoryPool,*\":[\"Usage\"]," +
                "\"java.lang:type=Memory\":[\"HeapMemoryUsage\", \"NonHeapMemoryUsage\"]," +
                "\"java.lang:type=GarbageCollector,*\":[\"CollectionTime\"]," +
                "\"java.lang:type=Threading\":[\"ThreadCount\"]," +
                "\"java.lang:type=OperatingSystem\":[\"ProcessCpuTime\", \"AvailableProcessors\", \"ProcessCpuLoad\"]," +
                "\"java.lang:type=Runtime,*\":[\"Uptime\"]" +
        		"}");
        
        MetricsInfo[] definedMetricsInfo = new MetricsInfo[] {
            new MetricsInfo("MemoryPool", "Usage"),
            new MetricsInfo("Memory", "HeapMemoryUsage", "NonHeapMemoryUsage"),
            new MetricsInfo("GarbageCollector", "CollectionTime"),
            new MetricsInfo("Threading", "ThreadCount"),
            new MetricsInfo("OperatingSystem", "ProcessCpuTime", "AvailableProcessors", "ProcessCpuLoad"),
            new MetricsInfo("Runtime", "Uptime")
        };
        
        
        JMXCollector collector = new JMXCollector(configs);
        Map<MetricKey, Number> collectedMetrics = collectAll(collector);
        
        assertCollectedMetrics(collectedMetrics, definedMetricsInfo);
    }
    
    /**
     * Test with hitting maxEntryCount
     * @throws Exception
     */
    @Test
    public void testMaxEntryCount() throws Exception {
        ConfigContainer configs = new ConfigContainer();
        
        //configs.putByStringValue(ConfigProperty.MONITOR_JMX_SCOPES, "java.lang:type=MemoryPool,name=PS Eden Space[Usage];java.lang:type=Memory[HeapMemoryUsage,NonHeapMemoryUsage]");
        configs.putByStringValue(ConfigProperty.MONITOR_JMX_MAX_ENTRY, "250");
        configs.putByStringValue(ConfigProperty.MONITOR_JMX_SCOPES, "{" +
                "\"test:type=A,*\":\"*\"," +
                "\"test:type=B,*\":\"*\"," +
                "\"test:type=C,*\":\"*\"," +
                "\"test:type=D,*\":\"*\"," +
                "\"test:type=E,*\":\"*\"," +
                "\"test:type=F,*\":\"*\"," +
                "}");
        
        JMXCollector collector = new JMXCollector(configs);
        Map<MetricKey, Number> collectedMetrics = collector.collect(new TestMBeanServer(100));
        
        assertEquals(250, collectedMetrics.size());
    }
    
   
    private Map<MetricKey, Number> collectAll(JMXCollector collector) throws IntrospectionException, InstanceNotFoundException, AttributeNotFoundException, ReflectionException, MBeanException {
        return collector.collect();
    }
    
    /**
     * Assert the the metrics collected matches the metricsInfo defined
     * @param collectedMetrics
     * @param definedMetricsInfo
     */
    private void assertCollectedMetrics(Map<MetricKey, Number> collectedMetrics, MetricsInfo[] definedMetricsInfo) {
        for (MetricsInfo definedMetricsEntry : definedMetricsInfo) {
            for (String definedAttribute : definedMetricsEntry.attributes) {
                boolean found = false;
                for (MetricKey key : collectedMetrics.keySet()) {
                    String stringKey = key.getStringKey();
                    
                    String expectedPrefix = JMXCollector.JMX_LABEL + ".";
                    stringKey = stringKey.substring(expectedPrefix.length()); //trim the first part out
                    
                    if (stringKey.contains(definedAttribute)) {
                        found = true;
                    }
                }
                assertTrue(found);
            }
        }
    }
    
    private class MetricsInfo {
        private String type;
        private String[] attributes;
        
        private MetricsInfo(String type, String...attributes) {
            this.type = type;
            this.attributes = attributes;
        }
        
    }
    
    private class TestMBeanServer implements MBeanServer {
        private int entriesPerObjectName;
        public TestMBeanServer(int entriesPerObjectName) {
            this.entriesPerObjectName = entriesPerObjectName;
        }
        

        public ObjectInstance createMBean(String className, ObjectName name) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException {
            // TODO Auto-generated method stub
            return null;
        }

        public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException {
            // TODO Auto-generated method stub
            return null;
        }

        public ObjectInstance createMBean(String className, ObjectName name, Object[] params, String[] signature) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException {
            // TODO Auto-generated method stub
            return null;
        }

        public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName, Object[] params, String[] signature) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException {
            // TODO Auto-generated method stub
            return null;
        }

        public ObjectInstance registerMBean(Object object, ObjectName name) throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
            // TODO Auto-generated method stub
            return null;
        }

        public void unregisterMBean(ObjectName name) throws InstanceNotFoundException, MBeanRegistrationException {
            // TODO Auto-generated method stub
            
        }

        public ObjectInstance getObjectInstance(ObjectName name) throws InstanceNotFoundException {
            // TODO Auto-generated method stub
            return null;
        }

        public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) {
            // TODO Auto-generated method stub
            return null;
        }

        public Set<ObjectName> queryNames(ObjectName name, QueryExp query) {
            return Collections.singleton(name);
        }

        public boolean isRegistered(ObjectName name) {
            // TODO Auto-generated method stub
            return false;
        }

        public Integer getMBeanCount() {
            // TODO Auto-generated method stub
            return null;
        }

        public Object getAttribute(ObjectName name, String attribute) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException {
            return Integer.valueOf(attribute);
        }

        public AttributeList getAttributes(ObjectName name, String[] attributes) throws InstanceNotFoundException, ReflectionException {
            // TODO Auto-generated method stub
            return null;
        }

        public void setAttribute(ObjectName name, Attribute attribute) throws InstanceNotFoundException, AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
            // TODO Auto-generated method stub
            
        }

        public AttributeList setAttributes(ObjectName name, AttributeList attributes) throws InstanceNotFoundException, ReflectionException {
            // TODO Auto-generated method stub
            return null;
        }

        public Object invoke(ObjectName name, String operationName, Object[] params, String[] signature) throws InstanceNotFoundException, MBeanException, ReflectionException {
            // TODO Auto-generated method stub
            return null;
        }

        public String getDefaultDomain() {
            // TODO Auto-generated method stub
            return null;
        }

        public String[] getDomains() {
            // TODO Auto-generated method stub
            return null;
        }

        public void addNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException {
            // TODO Auto-generated method stub
            
        }

        public void addNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException {
            // TODO Auto-generated method stub
            
        }

        public void removeNotificationListener(ObjectName name, ObjectName listener) throws InstanceNotFoundException, ListenerNotFoundException {
            // TODO Auto-generated method stub
            
        }

        public void removeNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, ListenerNotFoundException {
            // TODO Auto-generated method stub
            
        }

        public void removeNotificationListener(ObjectName name, NotificationListener listener) throws InstanceNotFoundException, ListenerNotFoundException {
            // TODO Auto-generated method stub
            
        }

        public void removeNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, ListenerNotFoundException {
            // TODO Auto-generated method stub
            
        }

        public MBeanInfo getMBeanInfo(ObjectName name) throws InstanceNotFoundException, IntrospectionException, ReflectionException {
            MBeanAttributeInfo[] attributeInfo = new MBeanAttributeInfo[entriesPerObjectName];
            for (int i = 0 ; i < entriesPerObjectName ; i++) {
                attributeInfo[i] = new MBeanAttributeInfo(String.valueOf(i), "int", "test-attribute-" + i, true, true, false);
            }
            return new MBeanInfo(name.toString(), name.toString(), attributeInfo, null, null, null); 
        }

        public boolean isInstanceOf(ObjectName name, String className) throws InstanceNotFoundException {
            // TODO Auto-generated method stub
            return false;
        }

        public Object instantiate(String className) throws ReflectionException, MBeanException {
            // TODO Auto-generated method stub
            return null;
        }

        public Object instantiate(String className, ObjectName loaderName) throws ReflectionException, MBeanException, InstanceNotFoundException {
            // TODO Auto-generated method stub
            return null;
        }

        public Object instantiate(String className, Object[] params, String[] signature) throws ReflectionException, MBeanException {
            // TODO Auto-generated method stub
            return null;
        }

        public Object instantiate(String className, ObjectName loaderName, Object[] params, String[] signature) throws ReflectionException, MBeanException, InstanceNotFoundException {
            // TODO Auto-generated method stub
            return null;
        }

        public ObjectInputStream deserialize(ObjectName name, byte[] data) throws InstanceNotFoundException, OperationsException {
            // TODO Auto-generated method stub
            return null;
        }

        public ObjectInputStream deserialize(String className, byte[] data) throws OperationsException, ReflectionException {
            // TODO Auto-generated method stub
            return null;
        }

        public ObjectInputStream deserialize(String className, ObjectName loaderName, byte[] data) throws InstanceNotFoundException, OperationsException, ReflectionException {
            // TODO Auto-generated method stub
            return null;
        }

        public ClassLoader getClassLoaderFor(ObjectName mbeanName) throws InstanceNotFoundException {
            // TODO Auto-generated method stub
            return null;
        }

        public ClassLoader getClassLoader(ObjectName loaderName) throws InstanceNotFoundException {
            // TODO Auto-generated method stub
            return null;
        }

        public ClassLoaderRepository getClassLoaderRepository() {
            // TODO Auto-generated method stub
            return null;
        }
        
    }
   
}
