package com.tracelytics.instrumentation.nosql;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.ext.javassist.NotFoundException;

/**
 * Tags the RPC requests which provides Region information
 * 
 * @author Patson Luk
 *
 */
public class HbaseRpcRequestPatcher extends HbaseBaseInstrumentation {
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        
        
        try {
          //we only interested in those declared with ClientProtos
            CtClass clientProtoClass = classPool.get("org.apache.hadoop.hbase.protobuf.generated.ClientProtos");
            if (clientProtoClass.equals(cc.getDeclaringClass())) {
                try {
                    cc.getMethod("getRegion", "()Lorg/apache/hadoop/hbase/protobuf/generated/HBaseProtos$RegionSpecifier;");
                    cc.addMethod(CtNewMethod.make("public String getTvRegionInfo() { " +
                    		                      "    return getRegion() != null && " +
                    		                      "           getRegion().getValue() != null && " +
                    		                      "           getRegion().getValue().isValidUtf8() ? " +
                    		                      "           getRegion().getValue().toStringUtf8() : null; " +
                    		                      "}", cc));
                    tagInterface(cc, HbaseObjectWithRegionInfo.class.getName());
                    return true;
                } catch (NotFoundException e) {
                    logger.debug("There is no getRegion method in the hbase request class [" + cc.getName() + "]. Region info will not be captured for this class");
                }
            }
        } catch (NotFoundException e) {
            //ok, any other non hbase framework can use this google class
        }
        
        return false;
    }
}
