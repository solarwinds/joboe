package com.sample;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.ws.handler.LogicalHandler;
import javax.xml.ws.handler.LogicalMessageContext;
import javax.xml.ws.handler.MessageContext;

public class XTraceIdHandler implements LogicalHandler<LogicalMessageContext> {


    @Override
    public void close(MessageContext context) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean handleMessage(LogicalMessageContext context) {
        if ((Boolean)context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY)) {
            Map map = (Map) context.get(MessageContext.HTTP_RESPONSE_HEADERS);
            
            if (map == null) {
            	map = new TreeMap<String,List<String>>(String.CASE_INSENSITIVE_ORDER);
            	context.put(MessageContext.HTTP_RESPONSE_HEADERS, map);
            }
            map.put("X-Trace", Collections.singletonList(Operator.getContext()));
            
            //the below line is for CXF server only, since it does not recognize the MessageContext.HTTP_RESPONSE_HEADERS properly
            context.put("org.apache.cxf.message.Message.PROTOCOL_HEADERS", map);
        } else {
            Map map = new TreeMap(String.CASE_INSENSITIVE_ORDER); //wrap it with case insensitive map to avoid case sensitive problem with some framework (CXF)
            map.putAll((Map) context.get(MessageContext.HTTP_REQUEST_HEADERS));
            
            List<String> xTraceId = (List<String>)map.get("X-Trace");
            
            
            if (xTraceId != null && !xTraceId.isEmpty()) {
                Operator.setContext(xTraceId.get(0));
            } else {
                Operator.setContext(null);
            }
        }
        return true;
    }

    @Override
    public boolean handleFault(LogicalMessageContext context) {
        // TODO Auto-generated method stub
        return false;
    }

}
