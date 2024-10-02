package com.tracelytics.instrumentation.http.netty;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.Module;
import com.tracelytics.instrumentation.config.HideParamsConfig;
import com.tracelytics.instrumentation.http.ServletInstrumentation;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.XTraceHeader;
import com.tracelytics.joboe.XTraceOptionsResponse;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.joboe.span.impl.ScopeManager;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.joboe.span.impl.Span.SpanProperty;
import com.tracelytics.joboe.span.impl.SpanDictionary;
import com.tracelytics.util.HttpUtils;

public abstract class NettyBaseInstrumentation extends ClassInstrumentation {
    private static String LAYER_NAME = "netty";

    //Flag for whether hide query parameters as a part of the URL or not. By default false
    private static boolean hideUrlQueryParams = ConfigManager.getConfig(ConfigProperty.AGENT_HIDE_PARAMS) != null ? ((HideParamsConfig) ConfigManager.getConfig(ConfigProperty.AGENT_HIDE_PARAMS)).shouldHideParams(Module.NETTY) : false;
    
    private static final int SWITCHING_PROTOCOL_CODE = 101;
    private static final String UPGRADE_KEY = "upgrade";
    private static final String UPGRADE_HTTP2_TOKEN = "h2c";
    private static final String UPGRADE_HTTP2_TLS_TOKEN = "h2";
    private static final String HTTP2_STREAM_ID_KEY = "x-http2-stream-id"; //NOT the stream id used in actual http/2 request, this is the http/1.1 header converted from a http/2 header
    
    private static final String GRPC_APPLICATION = "application/grpc";

    public static void endTrace(Object channel, Object message, boolean endAll) {
        endTrace(channel, message, endAll, null);
    }
    public static void endTrace(Object channel, Object message, boolean endAll, Integer streamId) {
        if (!(channel instanceof NettyChannel)) {
            logger.warn("Cannot find the context from the Netty Context, cannot create the end trace event.");
            return;
        }
        NettyHttpResponse response = (NettyHttpResponse) message;
        
        if (isGrpcApplication(response)) { //do not handle grpc in netty level
            return;
        }
        
        NettyChannel nettyChannel = (NettyChannel) channel;
        Map<Integer, Span> spansOnChannel;  
        
        // get the activated spans stored in the channel in the layer entry. We need this as the entry and exit events could be on different threads. 
        // For http/2 request, there could be multiple requests/streams handled by the same instance. If a channel close is invoked, it should close ALL the requests/streams
        if (endAll) {
            spansOnChannel = nettyChannel.tvGetAndRemoveAllSpans();
        } else {
            spansOnChannel = new HashMap<Integer, Span>();
            Span span;
            if (isHttp2UpgradeResponse(response)) { //successfully upgraded
                streamId = 1; //should be stream id 1 for the response of upgrade, see https://http2.github.io/http2-spec/#StreamIdentifiers
                span = nettyChannel.tvGetAndRemoveSpan(null); //get the span created by the upgrade request
            } else {
                span = nettyChannel.tvGetAndRemoveSpan(streamId);
            }

            if (span != null) {
                spansOnChannel.put(streamId, span);
            } else { //already finished, or a response that is NOT directly initiated by request from client (for example PUSH_PROMISE)
                return;
            }
        }
        
        for (Entry<Integer, Span> spanEntry : spansOnChannel.entrySet()) {
            Integer streamIdInChannel = spanEntry.getKey();
            Span span = spanEntry.getValue();
            
            if (isHttp2UpgradeResponse(response)) { //response on http/1.1 upgrade to http/2
                Map<String, Object> info = new HashMap<String, Object>();
                info.put("HTTP2Upgrade", true);
                info.put("HTTP2UpgradeStatus", response.tvGetStatusCode());
                span.log(info);
                
                //do not finish the span, instead put it back to channel for the actual non 101 response frame to exit
                nettyChannel.tvSetSpan(streamId, span);
            } else {
                if (response != null && response.tvGetStatusCode() != null) {
                    span.setTag("Status", response.tvGetStatusCode());
                }
                
                if (streamIdInChannel != null) {
                    span.setTag("ResponseStreamId", streamIdInChannel);
                }
                
                span.finish();
    
                if (span.getSpanPropertyValue(SpanProperty.EXIT_XID) != null && response != null) {
                    response.tvSetHeader(ServletInstrumentation.XTRACE_HEADER.toLowerCase(), span.getSpanPropertyValue(SpanProperty.EXIT_XID)); //set x-trace to response header
                }

                if (response != null) {
                    XTraceOptionsResponse xTraceOptionsResponse = XTraceOptionsResponse.computeResponse(span);
                    if (xTraceOptionsResponse != null) {
                        response.tvSetHeader(ServletInstrumentation.X_TRACE_OPTIONS_RESPONSE_KEY, xTraceOptionsResponse.toString());
                    }
                }
                
                //cleanup
                SpanDictionary.removeSpan(span);
                Context.clearMetadata();
            }
        }
    }

    private static boolean isHttp2UpgradeResponse(NettyHttpResponse response) {
        if (response == null || response.tvGetStatusCode() != SWITCHING_PROTOCOL_CODE) {
            return false;
        }
        String upgradeToken = response.tvGetHeader(UPGRADE_KEY);
        return UPGRADE_HTTP2_TOKEN.equals(upgradeToken) || UPGRADE_HTTP2_TLS_TOKEN.equals(upgradeToken);
    }
    
