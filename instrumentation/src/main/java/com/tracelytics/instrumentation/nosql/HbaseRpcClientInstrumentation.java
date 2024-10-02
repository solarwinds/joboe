package com.tracelytics.instrumentation.nosql;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;
import com.tracelytics.joboe.Metadata;

/**
 * Instruments RPC Client which is used by later version of HBase 0.95+ to manage request traffic. We can extract crucial information such as region and remote
 * address here
 * 
 * @author Patson Luk
 *
 */
public class HbaseRpcClientInstrumentation extends HbaseBaseInstrumentation {
    private static final String CLASS_NAME = HbaseRpcClientInstrumentation.class.getName();

    private static List<MethodMatcher<Type>> methodMatchers = new ArrayList<MethodMatcher<Type>>();
    private static ThreadLocal<Boolean> activeClientSpan = new ThreadLocal<Boolean>();
    
    private static enum Type { CALL_0_96, CALL_0_99, CALL_1 }
        
    static {
        methodMatchers.add(new MethodMatcher<Type>("call", new String[] { "com.google.protobuf.Descriptors$MethodDescriptor", "com.google.protobuf.Message", "java.lang.Object", "java.lang.Object", "java.lang.Object", "java.net.InetSocketAddress"}, "java.lang.Object", Type.CALL_0_96)); //version 0.96 or before
        methodMatchers.add(new MethodMatcher<Type>("call", new String[] { "org.apache.hadoop.hbase.ipc.PayloadCarryingRpcController", "com.google.protobuf.Descriptors$MethodDescriptor", "com.google.protobuf.Message", "java.lang.Object", "java.lang.Object", "java.lang.Object", "java.net.InetSocketAddress"}, "java.lang.Object", Type.CALL_0_99)); //version 0.99
        methodMatchers.add(new MethodMatcher<Type>("call", new String[] { "org.apache.hadoop.hbase.ipc.PayloadCarryingRpcController", "com.google.protobuf.Descriptors$MethodDescriptor", "com.google.protobuf.Message", "java.lang.Object", "java.lang.Object", "java.net.InetSocketAddress"}, "java.lang.Object", Type.CALL_1)); //version 1.0 +
        
    }
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        
        for (Entry<CtMethod, Type> entry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            CtMethod method = entry.getKey();
            Type type = entry.getValue();
            String descriptorToken;
            String messageToken;
            String addressToken;
            if (type == Type.CALL_0_96) {
                descriptorToken = "$1";
                messageToken = "$2";
                addressToken = "$6";
            } else if (type == Type.CALL_0_99) {
                descriptorToken = "$2";
                messageToken = "$3";
                addressToken = "$7";
            } else {
                descriptorToken = "$2";
                messageToken = "$3";
                addressToken = "$6";
            }
            
            insertBefore(method,
                     "String methodName = null;" +
                     "if (" + descriptorToken + " != null) {" +
                     "    methodName = " + descriptorToken + ".getName();" +
                     "}" +
                     //iterate through the multiRequest for all the region information
                     "if (" + messageToken + " instanceof org.apache.hadoop.hbase.protobuf.generated.ClientProtos.MultiRequest) {" +
                     "    java.util.List actionList = ((org.apache.hadoop.hbase.protobuf.generated.ClientProtos.MultiRequest)" + messageToken + ").getRegionActionList();" +
                     "    int actionCount = 0;" +
                     "    for (int i = 0; i < actionList.size(); i++) {" +
                     "        if (actionList.get(i) instanceof org.apache.hadoop.hbase.protobuf.generated.ClientProtos.RegionAction) {" +
                     "            actionCount += ((org.apache.hadoop.hbase.protobuf.generated.ClientProtos.RegionAction)actionList.get(i)).getActionCount();" +
                     "        }" +
                     "    }" +
                          CLASS_NAME + ".layerEntry(" + addressToken + ", methodName, actionList, Integer.valueOf(actionCount));" +
                     "} else {" +
                          CLASS_NAME + ".layerEntry(" + addressToken + ", methodName, " + messageToken + ");" +
                     "}"
                );
            insertAfter(method, CLASS_NAME + ".layerExit();", true);
        }

        return true; 
    }
    
    public static void layerEntry(InetSocketAddress socket, String methodName, Object param) {
        layerEntry(socket, methodName, Collections.singletonList(param), null);
    }

    public static void layerEntry(InetSocketAddress socket, String methodName, List<?> params, Integer actionCount) {
        if (isScannerCall(methodName) && getCurrentInstrumentedScannerOperation() == null) { //for scanner operation, only continue on if there's an active scanner operation instrumented
            return;
        }
        
        Event event = Context.createEvent();
        event.addInfo("Layer", LAYER_NAME,
                      "Flavor", FLAVOR,
                      "ClientMethod", methodName);
        
        if (socket.getAddress() != null) {
            //event.addInfo("RemoteHost", socket.getAddress().getHostAddress() + ":" + socket.getPort());
            event.addInfo("RemoteHost", socket.getAddress().getHostAddress());
            event.addInfo("RemotePort", socket.getPort());
        }
        
        List<String> allRegionInfo = new ArrayList<String>();
        for (Object param : params) {
            if (param instanceof HbaseObjectWithRegionInfo) { 
                String regionInfo = ((HbaseObjectWithRegionInfo)param).getTvRegionInfo();
                if (regionInfo != null && !"".equals(regionInfo)) {
                    allRegionInfo.add(regionInfo);
                }
            }
        }
        
        
        if (allRegionInfo.size() == 1) { //insert this as String if there is only one region
            event.addInfo("RegionInfo", allRegionInfo.get(0));
        } else if (allRegionInfo.size() > 1) { //insert as String[] if there are multiple regions
            event.addInfo("RegionInfo", allRegionInfo.toArray(new String[0]));
        }
                
        if (actionCount != null) {
            event.addInfo("ActionCount", actionCount);
        }
        
        if (!hasActiveExtent()) {
        	event.addInfo("Label", "entry");
            event.report();
            activeClientSpan.set(true);
        } else { //then only report the info event
            event.addInfo("Label", "info");
            event.report();
        }

    }
    
    /**
     * Identifies whether this client request is triggered by scanner calls by checking the method name
     * @param methodName
     * @return
     */
    private static boolean isScannerCall(String methodName) {
        return "Scan".equals(methodName);
    }

    public static void layerExit() {
    	if (activeClientSpan.get() == Boolean.TRUE) {
	        Event event = Context.createEvent();
	        event.addInfo("Layer", LAYER_NAME,
	                      "Label", "exit");
	        event.report();
	        
	        activeClientSpan.remove();
    	}
    }
}
