package com.tracelytics.instrumentation.solr;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.ClassInstrumentation;

/**
 * Wraps SolrCore to provide convenient methods to get shard id and collection name if the core supports Cloud model. Take note that Solr Cloud was introduced since
 * Solr 4.0. If the core does not support it, no modification would be done to the SolrCore class
 * 
 * @author Patson Luk
 *
 */
public class SolrCoreInstrumentation extends ClassInstrumentation {

    @Override
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        
        CtClass coreDescriptionClass = classPool.get("org.apache.solr.core.CoreDescriptor");
        
        if (coreDescriptionClass != null) {
            try {
                coreDescriptionClass.getMethod("getCloudDescriptor", "()Lorg/apache/solr/cloud/CloudDescriptor;");
            } catch (NotFoundException e) {
                logger.info("Cannot find getCloudDescriptor() from Solr CoreDescriptor. Old version that does not support cloud");
                return false;
            }
        }
        
        cc.addMethod(CtNewMethod.make("public String tvGetShardId() { " +
        		                      "    if (getCoreDescriptor() != null && getCoreDescriptor().getCloudDescriptor() != null) { " +
        		                      "        return getCoreDescriptor().getCloudDescriptor().getShardId();" +
        		                      "    } else {" +
        		                      "        return null;" +
        		                      "    }" +
        		                      "}", cc));
        
        cc.addMethod(CtNewMethod.make("public String tvGetCollectionName() { " +
                                      "    if (getCoreDescriptor() != null && getCoreDescriptor().getCloudDescriptor() != null) { " +
                                      "        return getCoreDescriptor().getCloudDescriptor().getCollectionName();" +
                                      "    } else {" +
                                      "        return null;" +
                                      "    }" +
                                      "}", cc));
        
        tagInterface(cc, SolrCloudAwareCore.class.getName());
            
        return true;
    }
}