    private static boolean isConvertedHttpRequest(NettyHttpRequest request) {
        return request.tvGetHeader(HTTP2_STREAM_ID_KEY) != null;
    }
    
    public static void startTrace(Object channel, Object message) {
        startTrace(channel, message, null);
    }
   
    public static void startTrace(Object channel, Object message, Integer streamId) {
        final NettyHttpRequest request = (NettyHttpRequest)message;
        
        if (isGrpcApplication(request)) { //do not handle grpc in netty level
            return;
        }

        if (streamId == null && isConvertedHttpRequest(request)) {  //do not trace if it's http request converted from http/2 header, as we instrument http/2 headers already
            return;
        }
        
        ScopeManager.INSTANCE.removeAllScopes();

        if (channel instanceof NettyChannel) {
            NettyChannel nettyChannel = (NettyChannel)channel;

            if (nettyChannel.tvGetSpan(streamId) != null) {
                return; //a span for this stream id has already started
            }
            
            Span span = startTraceAsSpan(LAYER_NAME, extractXTraceHeaders(request), request.getUri(), false);

            span.setTag("Spec", "ws");
            
            ClassInstrumentation.setForwardedTags(span, new HeaderExtractor<String, String>() {
                @Override public String extract(String s) {
                    return request.tvGetHeader(s);
                }
            });

            if (request.getUri() != null) {
                span.setTag("URL", hideUrlQueryParams ? HttpUtils.trimQueryParameters(request.getUri()) : request.getUri());
            }

            if (request.getTvMethod() != null) {
                span.setTag("HTTPMethod", request.getTvMethod());
            }
            
            if (streamId != null) { //for http/2, report as extra information just for reference 
                span.setTag("RequestStreamId", streamId);
            }

            if (nettyChannel.getRemoteAddress() != null) {
                span.setTag("Remote-Host", nettyChannel.getRemoteAddress().toString());
            }

            if (nettyChannel.getLocalAddress() != null) {
                span.setTag("HTTP-Host", nettyChannel.getLocalAddress().toString());
            }

            //set the span to the channel object so the channel can later finish the span
            nettyChannel.tvSetSpan(streamId, span); 
            //set the span to the dictionary and propagate the ID in request header such that other instrumentation (for example play) can continue on with it
            long spanKey = SpanDictionary.setSpan(span); 
            request.tvSetHeader(X_SPAN_KEY, String.valueOf(spanKey));

            //set x-trace id too as a backup, as other instrumentation could be running on a different process fi this netty server is acting as a http proxy
            request.tvSetHeader(XTRACE_HEADER, span.context().getMetadata().toHexString());
            
            Context.clearMetadata(); //should clear this to avoid context leak, as this thread only runs the event loop so clearing right after is correct
        }
    }
    
    private static boolean isGrpcApplication(NettyHttpMessage message) {
        return message != null && GRPC_APPLICATION.equals(message.tvGetHeader("content-type"));
    }
    
    public static void reportException(Object channel, Throwable exception) {
        if (!(channel instanceof NettyChannel)) {
            logger.warn("Cannot find the context from the Netty Context, cannot create error event.");
            return;
        }
        
        if (exception == null) {
            logger.debug("Not reporting error event for netty when execption object is null");
            return;
        }
        
        NettyChannel nettyChannel = ((NettyChannel)channel); 
        
        //take note that exception can not (and should not sometimes, for example a ReadTimeout is not specific to any particular request within a stream) be correlated to a specific stream id, therefore the null
        Span span = nettyChannel.tvGetSpan(null);  

        if (span == null) { //not traced or has already exited
            return;
        }
        
        if (!nettyChannel.tvHasExceptionReported(exception)) {
            reportError(span, exception);
            
            nettyChannel.tvAddExceptionReported(exception);
        }
    }
    
    /**
     * Extract special x-trace headers from the Netty http request
     * @param req   Netty Http Request
     * @return      a Map with key of type XTraceHeader, and value as the Http Request header's value
     * @see         XTraceHeader
     */
    private static Map<XTraceHeader, String> extractXTraceHeaders(NettyHttpRequest req) {
        Map<XTraceHeader, String> headers = new HashMap<XTraceHeader, String>();

        //cannot iterate from Http request headers, as it is not case sensitive and it might not match to values declared in XTRACE_HTTP_HEADER_KEYS
        for (Entry<String, XTraceHeader> xtraceHeaderEntry : ServletInstrumentation.XTRACE_HTTP_HEADER_KEYS.entrySet()) {
            String httpHeaderValue;
            if ((httpHeaderValue = req.tvGetHeader(xtraceHeaderEntry.getKey())) != null) {
                headers.put(xtraceHeaderEntry.getValue(), httpHeaderValue);
            }
        }

        return headers;
    }
    /**
     * Checks if specified class has a headers() method (for Netty 3.8+)
     * @param cc - Specified class
     * @return
     */
    protected boolean hasHeadersMethod(CtClass cc) {
        try {
            cc.getMethod("headers", "()Lorg/jboss/netty/handler/codec/http/HttpHeaders;");
            return true;
        } catch (NotFoundException e) {
            return false;
        }
    }
}
