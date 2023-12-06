package com.solarwinds.monitor.jmx.processor;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;

/**
 * A CPU load attribute processor that computes the CPU load if such an attribute is not supported by the system. CPU load is only support in Java 1.7+
 * 
 * Take note that this should be a singleton as it is stateful to a specific JVM instance (process).
 * 
 * @author Patson Luk
 *
 */
class CpuLoadAttributeProcessor extends AttributeProcessor  {
    private static CpuLoadAttributeProcessor reference;
    
    
    private long previousProcessCpuTime = 0;
    private long previousUpTime = 0; 
    
    private CpuLoadAttributeProcessor(MBeanServer mBeanServer) {
        super(mBeanServer);
    }
    
    static synchronized CpuLoadAttributeProcessor getInstance(MBeanServer mBeanServer) {
        if (reference == null) {
            reference = new CpuLoadAttributeProcessor(mBeanServer);
        }

        return reference;
    }
    
    
    @Override
    /**
     * Attempt to retrieve the CPU load information from MBeanServer. If not available, try to compute the value.
     */
    public Map<String, Object> process(ObjectName mBeanName, String attributeName) throws InstanceNotFoundException, ReflectionException, MBeanException {
        Map<String, Object> values = new LinkedHashMap<String, Object>();
    
        Object attributeObj;
        try {
            attributeObj = mBeanServer.getAttribute(mBeanName, attributeName);
            appendAttribute(attributeObj, "", values);
            
            //System.out.println("Compared with computation result: " + computeCpuLoad()); //just for comparison!
            
        } catch (AttributeNotFoundException e) {
            //try to compute the CPU load if the attribute is not available in the mBean as it is only supported in java 1.7+
            Double cpuLoad = computeCpuLoad();
            if (cpuLoad != null) {
                appendAttribute(cpuLoad, "", values);
            }
        }        
        
        return values;
    }
    
    /**
     * Compute the Cpu load using logic in JConsole source. Referenced from {@link http://knight76.blogspot.ca/2009/05/how-to-get-java-cpu-usage-jvm-instance.html}
     * 
     * @return Cpu load from 0 to 1. Null if the computation is not successful
     */
    private synchronized Double computeCpuLoad() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        
        int availableProcessorCount = osBean.getAvailableProcessors();
        
        try {
            ObjectName objectName = new ObjectName("java.lang:type=OperatingSystem");
            Object processCpuTimeObj;
            
            try {
                processCpuTimeObj= mBeanServer.getAttribute(objectName, "ProcessCpuTime");
            } catch (AttributeNotFoundException e) {
                //Ok as some JVM (IBM 5.0) does not provide this value
                logger.debug("Cannot compute CPU load as ProcessCpuTime is not available");
                return null;
            }
            
            if (processCpuTimeObj != null) {
                long elapsedCpu = (Long)processCpuTimeObj - previousProcessCpuTime;
    
                previousProcessCpuTime = (Long)processCpuTimeObj;
                
                long elapsedUpTime = runtimeBean.getUptime() - previousUpTime;
    
                previousUpTime = runtimeBean.getUptime();
                
//                System.out.println("Process: " + elapsedCpu + " Uptime: " + elapsedUpTime + " Processor count: " + availableProcessorCount);
                
                
                if (elapsedUpTime > 0L) {
                    BigDecimal cpuUsage = new BigDecimal(elapsedCpu / (elapsedUpTime * 1000000.0 * availableProcessorCount)); 
                    
                    cpuUsage = cpuUsage.setScale(4, RoundingMode.HALF_UP);
                    
                    return cpuUsage.doubleValue();
                }
            }
        
        } catch (Exception e) {
            logger.warn("Unexpected error while computing CPU load: " + e.getMessage(), e);
        }
        
        return null;
    }
    
   
}
