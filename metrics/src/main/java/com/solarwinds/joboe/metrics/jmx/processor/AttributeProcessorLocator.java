package com.solarwinds.joboe.metrics.jmx.processor;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;

/**
 * This locator holds the information of <code>AttributeProcessor</code> assignments. 
 * <p>
 * Based on different mBean and attribute, this locator will return the corresponding <code>AttributeProcessor</code> 
 * @author Patson Luk
 *
 */
public class AttributeProcessorLocator {
    protected static final Logger logger = LoggerFactory.getLogger();
    
    private static ObjectName OS_OBJECT_NAME;
    
    static {
        try {
            OS_OBJECT_NAME= new ObjectName("java.lang:type=OperatingSystem");
        } catch (MalformedObjectNameException e) {
            logger.warn(e.getMessage());
        } catch (NullPointerException e) {
            logger.warn(e.getMessage());
        }
    }
    
    private AttributeProcessorLocator() {
    }
    
    /**
     * 
     * @param mBeanName     mBean name as ObjectName
     * @param attributeName attribute to be processed
     * @param mbs 
     * @return              AttributeProcessor located based on the inputs
     */
    public static AttributeProcessor getProcessor(ObjectName mBeanName, String attributeName, MBeanServer mbs) {
        if (OS_OBJECT_NAME.equals(mBeanName) && attributeName.equals("ProcessCpuLoad")) { //special processor for java.lang:type=OperatingSystem[ProcessCpuLoad]
            return CpuLoadAttributeProcessor.getInstance(mbs);
        } else if (OS_OBJECT_NAME.equals(mBeanName) && attributeName.equals("ProcessCpuTime")) { //IBM 1.5 does not have this attribute, so this attribute is optional (should not trigger error message if missing)
            return OptionalAttributeProcessor.getInstance(mbs);
        } else {
            return new GenericAttributeProcessor(mbs);
        }
    }
    
    
    
    
}
