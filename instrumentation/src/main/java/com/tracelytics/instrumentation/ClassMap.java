/**
 * Maps class names (those being instrumented) to the instrumentation classes that do it.
 */
package com.tracelytics.instrumentation;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.ext.javassist.bytecode.annotation.Annotation;
import com.tracelytics.ext.json.JSONArray;
import com.tracelytics.ext.json.JSONException;
import com.tracelytics.instrumentation.actor.akka.AkkaActorCellInstrumentation;
import com.tracelytics.instrumentation.actor.akka.AkkaEnvelopeCompanionInstrumentation;
import com.tracelytics.instrumentation.cache.ehcache.EhcacheElementTagger;
import com.tracelytics.instrumentation.cache.ehcache.EhcacheInstrumentation;
import com.tracelytics.instrumentation.cache.ehcache.EhcacheSearchResultsTagger;
import com.tracelytics.instrumentation.cache.ehcache.HibernateCacheKeyTagger;
import com.tracelytics.instrumentation.cache.spymemcached.SpyMemcachedCallbackInstrumentation;
import com.tracelytics.instrumentation.cache.spymemcached.SpyMemcachedConnectionInstrumentation;
import com.tracelytics.instrumentation.cache.spymemcached.SpyMemcachedInstrumentation;
import com.tracelytics.instrumentation.cache.spymemcached.SpyMemcachedOperationInstrumentation;
import com.tracelytics.instrumentation.cache.xmemcached.XMemcachedInstrumentation;
import com.tracelytics.instrumentation.ejb.*;
import com.tracelytics.instrumentation.grpc.*;
import com.tracelytics.instrumentation.http.*;
import com.tracelytics.instrumentation.http.akka.client.AkkaHttpClientInstrumentation;
import com.tracelytics.instrumentation.http.akka.server.*;
import com.tracelytics.instrumentation.http.apache.ApacheHttpClientInstrumentation;
import com.tracelytics.instrumentation.http.apache.ApacheHttpMessageInstrumentation;
import com.tracelytics.instrumentation.http.apache.ApacheHttpMethodInstrumentation;
import com.tracelytics.instrumentation.http.apache.async.ApacheAsyncHttpClientInstrumentation;
import com.tracelytics.instrumentation.http.apache.async.ApacheAsyncRequestProducerInstrumentation;
import com.tracelytics.instrumentation.http.apache.async.ApacheFutureInstrumentation;
import com.tracelytics.instrumentation.http.apache.async.ApacheFutureWrapperInstrumentation;
import com.tracelytics.instrumentation.http.grizzly.GlassfishGrizzlyHttpHandlerInstrumentation;
import com.tracelytics.instrumentation.http.grizzly.GlassfishGrizzlyRequestPatcher;
import com.tracelytics.instrumentation.http.grizzly.GlassfishGrizzlyResponseInstrumentation;
import com.tracelytics.instrumentation.http.jetty.JettyHttpRequestInstrumentation;
import com.tracelytics.instrumentation.http.jetty.JettyHttpResponseListenerInstrumentation;
import com.tracelytics.instrumentation.http.netty.*;
import com.tracelytics.instrumentation.http.okhttp.OkHttpCallFactoryPatcher;
import com.tracelytics.instrumentation.http.okhttp.OkHttpCallInstrumentation;
import com.tracelytics.instrumentation.http.okhttp.OkHttpCallbackInstrumentation;
import com.tracelytics.instrumentation.http.play.*;
import com.tracelytics.instrumentation.http.sling.*;
import com.tracelytics.instrumentation.http.spray.*;
import com.tracelytics.instrumentation.http.undertow.UndertowHttpHandlerInstrumentation;
import com.tracelytics.instrumentation.http.undertow.UndertowHttpServerExchangeInstrumentation;
import com.tracelytics.instrumentation.http.undertow.UndertowIoCallbackInstrumentation;
import com.tracelytics.instrumentation.http.webflow.*;
import com.tracelytics.instrumentation.http.webflux.*;
import com.tracelytics.instrumentation.http.ws.*;
import com.tracelytics.instrumentation.http.ws.server.AxisMessageReceiverInstrumentation;
import com.tracelytics.instrumentation.http.ws.server.GlassfishJerseyMethodInvokerInstrumentation;
import com.tracelytics.instrumentation.http.ws.server.JaxWsWebServiceInstrumentation;
import com.tracelytics.instrumentation.http.ws.server.SunJerseyMethodDispatcherInstrumentation;
import com.tracelytics.instrumentation.jcr.JcrQueryInstrumentation;
import com.tracelytics.instrumentation.jcr.JcrSessionInstrumentation;
import com.tracelytics.instrumentation.jdbc.*;
import com.tracelytics.instrumentation.jms.MessageConsumerInstrumentation;
import com.tracelytics.instrumentation.jms.MessageListenerInstrumentation;
import com.tracelytics.instrumentation.jms.MessageProducerInstrumentation;
import com.tracelytics.instrumentation.job.quartz.QuartzJobInstrumentation;
import com.tracelytics.instrumentation.job.springbatch.SpringBatchJobInstrumentation;
import com.tracelytics.instrumentation.job.springbatch.SpringBatchStepExecutionInstrumentation;
import com.tracelytics.instrumentation.job.springbatch.SpringBatchStepInstrumentation;
import com.tracelytics.instrumentation.kotlin.CoroutineContinuationPatcher;
import com.tracelytics.instrumentation.logging.*;
import com.tracelytics.instrumentation.nosql.*;
import com.tracelytics.instrumentation.nosql.cassandra.*;
import com.tracelytics.instrumentation.nosql.redis.jedis.RedisJedisConnectionInstrumentation;
import com.tracelytics.instrumentation.nosql.redis.jedis.RedisJedisInstrumentation;
import com.tracelytics.instrumentation.nosql.redis.lettuce.*;
import com.tracelytics.instrumentation.nosql.redis.redisson.RedisRedisson1ObjectInstrumentation;
import com.tracelytics.instrumentation.nosql.redis.redisson.RedisRedissonConnectionManagerInstrumentation;
import com.tracelytics.instrumentation.nosql.redis.redisson.RedisRedissonIteratorPatcher;
import com.tracelytics.instrumentation.proxy.JbossProxyFactoryPatcher;
import com.tracelytics.instrumentation.sbt.SbtClasspathFilterPatcher;
import com.tracelytics.instrumentation.scala.ScalaForkJoinTaskPatcher;
import com.tracelytics.instrumentation.solr.*;
import com.tracelytics.instrumentation.threadpool.*;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ClassMap {
    private static final Logger logger = LoggerFactory.getLogger();
    //matches class name to instrumentation builder of ClassInstrumentation based on class hierarchy
    private static final Map<String, Set<InstrumentationBuilder<ClassInstrumentation>>> instMap = new ConcurrentHashMap<String, Set<InstrumentationBuilder<ClassInstrumentation>>>();
    //matches class name to instrumentation builder of ClassLocator based on class hierarchy
    private static final Map<String, Set<InstrumentationBuilder<ClassLocator>>> locatorMap = new HashMap<String, Set<InstrumentationBuilder<ClassLocator>>>();
    //matches annotation type name to instrumentation builder of ClassInstrumentation based on class annotation attributes
    private static final Map<String, Set<InstrumentationBuilder<ClassInstrumentation>>> annotatedClassInstrumentationMap = new HashMap<String, Set<InstrumentationBuilder<ClassInstrumentation>>>();
    //matches annotation type name to instrumentation builder of AnnotatedMethodsInstrumentation based on method annotation attributes
    private static final Map<String, InstrumentationBuilder<AnnotatedMethodsInstrumentation>> annotatedMethodInstrumentationMap = new HashMap<String, InstrumentationBuilder<AnnotatedMethodsInstrumentation>>();
    private static final Set<String> excludedPrefixes = new HashSet<String>();
    private static final Set<String> excludedPhrases = new HashSet<String>();
    private static final Set<String> excludedTypes = Collections.synchronizedSet(new HashSet<String>());
    private static final Set<String> annotationExcluded = new HashSet<String>();
    private static final Set<Module> excludedModules = new HashSet<Module>();
    private static final Set<String> retransformClasses = Collections.synchronizedSet(new HashSet<String>());
    private static final InstrumentationBuilderFactory builderFactory = InstrumentationBuilderFactory.INSTANCE;

    // Map of classes/interfaces to associated instrumentation class:
    static {
        if (ConfigManager.getConfig(ConfigProperty.AGENT_EXCLUDE_CLASSES) != null) {
            List<String> excludedClasses = parseExcludeClassString((String) ConfigManager.getConfig(ConfigProperty.AGENT_EXCLUDE_CLASSES));
            
            excludedPhrases.addAll(excludedClasses);
        }

        if (ConfigManager.getConfig(ConfigProperty.AGENT_EXCLUDE_MODULES) != null) {
            
            excludedModules.addAll(parseExcludeModulesString((String) ConfigManager.getConfig(ConfigProperty.AGENT_EXCLUDE_MODULES)));
        }
        
        registerInstrumentation("org.springframework.http.server.reactive.ServletHttpHandlerAdapter", ServletWithSpanContextInstrumentation.class, Module.SERVLET);
        
        registerInstrumentation("javax.servlet.http.HttpServlet", ServletInstrumentation.class, Module.SERVLET);
        registerInstrumentation("javax.servlet.http.HttpServletResponse", ServletResponseInstrumentation.class, Module.SERVLET);
        registerInstrumentation("javax.servlet.http.HttpServletRequest", ServletRequestInstrumentation.class, Module.SERVLET);
        registerInstrumentation("javax.servlet.AsyncContext", ServletAsyncContextInstrumentation.class, Module.SERVLET);

        registerInstrumentation("javax.servlet.Filter", FilterInstrumentation.class, Module.SERVLET);
        
        // WebLogic
        registerInstrumentation("weblogic.servlet.internal.WebAppServletContext", WebLogicServletContextInstrumentation.class, Module.SERVLET);
        // Websphere        
        registerInstrumentation("com.ibm.wsspi.webcontainer.RequestProcessor", IbmRequestProcessorInstrumentation.class, Module.SERVLET);
        // JRuby Rack - correct context and response header
        registerInstrumentation("org.jruby.rack.AbstractFilter", JRubyRackFilterPatcher.class, Module.SERVLET);

        registerInstrumentation("java.sql.Connection", ConnectionInstrumentation.class, Module.JDBC);
        registerInstrumentation("java.sql.Statement", StatementInstrumentation.class, Module.JDBC);
        //registerInstrumentation("java.sql.PreparedStatement", PreparedStatementInstrumentation.class, Module.JDBC); //uses app loader
        registerInstrumentation("org.postgresql.jdbc2.AbstractJdbc2Statement", PreparedStatementInstrumentation.class, Module.JDBC);
        registerInstrumentation("org.postgresql.jdbc3.AbstractJdbc3Statement", PreparedStatementInstrumentation.class, Module.JDBC);
        registerInstrumentation("org.postgresql.jdbc4.AbstractJdbc4Statement", PreparedStatementInstrumentation.class, Module.JDBC);
        registerInstrumentation("oracle.net.ns.NetOutputStream", OracleNetOutputStreamInstrumentation.class, Module.JDBC);

        registerInstrumentation("net.rubyeye.xmemcached.XMemcachedClient", XMemcachedInstrumentation.class, Module.X_MEMCACHED);
        registerInstrumentation("net.spy.memcached.MemcachedClient", SpyMemcachedInstrumentation.class, Module.SPY_MEMCACHED);
        registerInstrumentation("net.spy.memcached.ops.OperationCallback", SpyMemcachedCallbackInstrumentation.class, Module.SPY_MEMCACHED);
        registerInstrumentation("net.spy.memcached.MemcachedConnection", SpyMemcachedConnectionInstrumentation.class, Module.SPY_MEMCACHED);
        registerInstrumentation("net.spy.memcached.protocol.BaseOperationImpl", SpyMemcachedOperationInstrumentation.class, Module.SPY_MEMCACHED);

        registerInstrumentation("org.jboss.invocation.http.interfaces.Util", JbossHttpInvocationClientInstrumentation.class, Module.JBOSS);
        registerInstrumentation("org.jboss.invocation.Invocation", JbossHttpInvocationClientInstrumentation.class, Module.JBOSS);

        //JBOSS 7/8 EJB client
        registerInstrumentation("org.jboss.remoting3.Connection", JbossConnectionPatcher.class, Module.JBOSS); 
        registerInstrumentation("org.jboss.naming.remote.client.cache.ConnectionCache", JbossEjbConnectionCachePatcher.class, Module.JBOSS); //JBOSS 7 (jboss-remote-naming-1.x)
        registerInstrumentation("org.jboss.ejb.client.remoting.RemotingConnectionManager", JbossEjbConnectionManagerPatcher.class, Module.JBOSS); //JBOSS 8 (jboss-ejb-client-2.x)
        registerInstrumentation("org.jboss.ejb.client.remoting.RemotingConnectionEJBReceiver", JbossEjbReceiverInstrumentation.class, Module.JBOSS);
        registerInstrumentation("org.jboss.ejb.client.remoting.ProtocolMessageHandler", JbossEjbMessageHandlerPatcher.class, Module.JBOSS);
        registerInstrumentation("org.jboss.ejb.client.EJBClientInvocationContext", JbossEjbInvocationContextInstrumentation.class, Module.JBOSS);
        registerInstrumentation("org.jboss.ejb.client.EJBInvocationHandler", JbossEjbInvocationHandlerInstrumentation.class, Module.JBOSS);
        //JBoss 5/6 EJB client
        registerInstrumentation("org.jboss.remoting.MicroRemoteClientInvoker", JbossRemoteClientInstrumentation.class, Module.JBOSS);
        registerInstrumentation("org.jboss.remoting.InvocationRequest", JbossRemoteClientInstrumentation.class, Module.JBOSS);
        registerInstrumentation("org.jboss.remoting.InvocationResponse", JbossRemoteClientInstrumentation.class, Module.JBOSS);
        registerInstrumentation("org.jboss.aop.joinpoint.InvocationBase", JbossRemoteClientInstrumentation.class, Module.JBOSS);
        registerInstrumentation("org.jboss.aop.metadata.SimpleMetaData", JbossRemoteClientInstrumentation.class, Module.JBOSS);
        //JBoss 5/6 EJB server
        registerInstrumentation("org.jboss.remoting.ServerInvoker", JbossRemoteServerInstrumentation.class, Module.JBOSS);
        //JBoss 7/8 EJB server
        registerInstrumentation("org.jboss.as.ejb3.remote.protocol.versionone.MethodInvocationMessageHandler", JbossRemoteServerHandlerInstrumentation.class, Module.JBOSS);

        registerInstrumentation("org.jboss.invocation.proxy.AbstractClassFactory", JbossProxyFactoryPatcher.class, Module.JBOSS);
        
        // Struts
        registerInstrumentation("com.opensymphony.xwork2.ActionProxy", StrutsActionProxyInstrumentation.class, Module.STRUTS);

        // Spring
        registerInstrumentation("org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter", SpringHandlerAdapterInstrumentation.class, Module.SPRING); //Spring 3.1-
        registerInstrumentation("org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter", SpringRequestMappingHandlerAdapterInstrumentation.class, Module.SPRING); //Spring 3.2+
        registerInstrumentation("org.springframework.web.servlet.mvc.Controller", SpringControllerInstrumentation.class, Module.SPRING);
        registerInstrumentation("org.springframework.web.servlet.View", SpringViewInstrumentation.class, Module.SPRING);
        
        // Spring WebFlux
        registerInstrumentation("org.springframework.web.reactive.DispatcherHandler", DispatcherHandlerInstrumentation.class, Module.WEBFLUX);
        registerInstrumentation("org.springframework.web.reactive.function.server.RouterFunctions$DefaultRouterFunction", RouterFunctionInstrumentation.class, Module.WEBFLUX);
        registerInstrumentation("org.springframework.web.reactive.HandlerAdapter", HandlerAdapterInstrumentation.class, Module.WEBFLUX);
        registerInstrumentation("org.springframework.http.server.reactive.AbstractServerHttpRequest", AbstractServerHttpRequestInstrumentation.class, Module.WEBFLUX);
        registerInstrumentation("org.springframework.web.reactive.handler.AbstractHandlerMapping", AbstractHandlerMappingInstrumentation.class, Module.WEBFLUX);

        // Classloader patching
        registerInstrumentation("java.lang.ClassLoader", ClassLoaderPatcher.class, Module.CLASSLOADER);
        
        // Java ThreadPoolExecutor patching
        registerInstrumentation("java.util.concurrent.ThreadPoolExecutor", ThreadPoolExecutorPatcher.class, Module.JAVA_THREAD, true);
        registerInstrumentation("java.util.concurrent.ForkJoinPool", ForkJoinPoolPatcher.class, Module.JAVA_THREAD, true);
        registerInstrumentation("java.util.concurrent.ForkJoinTask", ForkJoinTaskPatcher.class, Module.JAVA_THREAD, true); 
        registerInstrumentation("java.util.concurrent.ForkJoinWorkerThread", ForkJoinWorkerThreadPatcher.class, Module.JAVA_THREAD, true);

        
        // Scala Thread patching
        registerInstrumentation("scala.concurrent.impl.Future$PromiseCompletingRunnable", RunnablePatcher.class, Module.SCALA_THREAD);
        registerInstrumentation("scala.concurrent.impl.CallbackRunnable", RunnablePatcher.class, Module.SCALA_THREAD);
        registerInstrumentation("scala.concurrent.forkjoin.ForkJoinTask", ScalaForkJoinTaskPatcher.class, Module.SCALA_THREAD); //for Scala 2.11 or earlier
        registerInstrumentation("scala.concurrent.forkjoin.ForkJoinWorkerThread", ForkJoinWorkerThreadPatcher.class, Module.SCALA_THREAD, true); //for Scala 2.11 or earlier

        // Kotlin Coroutine patching
        registerInstrumentation("kotlin.coroutines.Continuation", CoroutineContinuationPatcher.class, Module.KOTLINE_COROUTINE);

        // Netty thread factory patching
        registerInstrumentation("io.netty.util.concurrent.DefaultThreadFactory", NettyDefaultThreadFactoryPatcher.class, Module.NETTY_THREAD);
        
        // JSF
        registerInstrumentation("com.sun.faces.application.ActionListenerImpl", JSFActionListenerInstrumentation.class, Module.JSF);
        registerInstrumentation("org.apache.myfaces.application.ActionListenerImpl", JSFActionListenerInstrumentation.class, Module.JSF);
        // FacesServlet implements javax.servlet.Servlet not HttpServlet, so our HttpServlet instrumentation doesn't find
        // XXX: may want to consider instrumenting all Servlets, not just HttpServlets.
        registerInstrumentation("javax.faces.webapp.FacesServlet", ServletInstrumentation.class, Module.JSF);

        // Apache Commons HttpClient 3.x
        registerInstrumentation("org.apache.commons.httpclient.HttpClient", ApacheHttpClientInstrumentation.class, Module.APACHE_HTTP);
        registerInstrumentation("org.apache.commons.httpclient.HttpMethod", ApacheHttpMethodInstrumentation.class, Module.APACHE_HTTP);

        // Apache HttpComponents HttpClient 4.x
        registerInstrumentation("org.apache.http.client.HttpClient", ApacheHttpClientInstrumentation.class, Module.APACHE_HTTP);
        registerInstrumentation("org.apache.http.HttpMessage", ApacheHttpMessageInstrumentation.class, Module.APACHE_HTTP);
        
        // Apache AsyncHttpClient
        registerInstrumentation("org.apache.http.concurrent.BasicFuture", ApacheFutureInstrumentation.class, Module.APACHE_ASYNC_HTTP);
        registerInstrumentation("org.apache.http.impl.nio.client.FutureWrapper", ApacheFutureWrapperInstrumentation.class, Module.APACHE_ASYNC_HTTP);
        registerInstrumentation("org.apache.http.nio.client.HttpAsyncClient", ApacheAsyncHttpClientInstrumentation.class, Module.APACHE_ASYNC_HTTP);
        registerInstrumentation("org.apache.http.nio.protocol.HttpAsyncRequestProducer", ApacheAsyncRequestProducerInstrumentation.class, Module.APACHE_ASYNC_HTTP);
        
        // OkHttp
        registerInstrumentation("okhttp3.Call", OkHttpCallInstrumentation.class, Module.OKHTTP);
        registerInstrumentation("okhttp3.Call$Factory", OkHttpCallFactoryPatcher.class, Module.OKHTTP);
        registerInstrumentation("okhttp3.Callback", OkHttpCallbackInstrumentation.class, Module.OKHTTP);
        
        // Thrift
        registerInstrumentation("org.apache.thrift.TServiceClient", ThriftServiceClientInst.class, Module.THRIFT);
        registerInstrumentation("org.apache.thrift.ProcessFunction", ThriftProcessFunctionInst.class, Module.THRIFT);
        registerInstrumentation("org.apache.thrift.scheme.StandardScheme", ThriftStandardSchemeInst.class, Module.THRIFT);
        registerInstrumentation("org.apache.thrift.transport.TSocket", ThriftSocketInst.class, Module.THRIFT);
        
        // Web Flow
        registerInstrumentation("org.springframework.webflow.executor.mvc.FlowController", FlowControllerInstrumentation.class, Module.WEBFLOW); //Web flow v1 entry point controller
        registerInstrumentation("org.springframework.webflow.mvc.servlet.FlowHandlerAdapter", FlowHandlerAdapterInstrumentation.class, Module.WEBFLOW); //Web flow v2 entry point controller/adapter
        registerInstrumentation("org.springframework.webflow.execution.RequestContext", RequestContextInstrumentation.class, Module.WEBFLOW); //Track Transit-On and Current State object 
        registerInstrumentation("org.springframework.webflow.execution.ActionExecutor", ActionExecutorInstrumentation.class, Module.WEBFLOW); //Track Actions
        registerInstrumentation("org.springframework.webflow.execution.Action", ActionInstrumentation.class, Module.WEBFLOW); //Optional: To remove the wrapper info, cleaner info on the action in certain cases
        registerInstrumentation("org.springframework.webflow.engine.State", StateInstrumentation.class, Module.WEBFLOW); //State enter/exit tracking
        registerInstrumentation("org.springframework.webflow.engine.FlowExecutionExceptionHandler", FlowExceptionHandlerInstrumentation.class, Module.WEBFLOW); //Exception tracking
        registerInstrumentation("org.springframework.webflow.engine.Transition", TransitionInstrumentation.class, Module.WEBFLOW); //tracking decision action that goes through Transition's canExecute
        registerInstrumentation("org.springframework.webflow.action.ActionResultExposer", ActionResultExposerPatcher.class, Module.WEBFLOW); //wrap org.springframework.webflow.action.ActionResultExposer to expose methods
        
        //SOAP/REST
        registerInstrumentation("org.apache.cxf.endpoint.Client", CxfEndpointClientInstrumentation.class, Module.WEB_SERVICE); //Cxf SOAP
        registerInstrumentation("org.apache.cxf.endpoint.ClientCallback", CxfClientCallbackInstrumentation.class, Module.WEB_SERVICE); //Cxf SOAP
        registerInstrumentation("org.apache.cxf.jaxrs.client.AbstractClient", CxfRsClientInstrumentation.class, Module.WEB_SERVICE); //Cxf REST
        registerInstrumentation("org.apache.cxf.jaxrs.client.ClientProxyImpl", CxfRsProxyClientInstrumentation.class, Module.WEB_SERVICE); //Cxf REST
        registerInstrumentation("org.apache.cxf.jaxrs.client.WebClient", CxfRsWebClientInstrumentation.class, Module.WEB_SERVICE); //Cxf REST
        registerInstrumentation("org.apache.cxf.jaxrs.client.JaxrsClientCallback$JaxrsResponseCallback", CxfRsResponseCallbackInstrumentation.class, Module.WEB_SERVICE); //Cxf REST
        registerInstrumentation("org.apache.axis2.engine.AxisEngine", AxisWsClientInstrumentation.class, Module.WEB_SERVICE); //Axis 2 SOAP/REST
        registerInstrumentation("org.apache.axis2.description.OutInAxisOperationClient$NonBlockingInvocationWorker", AxisWsNonBlockingWorkerPatcher.class, Module.WEB_SERVICE); //Axis 2
        registerInstrumentation("org.apache.axis2.context.MessageContext", AxisWsMessageContextPatcher.class, Module.WEB_SERVICE); //Axis 2
        
        
        registerInstrumentation("com.sun.xml.internal.ws.client.sei.SEIStub", JaxWsClientInstrumentation.class, Module.WEB_SERVICE); //Glasshfish SOAP
        registerInstrumentation("com.sun.xml.internal.ws.api.pipe.Fiber$CompletionCallback", JaxWsCompletionCallbackInstrumentation.class, Module.WEB_SERVICE); //Glasshfish SOAP
        registerInstrumentation("com.sun.xml.ws.client.sei.SEIStub", JaxWsClientInstrumentation.class, Module.WEB_SERVICE); //Glassfish SOAP
        registerInstrumentation("com.sun.xml.ws.api.pipe.Fiber$CompletionCallback", JaxWsCompletionCallbackInstrumentation.class, Module.WEB_SERVICE); //Glassfish SOAP
        registerInstrumentation("javax.xml.soap.SOAPConnection", SOAPConnectionInstrumentation.class, Module.WEB_SERVICE); //javax.xml.soap
        registerInstrumentation("com.sun.jersey.api.client.Client", SunJerseyClientInstrumentation.class, Module.WEB_SERVICE); //Sun Jersey 1.x REST
        registerInstrumentation("com.sun.jersey.api.client.ClientRequest", SunJerseyClientRequestInstrumentation.class, Module.WEB_SERVICE); //Sun Jersey 1.x REST 
        registerInstrumentation("com.sun.jersey.api.client.AsyncWebResource", SunJerseyAsyncWebResourceInstrumentation.class, Module.WEB_SERVICE); //Sun Jersey 1.x REST
        registerInstrumentation("org.glassfish.jersey.client.ClientRuntime", GlassfishJerseyClientInstrumentation.class, Module.WEB_SERVICE); //Glassfish Jersey 2.x+ REST
        registerInstrumentation("org.glassfish.jersey.client.ResponseCallback", GlassfishJerseyResponseCallbackInstrumentation.class, Module.WEB_SERVICE); //Glassfish Jersey 2.x+ REST
        registerInstrumentation("org.springframework.web.client.RestTemplate", RestTemplateInstrumentation.class, Module.WEB_SERVICE); //Spring REST template
        registerInstrumentation("org.springframework.util.concurrent.ListenableFutureCallbackRegistry", RestTemplateCallbackRegistryInstrumentation.class, Module.WEB_SERVICE); //Spring REST template
        registerInstrumentation("org.springframework.http.client.AsyncClientHttpRequest", RestTemplateAsyncRequestInstrumentation.class, Module.WEB_SERVICE); //Spring REST template
        registerInstrumentation("org.jboss.resteasy.client.ClientRequest", ResteasyClientInstrumentation.class, Module.WEB_SERVICE); //JBoss Resteasy
        registerInstrumentation("org.jboss.resteasy.client.jaxrs.internal.ClientInvocation", ResteasyClientInvocationInstrumentation.class, Module.WEB_SERVICE); //JBoss Resteasy
        registerInstrumentation("org.restlet.Client", RestletClientInstrumentation.class, Module.WEB_SERVICE); //Restlet
        registerInstrumentation("org.restlet.Request", RestletRequestPatcher.class, Module.WEB_SERVICE); //Restlet
        registerInstrumentation("org.restlet.engine.http.header.HeaderUtils", RestletHeaderUtilsPatcher.class, Module.WEB_SERVICE); //Restlet version 2.0.x
        registerInstrumentation("org.restlet.engine.header.HeaderUtils", RestletHeaderUtilsPatcher.class, Module.WEB_SERVICE); //Restlet version 2.1.x+
        
        
        //HttpURLConnection
        registerInstrumentation("java.net.HttpURLConnection", HttpURLConnectionInstrumentation.class, Module.URL_CONNECTION);
        registerInstrumentation("sun.net.www.http.HttpClient", SunHttpClientPatcher.class, Module.URL_CONNECTION);

        //Java 11 HttpClient
        registerInstrumentation("java.net.http.HttpRequest", HttpRequestPatcher.class, Module.JAVA_HTTP_CLIENT);
        registerInstrumentation("java.net.http.HttpClient", HttpClientInstrumentation.class, Module.JAVA_HTTP_CLIENT);
        registerInstrumentation("java.net.http.HttpResponse", HttpResponsePatcher.class, Module.JAVA_HTTP_CLIENT);

        //Solr
        registerInstrumentation("org.apache.solr.handler.RequestHandlerBase", SolrHandlerInstrumentation.class, Module.SOLR);
        registerInstrumentation("org.apache.solr.handler.component.SearchComponent", SolrSearchComponentInstrumentation.class, Module.SOLR);
        registerInstrumentation("org.apache.solr.servlet.SolrDispatchFilter", SolrFilterInstrumentation.class, Module.SOLR);
        registerInstrumentation("org.apache.solr.core.SolrCore", SolrCoreInstrumentation.class, Module.SOLR);
        registerInstrumentation("org.apache.solr.response.QueryResponseWriter", SolrResponseWriterInstrumentation.class, Module.SOLR); //solr 3/4 class
        registerInstrumentation("org.apache.solr.request.QueryResponseWriter", SolrResponseWriterInstrumentation.class, Module.SOLR);  //solr 1.4- class
        registerInstrumentation("org.apache.solr.search.SolrCache", SolrCacheInstrumentation.class, Module.SOLR);
        registerInstrumentation("org.apache.solr.servlet.HttpSolrCall", SolrCallInstrumentation.class, Module.SOLR); //newer solr

        
        //HBase
        registerInstrumentation("org.apache.hadoop.hbase.client.HTableInterface", HbaseTableInstrumentation.class, Module.HBASE);
        registerInstrumentation("org.apache.hadoop.hbase.client.OperationWithAttributes", HbaseOperationInstrumentation.class, Module.HBASE);
        registerInstrumentation("org.apache.hadoop.hbase.client.Row", HbaseOperationInstrumentation.class, Module.HBASE);
        registerInstrumentation("org.apache.hadoop.hbase.ipc.HBaseClient$Connection", HbaseClientConnectionInstrumentation.class, Module.HBASE); //version 0.94 and before
        registerInstrumentation("org.apache.hadoop.hbase.ipc.HBaseClient$Call", HbaseClientCallInstrumentation.class, Module.HBASE);        //version 0.94 and before
        registerInstrumentation("org.apache.hadoop.hbase.ipc.RpcClient", HbaseRpcClientInstrumentation.class, Module.HBASE); //version after 0.94
        registerInstrumentation("com.google.protobuf.GeneratedMessage", HbaseRpcRequestPatcher.class, Module.HBASE); //version after 0.94, to get region info
        registerInstrumentation("org.apache.hadoop.hbase.client.ScannerCallable", HbaseScannerCallableInstrumentation.class, Module.HBASE);
        registerInstrumentation("org.apache.hadoop.hbase.KeyValue", HbaseKeyValueInstrumentation.class, Module.HBASE);
        registerInstrumentation("org.apache.hadoop.hbase.client.HBaseAdmin", HbaseAdminInstrumentation.class, Module.HBASE);
        registerInstrumentation("org.apache.hadoop.hbase.client.HConnectionManager$HConnectionImplementation", HbaseClientCallableLocator.class, Module.HBASE); //version 0.94 and before, locate the anonymous Callable class to instrument
        registerInstrumentation("org.apache.hadoop.hbase.client.AsyncProcess", HbaseClientRunnableLocator.class, Module.HBASE); //version 0.96, locate the anonymous Runnable class to instrument
        registerInstrumentation("org.apache.hadoop.hbase.client.AsyncProcess$AsyncRequestFutureImpl$SingleServerRequestRunnable", HbaseClientRunnableInstrumentation.class, Module.HBASE); //version after 0.99, to set context for Runnable sent to thread pool        
        registerInstrumentation("org.apache.hadoop.hbase.client.BufferedMutator", HbaseBufferedMutatorInstrumentation.class, Module.HBASE); //version after 1.0.0 - for batch operations

        //Grails/Groovy
        registerInstrumentation("org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerHelper", GrailsHelperInstrumentation.class, Module.GRAILS);
        registerInstrumentation("org.codehaus.groovy.grails.web.servlet.GrailsDispatcherServlet", GrailsDispatcherInstrumentation.class, Module.GRAILS);
        registerInstrumentation("org.codehaus.groovy.grails.plugins.web.filters.FilterToHandlerAdapter", GrailsFilterInstrumentation.class, Module.GRAILS);
        registerInstrumentation("org.codehaus.groovy.grails.plugins.web.filters.FilterConfig", GrailsFilterConfigPatcher.class, Module.GRAILS);
        
        //Sling
        registerInstrumentation("org.apache.sling.api.resource.Resource", SlingResourcePatcher.class, Module.SLING); //Apache Sling api 2.0.4+
        registerInstrumentation("org.apache.sling.api.SlingHttpServletRequest", SlingHttpServletRequestPatcher.class, Module.SLING); //Apache Sling api 2.0.4+
        registerInstrumentation("org.apache.sling.engine.SlingRequestProcessor", SlingRequestProcessorInstrumentation.class, Module.SLING); //Apache Sling Engine 2.2.0+
        registerInstrumentation("org.apache.sling.engine.impl.request.ContentData", SlingContentDataInstrumentation.class, Module.SLING);
        registerInstrumentation("org.apache.jackrabbit.webdav.server.AbstractWebdavServlet", JackRabbitWebdavServletInstrumentation.class, Module.SLING); //Apache Sling JCR Webdav 2.0.6+ and Apache Sling JCR DavEx 1.0.0+
        registerInstrumentation("org.apache.jackrabbit.webdav.WebdavRequest", JackRabbitWebdavRequestInstrumentation.class, Module.SLING);
        registerInstrumentation("org.apache.jackrabbit.server.util.RequestData", JackRabbitWebdavRequestDataInstrumentation.class, Module.SLING);
        

        //JCR
        registerInstrumentation("javax.jcr.query.Query", JcrQueryInstrumentation.class, Module.JCR);
        registerInstrumentation("javax.jcr.Session", JcrSessionInstrumentation.class, Module.JCR);

        //netty
        registerInstrumentation("org.jboss.netty.channel.Channel", NettyChannelInstrumentation.class, Module.NETTY);
        registerInstrumentation("org.jboss.netty.channel.ChannelHandler", Netty3ChannelHandlerInstrumentation.class, Module.NETTY);
        registerInstrumentation("org.jboss.netty.handler.codec.http.HttpRequest", Netty3HttpRequestPatcher.class, Module.NETTY);
        registerInstrumentation("org.jboss.netty.handler.codec.http.HttpResponse", Netty3HttpResponsePatcher.class, Module.NETTY);
        registerInstrumentation("io.netty.channel.Channel", Netty4ChannelInstrumentation.class, Module.NETTY);
        registerInstrumentation("io.netty.channel.ChannelInboundHandler", Netty4ChannelHandlerInstrumentation.class, Module.NETTY);
        registerInstrumentation("io.netty.channel.ChannelOutboundHandler", Netty4ChannelHandlerInstrumentation.class, Module.NETTY);
        registerInstrumentation("io.netty.handler.codec.http.HttpRequest", Netty4HttpRequestPatcher.class, Module.NETTY);
        registerInstrumentation("io.netty.handler.codec.http.HttpResponse", Netty4HttpResponsePatcher.class, Module.NETTY);
        registerInstrumentation("io.netty.handler.codec.http2.Http2Headers", Netty4Http2HeadersPatcher.class, Module.NETTY);
        registerInstrumentation("io.netty.handler.codec.http2.Http2FrameListener", NettyHttp2FrameListenerInstrumentation.class, Module.NETTY);
        registerInstrumentation("io.netty.handler.codec.http2.Http2FrameWriter", NettyHttp2FrameWriterInstrumentation.class, Module.NETTY);
        
        //gRPC client
        registerInstrumentation("io.grpc.internal.ClientCallImpl", GrpcClientCallInstrumentation.class, Module.GRPC);
        registerInstrumentation("io.grpc.stub.ClientCalls", GrpcClientCallsPatcher.class, Module.GRPC);
        registerInstrumentation("io.grpc.Metadata", GrpcMetadataPatcher.class, Module.GRPC);
        registerInstrumentation("io.grpc.ClientCall$Listener", GrpcClientCallListenerInstrumentation.class, Module.GRPC);
        registerInstrumentation("io.grpc.Channel", GrpcChannelPatcher.class, Module.GRPC);
        
        //gRPC server
        registerInstrumentation("io.grpc.ServerCallHandler", GrpcServerCallHandlerPatcher.class, Module.GRPC);
        registerInstrumentation("io.grpc.ServerCall$Listener", GrpcServerCallListenerPatcher.class, Module.GRPC);
        registerInstrumentation("io.grpc.internal.ServerCallImpl", GrpcServerCallInstrumentation.class, Module.GRPC);
        registerInstrumentation("io.grpc.internal.ServerCallImpl$ServerStreamListenerImpl", GrpcServerStreamListenerInstrumentation.class, Module.GRPC);
        
        //Play Controllers (scala)
        registerInstrumentation("play.api.mvc.ActionBuilder", PlayScalaActionProfileInstrumentation.class, Module.PLAY); //play 2.2+ scala action and controller
        registerInstrumentation("play.api.mvc.Action$$anon$1", Play2_0And2_1ActionProfileInstrumentation.class, Module.PLAY); //play 2.0 scala  action and controller
        registerInstrumentation("play.api.mvc.ActionBuilder$$anon$1", Play2_0And2_1ActionProfileInstrumentation.class, Module.PLAY); //play 2.1 scala  action and controller
        registerInstrumentation("play.api.mvc.Action$$anonfun$apply$4", Play2_0BlockWrapperPatcher.class, Module.PLAY); //block wrapper for Play 2.0
        registerLocator("play.api.mvc.ActionBuilder", PlayBlockWrapperLocator.class, Module.PLAY); //Locate block wrappers used in Play 2.1+

        
        //Play Controllers (java)
        registerInstrumentation("play.mvc.ActionInvoker", PlayActionInvokerInstrumentation.class, Module.PLAY); //play 1 action
        registerInstrumentation("play.mvc.Controller", PlayControllerInstrumentation.class, Module.PLAY); //play 1 java controller
        registerInstrumentation("play.core.j.JavaAction", PlayJavaActionProfileInstrumentation.class, Module.PLAY); //Play 2+ java action and controller
        
        //Play Template Rendering (Twirl)
        registerInstrumentation("play.templates.BaseScalaTemplate", PlayTemplateInstrumentation.class, Module.PLAY); //Play 2.0 - 2.2 template
        registerInstrumentation("play.twirl.api.BaseScalaTemplate", PlayTemplateInstrumentation.class, Module.PLAY); //Play 2.3+ template
        registerInstrumentation("twirl.api.BaseScalaTemplate", PlayTemplateInstrumentation.class, Module.PLAY); //Twirl standalone, basically the same as play.twirl but just with different package name
        
        //Play WS (scala)
        registerInstrumentation("play.api.libs.ws.WSRequest", PlayScalaWsRequestInstrumentation.class, Module.WEB_SERVICE); //2.3+ 
        registerInstrumentation("play.api.libs.ws.WS$WSRequest", PlayScalaWsRequestInstrumentation.class, Module.WEB_SERVICE); //2.1 - 2.2
        registerInstrumentation("play.api.libs.ws.WSResponseHeaders", PlayScalaWsResponseHeadersPatcher.class, Module.WEB_SERVICE); //2.3+
        registerInstrumentation("play.api.libs.ws.StreamedResponse", PlayScalaWsStreamedResponsePatcher.class, Module.WEB_SERVICE); //2.5+
        registerInstrumentation("play.api.libs.ws.WSResponse", PlayScalaWsResponsePatcher.class, Module.WEB_SERVICE); //2.3+ 
        registerInstrumentation("play.api.libs.ws.Response", PlayScalaWsResponsePatcher.class, Module.WEB_SERVICE); //2.1 - 2.2
        
        //Play WS (java)
        registerInstrumentation("play.libs.ws.StandaloneWSRequest", PlayJavaWsRequestInstrumentation.class, Module.WEB_SERVICE); //2.6+
        registerInstrumentation("play.libs.ws.WSRequest", PlayJavaWsRequestInstrumentation.class, Module.WEB_SERVICE); //2.3+
        registerInstrumentation("play.libs.WS$WSRequest", PlayJavaWsRequestInstrumentation.class, Module.WEB_SERVICE); //2.2
        registerInstrumentation("play.libs.ws.StreamedResponse", PlayJavaWsStreamedResponsePatcher.class, Module.WEB_SERVICE); //2.5+
        registerInstrumentation("play.libs.ws.StandaloneWSResponse", PlayJavaStandaloneWsResponsePatcher.class, Module.WEB_SERVICE); //2.6+
        registerInstrumentation("play.libs.ws.WSResponse", PlayJavaWsResponsePatcher.class, Module.WEB_SERVICE); //2.3+ 
        registerInstrumentation("play.libs.WS$Response", PlayJavaWsResponsePatcher.class, Module.WEB_SERVICE); //2.2

        //Play2Servlet
        registerInstrumentation("play.core.server.servlet.GenericPlay2Servlet", ServletWithSpanContextInstrumentation.class, Module.PLAY); //Play2war plugin servlet
        
        //Cassandra datastax
        registerInstrumentation("com.datastax.driver.core.Session", CassandraSessionInstrumentation.class, Module.CASSANDRA); 
        registerInstrumentation("com.datastax.driver.core.Query", CassandraStatementInstrumentation.class, Module.CASSANDRA);  //Cassandra Datastax cql 1.x
        registerInstrumentation("com.datastax.driver.core.Statement", CassandraStatementInstrumentation.class, Module.CASSANDRA); //Cassandra Datastax cql 2.x
        registerInstrumentation("com.datastax.driver.core.ResultSetFuture", CassandraResultSetFutureInstrumentation.class, Module.CASSANDRA); 
        registerInstrumentation("com.datastax.driver.core.BoundStatement", CassandraBoundStatementInstrumentation.class, Module.CASSANDRA);
        registerInstrumentation("com.datastax.driver.core.SimpleStatement", CassandraSimpleStatementInstrumentation.class, Module.CASSANDRA);
        registerInstrumentation("com.datastax.driver.core.Cluster", CassandraClusterInstrumentation.class, Module.CASSANDRA);
        
        //Ehcache 
        registerInstrumentation("net.sf.ehcache.Ehcache", EhcacheInstrumentation.class, Module.EHCACHE);
        registerInstrumentation("net.sf.ehcache.search.Results", EhcacheSearchResultsTagger.class, Module.EHCACHE);
        registerInstrumentation("net.sf.ehcache.Element", EhcacheElementTagger.class, Module.EHCACHE);
        registerInstrumentation("org.hibernate.cache.CacheKey", HibernateCacheKeyTagger.class, Module.EHCACHE); //group it as EHCACHE for now
        
        //Redis-Jedis
        registerInstrumentation("redis.clients.jedis.Connection", RedisJedisConnectionInstrumentation.class, Module.REDIS);
        registerInstrumentation("redis.clients.jedis.BinaryJedis", RedisJedisInstrumentation.class, Module.REDIS);
        registerInstrumentation("redis.clients.jedis.PipelineBase", RedisJedisInstrumentation.class, Module.REDIS); //newer version
        registerInstrumentation("redis.clients.jedis.Transaction", RedisJedisInstrumentation.class, Module.REDIS); //older version
        registerInstrumentation("redis.clients.jedis.Jedis", RedisJedisInstrumentation.class, Module.REDIS);
        
        //Redis-Lettuce (used by Redisson)
        registerInstrumentation("com.lambdaworks.redis.RedisAsyncConnection", RedisLettuceAsyncConnectionPatcher.class, Module.REDIS);
        registerInstrumentation("com.lambdaworks.redis.protocol.CommandHandler", RedisLettuceCommandHandlerInstrumentation.class, Module.REDIS);
        registerInstrumentation("com.lambdaworks.redis.protocol.Command", RedisLettuceCommandInstrumentation.class, Module.REDIS);
        registerInstrumentation("com.lambdaworks.redis.protocol.CommandArgs", RedisLettuceCommandArgsPatcher.class, Module.REDIS);
        registerInstrumentation("com.lambdaworks.redis.RedisClient", RedisLettuceClientInstrumentation.class, Module.REDIS);
        
        //Redis-Redisson
        registerInstrumentation("org.redisson.connection.ConnectionManager", RedisRedissonConnectionManagerInstrumentation.class, Module.REDIS);
        //v1.x
        registerInstrumentation("org.redisson.core.RObject", RedisRedisson1ObjectInstrumentation.class, Module.REDIS);
        //v2.x uses app loader see redisson-2 artifact under instrumentation
//        registerInstrumentation("org.redisson.api.RObject", RedisRedisson2ObjectInstrumentation.class, Module.REDIS);
        registerInstrumentation("org.redisson.iterator.RedissonListIterator", RedisRedissonIteratorPatcher.class, Module.REDIS);

        // Akka actor
        registerInstrumentation("akka.actor.ActorCell", AkkaActorCellInstrumentation.class, Module.AKKA_ACTOR);
        registerInstrumentation("akka.dispatch.Envelope$", AkkaEnvelopeCompanionInstrumentation.class, Module.AKKA_ACTOR);

        //Akka http general patching
        registerInstrumentation("akka.http.javadsl.model.HttpRequest", AkkaHttpRequestPatcher.class, Module.AKKA_HTTP);
        registerInstrumentation("akka.http.javadsl.model.HttpResponse", AkkaHttpResponsePatcher.class, Module.AKKA_HTTP);

        //Akka Http Server
        registerLocator("akka.http.impl.engine.server.HttpServerBluePrint$ControllerStage", AkkaHttpControllerStageFunctionLocator.class, Module.AKKA_HTTP);
        registerLocator("akka.http.impl.engine.server.HttpServerBluePrint$PrepareRequests", AkkaHttpPrepareRequestsInHandlerLocator.class, Module.AKKA_HTTP);
        registerInstrumentation("akka.http.impl.engine.parsing.ParserOutput$RequestStart", AkkaHttpRequestStartPatcher.class, Module.AKKA_HTTP);
        registerLocator("akka.http.scaladsl.server.ExceptionHandler$", AkkaHttpDefaultExceptionHandlerLocator.class, Module.AKKA_HTTP);

        //Akka Http Client
        registerInstrumentation("akka.http.scaladsl.HttpExt", AkkaHttpClientInstrumentation.class, Module.AKKA_HTTP);

        //Spray
        registerInstrumentation("spray.can.server.OpenRequestComponent$DefaultOpenRequest", SprayDefaultOpenRequestInstrumentation.class, Module.SPRAY); //Spray-can
        registerInstrumentation("spray.http.HttpRequest", SprayHttpRequestPatcher.class, Module.SPRAY);
        registerInstrumentation("spray.routing.HttpServiceBase$class", SprayHttpServiceInstrumentation.class, Module.SPRAY); //Spray-routing
        registerInstrumentation("spray.routing.ExceptionHandler", SprayExceptionInstrumentation.class, Module.SPRAY); //Spray-routing
        registerInstrumentation("spray.servlet.Servlet30ConnectorServlet", ServletWithSpanContextInstrumentation.class, Module.SPRAY); //Spray-servlet
        
        //Spray can client
        registerLocator("spray.can.client.ClientFrontend$", SprayClientPipelinesLocator.class, Module.SPRAY_CLIENT);
        registerLocator("spray.can.client.HttpClientConnection", SprayClientConnectionReceiveLocator.class, Module.SPRAY_CLIENT);
        registerInstrumentation("spray.can.client.HttpClientConnection", SprayClientConnectionPatcher.class, Module.SPRAY_CLIENT);
        registerInstrumentation("spray.http.HttpResponse", SprayHttpResponsePatcher.class, Module.SPRAY_CLIENT);
                
        //sbt classpath filter (classloader)
        registerInstrumentation("sbt.classpath.ClasspathFilter", SbtClasspathFilterPatcher.class, Module.CLASSLOADER);
        
        registerInstrumentation("io.undertow.server.HttpHandler", UndertowHttpHandlerInstrumentation.class, Module.UNDERTOW); 
        registerInstrumentation("io.undertow.server.HttpServerExchange", UndertowHttpServerExchangeInstrumentation.class, Module.UNDERTOW);
        registerInstrumentation("io.undertow.io.IoCallback", UndertowIoCallbackInstrumentation.class, Module.UNDERTOW);
        
        //Jersey server
        registerInstrumentation("org.glassfish.jersey.server.model.ResourceMethodInvoker", GlassfishJerseyMethodInvokerInstrumentation.class, Module.JERSEY_SERVER);
        registerInstrumentation("com.sun.jersey.server.impl.model.method.dispatch.ResourceJavaMethodDispatcher", SunJerseyMethodDispatcherInstrumentation.class, Module.JERSEY_SERVER);

        //Glassfish Grizzly Http Server
        registerInstrumentation("org.glassfish.grizzly.http.server.HttpHandler", GlassfishGrizzlyHttpHandlerInstrumentation.class, Module.GRIZZLY);
        registerInstrumentation("org.glassfish.grizzly.http.server.Request", GlassfishGrizzlyRequestPatcher.class, Module.GRIZZLY);
        registerInstrumentation("org.glassfish.grizzly.http.server.Response", GlassfishGrizzlyResponseInstrumentation.class, Module.GRIZZLY);


        //MDC
        registerInstrumentation("org.apache.log4j.MDC", Log4jMdcPatcher.class, Module.LOG_MDC);
        registerInstrumentation("org.apache.logging.log4j.ThreadContext", Log4j2ThreadContextPatcher.class, Module.LOG_MDC);
        registerInstrumentation("org.slf4j.spi.MDCAdapter", Slf4jMdcAdapterPatcher.class, Module.LOG_MDC);
        registerInstrumentation("org.apache.logging.log4j.core.layout.AbstractJacksonLayout", Log4j2LayoutPatcher.class, Module.LOG4J2);
        registerInstrumentation("org.jboss.logmanager.MDC", JbossMdcPatcher.class, Module.LOG_MDC);

        //Axis2 service
        registerInstrumentation("org.apache.axis2.receivers.AbstractMessageReceiver", AxisMessageReceiverInstrumentation.class, Module.WEB_SERVICE);

        //Jetty Http Client
        registerInstrumentation("org.eclipse.jetty.client.api.Request", JettyHttpRequestInstrumentation.class, Module.JETTY_HTTP_CLIENT);
        registerInstrumentation("org.eclipse.jetty.client.api.Response$CompleteListener", JettyHttpResponseListenerInstrumentation.class, Module.JETTY_HTTP_CLIENT);

        // JMS
        registerInstrumentation("javax.jms.MessageProducer", MessageProducerInstrumentation.class, Module.JMS);
        registerInstrumentation("javax.jms.MessageListener", MessageListenerInstrumentation.class, Module.JMS);
        registerInstrumentation("javax.jms.MessageConsumer", MessageConsumerInstrumentation.class, Module.JMS);
      
        //Quartz Job
        registerInstrumentation("org.quartz.Job", QuartzJobInstrumentation.class, Module.QUARTZ_JOB);

        //Spring Batch
        registerInstrumentation("org.springframework.batch.core.Job", SpringBatchJobInstrumentation.class, Module.SPRING_BATCH);
        registerInstrumentation("org.springframework.batch.core.Step", SpringBatchStepInstrumentation.class, Module.SPRING_BATCH);
        registerInstrumentation("org.springframework.batch.core.StepExecution", SpringBatchStepExecutionInstrumentation.class, Module.SPRING_BATCH);

        //SDK annotations
        registerAnnotatedMethodInstrumentation("com.tracelytics.api.ext.LogMethod", SdkAnnotationInstrumentation.class, Module.SDK);
        registerAnnotatedMethodInstrumentation("com.tracelytics.api.ext.ProfileMethod", SdkAnnotationInstrumentation.class, Module.SDK);
        registerAnnotatedMethodInstrumentation("com.appoptics.api.ext.LogMethod", SdkAnnotationInstrumentation.class, Module.SDK);
        registerAnnotatedMethodInstrumentation("com.appoptics.api.ext.ProfileMethod", SdkAnnotationInstrumentation.class, Module.SDK);
        
        //JAX-WS annotations
        registerAnnotatedClassInstrumentation("javax.jws.WebService", JaxWsWebServiceInstrumentation.class, Module.WEB_SERVICE);
        registerAnnotatedClassInstrumentation("javax.jws.WebServiceProvider", JaxWsWebServiceInstrumentation.class, Module.WEB_SERVICE);

        registerByServiceLoader();

        // Packages / classes we don't bother to inspect, primarily to avoid longer startup times:
        excludedPrefixes.add("java.lang.");
        excludedPrefixes.add("java.util.function.Function");
        excludedPrefixes.add("$Proxy"); // for jboss proxy classes (in version 5.x)
        excludedPrefixes.add("com.sun.proxy.");
        excludedPrefixes.add("java.io.RandomAccessFile"); //might trigger deadlock on ZipFile close, see https://github.com/librato/joboe/issues/1050
     
        //since felix org.apache.felix.framework.URLHandlersStreamHandlerProxy uses reflection that causes ClassCircularity error on several reflection classes
        //for details, refer to https://github.com/tracelytics/joboe/issues/134
        excludedPrefixes.add("sun.reflect."); 

        // Excluded, otherwise we get duplicate events because this wraps or extends other statement classes.
        // Older revs of Oracle JDBC (checked v9) did not have these Wrapper classes.
        excludedPrefixes.add("oracle.jdbc.driver.OracleCallableStatementWrapper");
        excludedPrefixes.add("oracle.jdbc.driver.OraclePreparedStatementWrapper");
        excludedPrefixes.add("oracle.jdbc.driver.OracleStatementWrapper");
        excludedPrefixes.add("oracle.jdbc.driver.OracleCallableStatement"); // extends PreparedStatement
        
        //exclude CGLib as it is unnecessary to instrument proxy and it triggers Verification Error sometimes, see https://github.com/librato/joboe/issues/601
        excludedPhrases.add("$$EnhancerByCGLIB");
        excludedPhrases.add("$$EnhancerBySpringCGLIB");

        // Avoid deadlocking as documented in https://github.com/librato/joboe/issues/595
        excludedPrefixes.add("sun.net.www.protocol.ftp.Handler");
        excludedPrefixes.add("sun.net.www.protocol.ftp.FtpURLConnection");
        
        // Avoid deadlocking for jboss + jdk 6, see https://github.com/librato/joboe/issues/702
        excludedPrefixes.add("org.jboss.kernel.");

        // Packages / classes we don't bother to apply annotations to:
        annotationExcluded.add("com.sun.");
        annotationExcluded.add("java.");
        annotationExcluded.add("javax.");
    }

    private static void registerByServiceLoader() {
        for (ClassInstrumentation instrumentation : ServiceLoader.load(ClassInstrumentation.class)) {
            logger.debug("Loaded instrumentation " + instrumentation.getClass().getName());
            Instrument annotation = instrumentation.getClass().getAnnotation(Instrument.class);
            if (annotation != null) {
                for (String targetType : annotation.targetType()) {
                    registerInstrumentation(targetType, instrumentation.getClass(), annotation.module(), annotation.retransform());
                }
            }
        }
    }


    /**
     * Gets a list of InstrumentationBuilder applicable to this class base on class level information provided in cc
     * 
     * @param cc
     * @param className
     * @return
     */
     public static Set<InstrumentationBuilder<ClassInstrumentation>> getInstrumentation(CtClass cc, String className) {
        if (cc.isInterface()) {
            // Skip interfaces
            return Collections.EMPTY_SET;
        }

        // Check to see if any instrumentation exists for this specific class:
        Set<InstrumentationBuilder<ClassInstrumentation>> builders = new LinkedHashSet<InstrumentationBuilder<ClassInstrumentation>>(); 
        
        Set<InstrumentationBuilder<ClassInstrumentation>> directInstrumentationBuilders = instMap.get(className.replace("/","."));
        
        if (directInstrumentationBuilders != null) {
            //builders.addAll(directInstrumentationBuilders);
            
          //for now, if any instrumentation is found, return and do not look further - this is done to preserve the behavior of previous instrumentation
          //Otherwise returning more instrumentation might produce unexpected result 
            return directInstrumentationBuilders;  
        }
        
        
        
        // Check the superclasses against instMap
        Set<InstrumentationBuilder<ClassInstrumentation>> inheritedInstrumentationClasses = lookupClassEntryByHeirarchy(cc, instMap);  
        
        builders.addAll(inheritedInstrumentationClasses);
        
        // Check class annotation
        for (Annotation annotation : AnnotationUtils.getAnnotationsFromType(cc)) {
            Set<InstrumentationBuilder<ClassInstrumentation>> annotatedClassInstrumentationBuilders = annotatedClassInstrumentationMap.get(annotation.getTypeName());
            if (annotatedClassInstrumentationBuilders != null) {
                builders.addAll(annotatedClassInstrumentationBuilders);
            }
        }
        
        return builders;
    }
     
    /**
     * Gets the list of InstrumentationBuilders of ClassLocator applicable to this class.
     * 
     * ClassLocators are used to to discover/locate valid classes for instrumentation. (for example anonymous classes)
     * @param cc
     * @param className
     * @return
     */
    public static Set<InstrumentationBuilder<ClassLocator>> getLocator(CtClass cc, String className) {
     // First check to see if any instrumentation exists for this specific class:
        Set<InstrumentationBuilder<ClassLocator>> result = new LinkedHashSet<InstrumentationBuilder<ClassLocator>>();
        Set<InstrumentationBuilder<ClassLocator>> directLocatorClasses = locatorMap.get(className.replace("/","."));
        if (directLocatorClasses != null) {
            result.addAll(directLocatorClasses);
        }
        
        result.addAll(lookupClassEntryByHeirarchy(cc, locatorMap));
        
        return result;
    } 
    
    /**
     * Gets the list of InstrumentationBuilders of AnnotatedMethodsInstrumentation applicable to methods declared in this class
     * 
     * The method annotations are used for matching
     * 
     * @param cc
     * @return
     */
    public static Map<InstrumentationBuilder<AnnotatedMethodsInstrumentation>, List<AnnotatedMethod>> getAnnotatedMethodInstrumentations(CtClass cc) {
        if (isAnnotationExcluded(cc.getName())) {
            return Collections.EMPTY_MAP;
        }
        
        CtMethod methods[] = cc.getDeclaredMethods();

        Map<InstrumentationBuilder<AnnotatedMethodsInstrumentation>, List<AnnotatedMethod>> result = new HashMap<InstrumentationBuilder<AnnotatedMethodsInstrumentation>, List<AnnotatedMethod>>(); 
        
        for(CtMethod method : methods) {
            List<Annotation> annotations = AnnotationUtils.getAnnotationsFromBehavior(method);
            
            for(Annotation annotation : annotations) {
                InstrumentationBuilder<AnnotatedMethodsInstrumentation> annotatedMethodsInstrumentationBuilder = annotatedMethodInstrumentationMap.get(annotation.getTypeName());
                if (annotatedMethodsInstrumentationBuilder != null) {
                    List<AnnotatedMethod> matches = result.get(annotatedMethodsInstrumentationBuilder);
                    if (matches == null) {
                        matches = new ArrayList<AnnotatedMethod>();
                        result.put(annotatedMethodsInstrumentationBuilder, matches);
                    }
                    matches.add(new AnnotatedMethod(method, annotation));
                }
            }
        }
        
        return result;
    }
    
    
    private static <T> Set<T> lookupClassEntryByHeirarchy(CtClass lookupClass, Map<String, Set<T>> classMap) {
        Set<T> result = new LinkedHashSet<T>();
        
     // check the superclasses:
        CtClass superClass = null;
        try {
            superClass = lookupClass.getSuperclass();
        
            while(superClass != null) {
                Set<T> instrumentationBuilders = classMap.get(superClass.getName());
                
                if (instrumentationBuilders != null) {
                    logger.trace("Class " + lookupClass.getName() + " found instrumentation/locator on superclass " + superClass.getName());
                    //result.addAll(instrumentationBuilders);
                    //for now, if any instrumentation is found, return and do not look further - this is done to preserve the behavior of previous instrumentation
                    //Otherwise returning more instrumentation might produce unexpected result
                    return instrumentationBuilders;
                }
                superClass = superClass.getSuperclass();
            }
        } catch (NotFoundException e) {
            logger.debug("Failed traversing superclasses on [" + lookupClass.getName() + "] at [" + superClass + "] : " + e.getMessage());
        }
        
                 
        // See if any instrumentation exists based on interface name, starting with this class and going to superclasses.
        CtClass baseClass = lookupClass;             
        
        try {
            while (baseClass != null) {
                List<CtClass> interfaces = new LinkedList<CtClass>();
                try {
                    getAllInterfaces(baseClass, interfaces);
                } catch (NotFoundException e) {
                    logger.debug("Failed traversing interfaces on [" + baseClass.getName() + "] : " + e.getMessage(),e );
                }
                for(CtClass interfaze : interfaces) {
                    Set<T> instrumentationBuilders= classMap.get(interfaze.getName());
                    if (instrumentationBuilders != null) {
                        logger.trace("Class " + lookupClass.getName() + " found instrumentation/locator on interface " + interfaze.getName());
                        //result.addAll(instrumentationBuilders);
                        //for now, if any instrumentation is found, return and do not look further - this is done to preserve the behavior of previous instrumentation
                        //Otherwise returning more instrumentation might produce unexpected result
                        return instrumentationBuilders;
                    }
                }
                baseClass = baseClass.getSuperclass();
            }
        } catch (NotFoundException e) {
            logger.debug("Failed traversing interfaces on [" + lookupClass.getName() + "] at [" + baseClass.getName() + "] : " + e.getMessage());
        }
        
        return result;
    }
    
    
    
    private static List<String> parseExcludeClassString(String config) {
        List<String> excludeClasses = new ArrayList<String>();
        try {
            JSONArray excludeClassArray = new JSONArray(config);
            for (int i = 0 ; i < excludeClassArray.length(); i++) {
                String excludeClass = (String) excludeClassArray.get(i);
                
                excludeClasses.add(excludeClass);
            }
        } catch (JSONException e) {
            logger.warn(e.getMessage());
            return Collections.emptyList();
        }
        
        return excludeClasses;
    }
    
    private static Set<Module> parseExcludeModulesString(String config) {
        Set<Module> excludeModules = new HashSet<Module>();
        try {
            JSONArray excludeModuleArray = new JSONArray(config);
            for (int i = 0 ; i < excludeModuleArray.length(); i++) {
                String excludeModuleString = (String) excludeModuleArray.get(i);
                
                try {
                    excludeModules.add(Module.valueOf(excludeModuleString.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    logger.warn("Unrecognized exclude module [" + excludeModuleString + "]");
                }
            }
        } catch (JSONException e) {
            logger.warn(e.getMessage());
            return Collections.emptySet();
        }
        
        return excludeModules;
    }

    /* Interfaces support multiple inheritance: Gets interfaces from a class, and all interfaces associated with those interfaces */
    private static void getAllInterfaces(CtClass baseClass, List<CtClass> interfaceList) throws NotFoundException {

        interfaceList.addAll(Arrays.asList(baseClass.getInterfaces()));
        for(CtClass interface_ : baseClass.getInterfaces()) {
            getAllInterfaces(interface_, interfaceList);
        }
    }
    
    /**
     * Determine if we should avoid processing a class entirely
     * @param pathClassName
     * @return
     */
    public static boolean isExcluded(String pathClassName) {
        String cls = pathClassName.replace("/",".");

        for(String pkg: excludedPrefixes) {
            if (cls.startsWith(pkg)) {
                return true;
            }
        }
        
        for (String phrase : excludedPhrases) {
            if (cls.contains(phrase)) {
                return true;
            }
        }

        if (excludedTypes.contains(cls)) {
            return true;
        }

        return false;
    }


    /**
     * Determine if we should avoid checking a class for annotations
     * @param pathClassName
     * @return
     */
    public static boolean isAnnotationExcluded(String pathClassName) {
        String cls = pathClassName.replace("/",".");

        for(String pkg: annotationExcluded) {
            if (cls.startsWith(pkg)) {
                return true;
            }
        }

        return false;
    }
    
    public static boolean registerInstrumentation(String targetTypeName, Class<? extends ClassInstrumentation> instrumentationClass, Module module) {
        return registerInstrumentation(targetTypeName, instrumentationClass, module, false);
    }
    public static boolean registerInstrumentation(String targetTypeName, Class<? extends ClassInstrumentation> instrumentationClass, Module module, boolean retransform) {
        return registerInstrumentation(targetTypeName, builderFactory.getBuilder(instrumentationClass), module, retransform);
    }
    
    /**
     * Registers an InstrumentationBuilder (of ClassInstrumentation) by class/interface name. If such a type name already has InstrumentationBuilder(s) registered, then append this new builder to that list
     * @param targetTypeName
     * @param instrumentationBuilder
     * @param module
     * @param retransform
     * @return  whether the registration was successful
     */
    public static boolean registerInstrumentation(String targetTypeName, InstrumentationBuilder<ClassInstrumentation> instrumentationBuilder, Module module, boolean retransform) {
        if (excludedModules.contains(module)) {
            logger.debug("Skipping instrumentation [" + instrumentationBuilder + "] on [" + targetTypeName + "] due to excluded module [" + module.name() + "]");
            return false;
        }
        
        synchronized(ClassMap.class) {
            Set<InstrumentationBuilder<ClassInstrumentation>> builders = instMap.get(targetTypeName);
            if (builders == null) {
                builders = Collections.synchronizedSet(new LinkedHashSet<InstrumentationBuilder<ClassInstrumentation>>());
                instMap.put(targetTypeName, builders);
            } 

            builders.add(instrumentationBuilder);

            if (retransform) {
                retransformClasses.add(targetTypeName);
            }
        }
        
        return true;
    }
    
    /**
     * Registers an InstrumentationBuilder by class annotation class name. If such a type name already has InstrumentationBuilder(s) registered, then append this new builder to that list
     * @param annotationClassName
     * @param instrumentationClass
     * @param module
     * @return
     */
    private static boolean registerAnnotatedClassInstrumentation(String annotationClassName, Class<? extends ClassInstrumentation> instrumentationClass, Module module) {
        if (excludedModules.contains(module)) {
            logger.debug("Skipping instrumentation [" + instrumentationClass.getName() + "] on [" + annotationClassName + "] due to excluded module [" + module.name() + "]");
            return false;
        }
        
        Set<InstrumentationBuilder<ClassInstrumentation>> builders = annotatedClassInstrumentationMap.get(annotationClassName);
        if (builders == null) {
            builders = new LinkedHashSet<InstrumentationBuilder<ClassInstrumentation>>();
            annotatedClassInstrumentationMap.put(annotationClassName, builders);
        } 
        
        builders.add(builderFactory.getBuilder(instrumentationClass));
        
        return true;
    }
     
    /**
     * Registers an InstrumentationBuilder by method annotation class name. If such a type name already has InstrumentationBuilder(s) registered, then append this new builder to that list
     * @param annotationTypeName
     * @param instrumentationClass
     * @param module
     * @return
     */
    private static boolean registerAnnotatedMethodInstrumentation(String annotationTypeName, Class<? extends AnnotatedMethodsInstrumentation> instrumentationClass, Module module) {
        if (excludedModules.contains(module)) {
            logger.debug("Skipping instrumentation [" + instrumentationClass.getName() + "] on [" + annotationTypeName + "] due to excluded module [" + module.name() + "]");
            return false;
        }
        
        annotatedMethodInstrumentationMap.put(annotationTypeName, builderFactory.getBuilder(instrumentationClass));
        
        return true;
    }
    
    /**
     * Registers an InstrumentationBuilder (of ClassLocator) by class/interface name. If such a type name already has InstrumentationBuilder(s) registered, then append this new builder to that list
     * @param targetTypeName
     * @param locatorClass
     * @param module
     * @return
     */
    private static boolean registerLocator(String targetTypeName, Class<? extends ClassLocator> locatorClass, Module module) {
        if (excludedModules.contains(module)) {
            logger.debug("Skipping locator [" + locatorClass.getName() + "] on [" + targetTypeName + "] due to excluded module [" + module.name() + "]");
            return false;
        }
        
        Set<InstrumentationBuilder<ClassLocator>> builders = locatorMap.get(targetTypeName);
        if (builders == null) {
            builders = new LinkedHashSet<InstrumentationBuilder<ClassLocator>>();
            locatorMap.put(targetTypeName, builders);
        } 
        
        builders.add(builderFactory.getBuilder(locatorClass));
        
        return true;
    }

    public static boolean addExcludedType(String typeName) {
        return excludedTypes.add(typeName);
    }
    
    
    /**
     * Gives a set of class names that should be considered for re-transformation if it could be skipped by the JVM in the init routine.
     * This usually is for core java class that is loaded before the agent has a chance to modify it. For example the "ThreadPoolExecutor"
     * @return
     */
    public static Set<String> getRetransformClasses() {
        return retransformClasses;
    }

}
