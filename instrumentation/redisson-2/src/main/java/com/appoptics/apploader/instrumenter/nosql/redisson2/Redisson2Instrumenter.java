package com.appoptics.apploader.instrumenter.nosql.redisson2;

import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.nosql.redis.redisson.BaseRedisRedissonObjectInstrumentation;
import com.tracelytics.joboe.EventValueConverter;
import com.tracelytics.joboe.span.impl.Scope;
import com.tracelytics.joboe.span.impl.ScopeManager;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.joboe.span.impl.Tracer;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import org.redisson.api.RFuture;
import org.redisson.client.protocol.CommandData;
import org.redisson.client.protocol.CommandsData;
import org.redisson.client.protocol.RedisCommand;
import org.redisson.misc.RPromise;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * Handle various captured operation from instrumentation. This class has access to the actual redission 2 framework
 */
public class Redisson2Instrumenter {
    private static final Logger logger = LoggerFactory.getLogger();

    private static final EventValueConverter eventValueConverter = new EventValueConverter(100, Logger.Level.DEBUG); //limit to 100 for key and script value
    public static final Redisson2Instrumenter SINGLETON = new Redisson2Instrumenter();
    private static final ThreadLocal<Boolean> flaggedInternalScript = new ThreadLocal<Boolean>();

    private static final ThreadLocal<Boolean> isSyncCommand = new ThreadLocal<Boolean>();

    public void handleAsync(Object futureObject) {
        if (BaseRedisRedissonObjectInstrumentation.shouldEndExtent()) {
            Scope scope = ScopeManager.INSTANCE.active();
            Span span = scope.span();
            if (span != null && BaseRedisRedissonObjectInstrumentation.LAYER_NAME.equals(span.getOperationName())) {
                registerFutureListener(span, futureObject);
            } else {
                logger.warn("Found mismatching span, expect : ["+ BaseRedisRedissonObjectInstrumentation.LAYER_NAME + "] but found " + span);
            }

            scope.close(); //this would not finish the span, as the finishOnClose flag should be false
        }
    }

    public void beforeSend(CommandsData commandsData, SocketAddress remoteAddress) {
        Span span = buildCommandSpan(remoteAddress);
        for (CommandData<?, ?> command : commandsData.getCommands()) {
            reportCommand(span, command);
        }
        addCommandListener(span, commandsData.getPromise(), false);
    }

    public void beforeSend(CommandData commandData, SocketAddress remoteAddress) {
        if (flaggedInternalScript.get() != null && flaggedInternalScript.get()) { //do not instrument internal script
            return;
        }

        if (commandData == null || commandData.getCommand() == null) {
            return;
        }
        String commandName = commandData.getCommand().getName();
        if (commandName == null || "PING".equals(commandName)) {
            return;
        }

        Span span = buildCommandSpan(remoteAddress);
        reportCommand(span, commandData);
        addCommandListener(span, commandData.getPromise(), "GET".equals(commandName));
    }

    private static Span buildCommandSpan(SocketAddress remoteAddress) {
        Tracer.SpanBuilder spanBuilder = ClassInstrumentation.buildTraceEventSpan("redis-redisson-command");
        if (!isSyncCommand()) {
            spanBuilder.withSpanProperty(Span.SpanProperty.IS_ASYNC, true);
        }
        if (remoteAddress != null) {
            spanBuilder.withTag("RemoteHost", remoteAddress.toString());
        }
        return spanBuilder.start();
    }

    protected static Boolean isSyncCommand() {
        //check if it's a low level sync call
        if (isSyncCommand.get() != null) {
            return true;
        }

        Span parentSpan = ScopeManager.INSTANCE.activeSpan();
        boolean hasSyncParentSpan = parentSpan != null &&
                BaseRedisRedissonObjectInstrumentation.LAYER_NAME.equals(parentSpan.getOperationName()) &&
                !parentSpan.getSpanPropertyValue(Span.SpanProperty.IS_ASYNC);
        return hasSyncParentSpan;
    }

    public void flagSyncCommand(boolean value) {
        if (value) {
            isSyncCommand.set(true);
        } else {
            isSyncCommand.remove();
        }
    }

    public static void reportCommand(Span span, CommandData commandData) {
        if (commandData != null) {
            RedisCommand command = commandData.getCommand();
            if (command != null) {
                String commandName = command.getName();
                Map<String, Object> fields = new HashMap<String, Object>();
                fields.put("KVOp", commandName.toLowerCase());

                if (!"AUTH".equals(commandName)) { //do not report AUTH password
                    if ("EVAL".equals(commandName) || "EVALSHA".equals(commandName)) { //report script if it's eval or evalsha
                        fields.put("Script", eventValueConverter.convertToEventValue(commandData.getParams()[0]));
                    } else if (commandData.getParams() != null && commandData.getParams().length >= 1) { //spec: key referenced if applicable, don't retrieve if more than 1. Though for most operations (set etc), the first param is the KVKey
                        fields.put("KVKey", eventValueConverter.convertToEventValue(commandData.getParams()[0]));
                    }
                }
                span.log(fields);
            }
        }
    }

    public static void flagInternalScript(boolean value) {
        flaggedInternalScript.set(value);
    }


    protected void addCommandListener(final Span span, RPromise promise, final boolean checkResult) {
        promise.addListener(new FutureListener() {
            @Override
            public void operationComplete(Future future) {
                if (checkResult) {
                    if (future.isSuccess()) {
                        span.setTag("KVHit", future.getNow() != null);
                    }
                }
                if (future.cause() != null) {
                    ClassInstrumentation.reportError(span, future.cause());
                }

                span.finish();
            }
        });
    }

    protected void registerFutureListener(Span span, Object futureObject) {
        if (futureObject instanceof RFuture) {
            ((RFuture) futureObject).addListener(new TracingFutureListener(span));
        }
    }

    private static class TracingFutureListener implements FutureListener {
        private final Span span;
        private TracingFutureListener(Span span) {
            this.span = span;
        }

        @Override
        public void operationComplete(Future future) throws Exception {
            if (future.cause() != null) {
                ClassInstrumentation.reportError(span, future.cause());
            }

            span.finish();
        }
    }

}
