package com.tracelytics.test;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.client.methods.DavMethodBase;
import org.apache.jackrabbit.webdav.client.methods.ReportMethod;
import org.apache.jackrabbit.webdav.client.methods.SearchMethod;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.version.report.ReportInfo;
import org.apache.jackrabbit.webdav.version.report.ReportType;
import org.w3c.dom.Document;

public class TestSlingSearch {
    private static final String DEFAULT_END_POINT = "http://localhost:8080/server/default/jcr:root";
    private static final String DEFAULT_QUERY = "SELECT * FROM [nt:base] WHERE [jcr:primaryType] = 'nt:unstructured'";
//    private static final String DEFAULT_QUERY = "*[@jcr:primaryType='nt:unstructured']";
    
    private static final String DEFAULT_LANUGAGE = "JCR-SQL2";
//    private static final String DEFAULT_LANUGAGE = "xpath";
    
    
           
    /**
     * @param args
     * @throws IOException 
     * @throws TransformerException 
     */
    public static void main(String[] args) throws IOException, TransformerException {
        HostConfiguration hostConfig = new HostConfiguration();
//        hostConfig.setHost("localhost", 8080);
//        hostConfig.setHost("localvm", 8080);
        HttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
        HttpConnectionManagerParams params = new HttpConnectionManagerParams();
        int maxHostConnections = 20;
        params.setMaxConnectionsPerHost(hostConfig, maxHostConnections);
        connectionManager.setParams(params);
        
        HttpClient client = new HttpClient(connectionManager);
        Credentials creds = new UsernamePasswordCredentials("admin", "admin");
        client.getState().setCredentials(AuthScope.ANY, creds);
        client.setHostConfiguration(hostConfig);
        
        
        String uri = DEFAULT_END_POINT;
        String query = DEFAULT_QUERY;
        String queryLanguage = DEFAULT_LANUGAGE;
        boolean hasError = false;
        if (args.length == 1) {
            System.err.println("Ignoring query argument [" + args[0] + "] as query language argument is not found!");
            hasError = true;
        } else if (args.length == 2) {
            query = args[0];
            queryLanguage = args[1];
        } else if (args.length == 3) {
            query = args[0];
            queryLanguage = args[1];
            uri = args[2];
        } else if (args.length > 3){
            System.err.println("Invalid parameters!");
        }
        
        if (hasError) {
            System.err.println("Either provide 2 arguments [query], [query language] or 3 arguments [query], [query language], [host]");
            return;
        }
        
        System.out.println(MessageFormat.format("Querying endpoint [{0}] with query [{1}] of query [{2}]", uri, query, queryLanguage));
         
        
        
        SearchMethod searchMethod = new SearchMethod(uri, query, queryLanguage);
        execute(client, searchMethod);
        
        DavPropertyNameSet nameSet = new DavPropertyNameSet();
        nameSet.add(DavPropertyName.GETLASTMODIFIED);
        nameSet.add(DavPropertyName.CREATIONDATE);
        nameSet.add(DavPropertyName.GETETAG);
        ReportMethod reportMethod = new ReportMethod("http://localhost:8080/server", new ReportInfo(ReportType.EXPAND_PROPERTY, DavConstants.DEPTH_INFINITY, nameSet));
        execute(client, reportMethod);
        

        PostMethod postMethod = new PostMethod(uri);
        NameValuePair[] urlParameters = new NameValuePair[] { 
                                                              new NameValuePair(":include", "/content/demo-spark/en_US/blog/jcr:content"),
                                                              new NameValuePair(":include", "/content/demo-spark/en_US/blog/pear/jcr:content"),
                                                              new NameValuePair(":include", "/content/demo-spark/en_US/jcr:content"),};
        
        
        postMethod.setRequestBody(urlParameters);
        System.out.println(postMethod.getResponseBodyAsString());
        client.getState().setCredentials(AuthScope.ANY, creds);
        client.executeMethod(postMethod);
        
        
        GetMethod getMethod = new GetMethod(uri + "/sling-test/sling/assert.js");
        client.executeMethod(getMethod);
        System.out.println(getMethod.getResponseBodyAsString());

    }
    
    private static void execute(HttpClient client, DavMethodBase method)   {
        
        try {
            client.executeMethod(method);
            System.out.println(method.getStatusCode());
            
            Document doc = method.getResponseBodyAsDocument();
            printDocument(doc, System.out);
        } catch (HttpException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (TransformerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }
    
    
    public static void printDocument(Document doc, OutputStream out) throws IOException, TransformerException {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        transformer.transform(new DOMSource(doc), 
             new StreamResult(new OutputStreamWriter(out, "UTF-8")));
    }

}
