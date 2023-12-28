package com.solarwinds.joboe.metrics.jmx.processor;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;

/**
 * A CPU time attribute processor that is more forgiving when the attribute is missing. Such an attribute is not found in IBM 1.5 jre
 * 
 * 
 * @author Patson Luk
 *
 */
class OptionalAttributeProcessor extends AttributeProcessor  {
    private static OptionalAttributeProcessor reference;
    
    
    
    private OptionalAttributeProcessor(MBeanServer mBeanServer) {
        super(mBeanServer);
    }
    
    static synchronized OptionalAttributeProcessor getInstance(MBeanServer mBeanServer) {
        if (reference == null) {
            reference = new OptionalAttributeProcessor(mBeanServer);
        }

        return reference;
    }
    
    
    @Override
    /**
     * Attempt to retrieve the CPU time information from MBeanServer. If not available, simply display debug message and return
     */
    public Map<String, Object> process(ObjectName mBeanName, String attributeName) throws InstanceNotFoundException, ReflectionException, MBeanException {
        Map<String, Object> values = new LinkedHashMap<String, Object>();
    
        Object attributeObj;
        try {
            attributeObj = mBeanServer.getAttribute(mBeanName, attributeName);
            appendAttribute(attributeObj, "", values);
            
        } catch (AttributeNotFoundException e) {
            //Ok as some JVM (IBM 5.0) does not provide this value
            logger.debug("Cannot load attribute [" + mBeanName.getCanonicalName() + "]. This is expected as some JVMs might not publish this attribute. Skipping...");
        }
        
        
        return values;
    }
}
