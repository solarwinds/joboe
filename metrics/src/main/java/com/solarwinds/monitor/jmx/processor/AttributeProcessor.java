package com.solarwinds.monitor.jmx.processor;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import com.solarwinds.logging.Logger;
import com.solarwinds.logging.LoggerFactory;

/**
 * Base class of processing an attribute. Processors are responsible to process the JMX attribute and convert it into a map filled with keys and values.
 * 
 * The concrete child class implements the actual logic and behavior of the processing
 * 
 * @author Patson Luk
 *
 */
public abstract class AttributeProcessor {
    protected static final Logger logger = LoggerFactory.getLogger();
        
    protected MBeanServer mBeanServer;
    
    public AttributeProcessor(MBeanServer mBeanServer) {
        super();
        this.mBeanServer = mBeanServer;
    }

    /**
     * Appends an attribute object to the input container map, take note that the object will be flattened if it is of type <code>CompositeData</code>, <code>TabularData</code> 
     * or arrays.
     * <p>
     * Otherwise the data would be converted to its String value and stored into the input container map
     * 
     * @param attributeObj  the value to be inserted into this container
     * @param prefix        prefix of the attribute
     * @param values        the container map that stores the result
     */
    protected void appendAttribute(Object attributeObj, String prefix, Map<String, Object> values) {
            
        if (attributeObj == null) {
            values.put(prefix, attributeObj);
        } else if (attributeObj instanceof CompositeData) {
            handleCompositeData((CompositeData) attributeObj, prefix, values);
        } else if (attributeObj instanceof TabularData) {
            //for tabular data, we will insert the indices/keys as [index1,index2,index3]
            //for example, if the index is "PS Survivor Space", then the full entry would be something like
            //JMX.java.lang:type=GarbageCollector,name=PS_Scavenge.LastGcInfo.memoryUsageAfterGc[PS_Survivor_Space].value.committed = 2555904
            
            TabularData tabularData = (TabularData) attributeObj;
            Set<?> keys = tabularData.keySet();

            for (Object key : keys) {
                Object[] indices = ((List<?>) key).toArray();

                StringBuffer indexString = new StringBuffer();
                
                for (Object index : indices) {
                    if (indexString.length() == 0) {
                        indexString.append(index.toString().replace(' ', '_'));
                    } else {
                        indexString.append("," + index.toString().replace(' ', '_'));
                    }
                }

                CompositeData compositeData = tabularData.get(indices);
                handleCompositeData(compositeData, prefix + "[" + indexString.toString() + "]", values);
            }
        } else if (attributeObj.getClass().isArray()) {
            //for array data, we will insert the key as [arrayIndex] for example Threading.thread[2].id = 2
            for (int i = 0; i < Array.getLength(attributeObj); i++) { //We cannot cast directly to Object[] as it would fail on primitive arrays
                Object element = Array.get(attributeObj, i);
//                System.out.println("Index : " + i);
                appendAttribute(element, prefix + "[" + i + "]", values);
            }
        } else if (attributeObj instanceof Number){ //simple data type
            values.put(prefix, attributeObj);
        } else {
            values.put(prefix, attributeObj.toString());
        }
    }

    private void handleCompositeData(CompositeData compositeData, String prefix, Map<String, Object> values) {
        @SuppressWarnings("unchecked")
        Set<String> keys = compositeData.getCompositeType().keySet();
    
        for (String key : keys) {
            appendAttribute(compositeData.get(key), prefix + "." + key.replace(' ', '_'), values);
        }
            
    }
    
    /**
     * Processes the JMX attribute and convert it into a map filled with keys and values.
     * @param mBeanName     the mBeanName as ObjectName
     * @param attributeName the name of the attribute
     * @return  a map filled with information by processing the input attribute
     */
    public abstract Map<String, Object> process(ObjectName mBeanName, String attributeName) throws Exception;

}