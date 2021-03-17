package com.tracelytics.monitor.jmx.processor;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.RuntimeMBeanException;

/**
 * Generic attribute processor for attribute values that do not need any special processing. Simply retrieve the value from MBeanServer and append it to the map
 * @author Patson Luk
 *
 */
class GenericAttributeProcessor extends AttributeProcessor  {
    
    public GenericAttributeProcessor(MBeanServer mBeanServer) {
        super(mBeanServer);
    }

    public Map<String, Object> process(ObjectName mBeanName, String attributeName) throws InstanceNotFoundException, AttributeNotFoundException, ReflectionException, MBeanException {
        Map<String, Object> values = new LinkedHashMap<String, Object>();
    
        Object attributeObj;
        try {
            attributeObj = mBeanServer.getAttribute(mBeanName, attributeName);
            appendAttribute(attributeObj, "", values);
        } catch (RuntimeMBeanException e) {
            if (e.getCause() != null && e.getCause() instanceof UnsupportedOperationException) { 
                //then it is OK, since some environment might not support certain properties even though they are defined in the MBean info
            } else {
                throw e;
            }
        }
        
        
        return values;
    }
    
   
}
