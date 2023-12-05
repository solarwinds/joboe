package com.tracelytics.monitor;

import java.util.Arrays;
import java.util.List;

/**
 * Defines the Scope of the JMX collection. It consists of the MBean name or pattern in the form of <code>ObjectName</code> and a list of attribute names.
 * <p>
 * Take note that if attribute names are not provided, it will look for all the attributes under the input MBean name or pattern
 * @author Patson Luk
 *
 */
public class JMXScope {
    private String objectName;
    private String[] attributes;
    
    /**
     * 
     * @param objectName    mBean name or pattern 
     * @param attributes    list of attributes to be targeted for JMX. If not provided, it will be interpreted as getting all attributes
     */
    JMXScope(String objectName, String...attributes) {
        this.objectName = objectName;
        this.attributes = attributes;
    }
    
    String getObjectName() {
        return objectName;
    }
    
    List<String> getAttributes() {
        if (attributes == null || attributes.length == 0) {
            return null;
        } else {
            return Arrays.asList(attributes);
        }
    }
    
    @Override
    public String toString() {
        StringBuffer result = new StringBuffer(objectName);
        if (attributes != null && attributes.length > 0) {
            result.append("[");
            for (String attribute : attributes) {
                result.append(attribute + ",");
            }
            
            result.delete(result.length() - 1, result.length()); //trim the trailing comma
            result.append("]");
        }
        return result.toString();
    }
    
    
}