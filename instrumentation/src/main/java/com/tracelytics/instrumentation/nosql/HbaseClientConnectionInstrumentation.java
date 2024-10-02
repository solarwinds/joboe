package com.tracelytics.instrumentation.nosql;

import java.net.InetSocketAddress;
import java.util.List;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.TvContextObjectAware;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;
import com.tracelytics.joboe.Metadata;

/**
 * Instrumentation for HbaseClient.Connection. This is the lower client level class HBase used for request traffic. We capture the client method, region info
 * , action count (for multiRequest) and remote address here. If there is already an active HBase in the current Thread, we will only report all the info as 
 * info event; Otherwise, we will create an async entry event and the exit is caught in the {@link HbaseClientCallInstrumentation}
 * @author Patson Luk
 *
 */
public class HbaseClientConnectionInstrumentation extends HbaseBaseInstrumentation {
    private static final String CLASS_NAME = HbaseClientConnectionInstrumentation.class.getName();

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        
        for (CtMethod method : cc.getMethods()) {
            if ("sendParam".equals(method.getName()) && shouldModify(cc, method)) {
                insertBefore(method,
                             "String methodName = null;" +
                             "Object regionInfo = null;" +
                             "Integer actionCount= null;" +
                             "if ($1 != null && $1.param instanceof org.apache.hadoop.hbase.ipc.Invocation) {" +
                             "    methodName = ((org.apache.hadoop.hbase.ipc.Invocation)$1.param).getMethodName(); " +
                             "    Object[] parameters = ((org.apache.hadoop.hbase.ipc.Invocation)$1.param).getParameters(); " +
                             "    if (parameters != null && parameters.length > 0) {" +
                             "        if (parameters[0] instanceof byte[]) {" +
                             "            regionInfo = parameters[0];" +
                             "        } else if (parameters[0] instanceof org.apache.hadoop.hbase.client.MultiAction) {" +
                             "            org.apache.hadoop.hbase.client.MultiAction multiAction = (org.apache.hadoop.hbase.client.MultiAction)parameters[0];" +
                             "            actionCount = Integer.valueOf(multiAction.size());" +
                             "            java.util.Set regions = multiAction.getRegions();" +
                             "            if (regions != null) { " + 
                             "                regionInfo = new java.util.ArrayList();" +
                             "                int counter = 0;" +
                             "                java.util.Iterator iterator = regions.iterator();" +
                             "                while (iterator.hasNext()) {" +
                             "                    byte[] regionName = (byte[])iterator.next();" +
                             "                    if (regionName != null) { " +
                             "                        ((java.util.List)regionInfo).add(regionName);" +
                             "                    }" +
                             "                }" +
                             "            }" +
                             "        }" +
                             "    }" +
                             "}" +
                             CLASS_NAME + ".layerEntry(getRemoteAddress(), $1, methodName, regionInfo, actionCount);"
                        );
                //   insertAfter(method, CLASS_NAME + ".layerExit($1 != null && $1.param instanceof org.apache.hadoop.hbase.ipc.Invocation ? ((org.apache.hadoop.hbase.ipc.Invocation)$1.param).getMethodName() : null);");
            }

        }

        return true; 
    }

    @SuppressWarnings("unchecked")
    public static void layerEntry(InetSocketAddress socket, Object call, String methodName, Object regionInfo, Integer actionCount) {
        if (isScannerCall(methodName) && getCurrentInstrumentedScannerOperation() == null) { //for scanner operation, only continue on if there's an active scanner operation instrumented
            return;
        }
        
        if (!hasActiveExtent()) {
            Event event = Context.createEvent();
            event.addInfo("Layer", LAYER_NAME,
                          "Label", "entry",
                          "Flavor", FLAVOR,
                          "ClientMethod", methodName);
    
            if (socket.getAddress() != null) {
                //event.addInfo("RemoteHost", socket.getAddress().getHostAddress() + ":" + socket.getPort());
                event.addInfo("RemoteHost", socket.getAddress().getHostAddress());
                event.addInfo("RemotePort", socket.getPort());
            }
    
            if (regionInfo != null) {
                addRegionInfo(event, regionInfo);
            }
            
            if (actionCount != null) {
                event.addInfo("ActionCount", actionCount);
            }
    
            event.report();

            if (Context.getMetadata().isSampled() && call instanceof TvContextObjectAware) {
                ((TvContextObjectAware) call).setTvContext(Context.getMetadata()); 
                ((TvContextObjectAware) call).setTvFromThreadId(Thread.currentThread().getId());
            }
        } else { //then only report the info event
            Event event = Context.createEvent();
            event.addInfo("Layer", LAYER_NAME,
                          "Label", "info",
                          "ClientMethod", methodName);
    
            if (socket.getAddress() != null) {
                event.addInfo("RemoteHost", socket.getAddress().getHostAddress()); 
                event.addInfo("RemotePort", socket.getPort());
            }
    
            if (regionInfo != null) {
                addRegionInfo(event, regionInfo);
            }
            
            if (actionCount != null) {
                event.addInfo("ActionCount", actionCount);
            }
    
            event.report();
        }

    }

    /**
     * Identifies whether this client request is triggered by scanner calls by checking the method name
     * @param methodName
     * @return
     */
    private static boolean isScannerCall(String methodName) {
        return "next".equals(methodName) || "openScanner".equals(methodName ) || "close".equals(methodName);
    }

    private static void addRegionInfo(Event event, Object regionInfo) {
        if (regionInfo instanceof List) {
            List<byte[]> allRegionInfo = (List<byte[]>)regionInfo;
            if (allRegionInfo.size() > 1) {
                String[] regionInfoArray = new String[allRegionInfo.size()];
                for (int i = 0 ; i < regionInfoArray.length; i++) {
                    regionInfoArray[i] = toStringBinary(allRegionInfo.get(i));
                }
                event.addInfo("RegionInfo", regionInfoArray);
            } else if (allRegionInfo.size() == 1) {
                event.addInfo("RegionInfo", toStringBinary((byte[])allRegionInfo.get(0)));
            }
        } else if (regionInfo instanceof byte[]){
            event.addInfo("RegionInfo", toStringBinary((byte[])regionInfo));
        }
    }

}
