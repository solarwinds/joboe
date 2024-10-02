package com.tracelytics.test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsAsyncClientHttpRequestFactory;
import org.springframework.http.client.Netty4ClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.client.AsyncRestTemplate;

/**
 * Tests sample rest server deployed locally, please see README.txt
 * @author Patson Luk
 *
 */
public class SampleRestAsyncServlet extends HttpServlet {
    private static final String ENDPOINT_STRING = "http://localhost:8080/test-rest-server/something/{param1}/?duration={param2}";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {

        //Test Rest client on our sample server
        try {
            testByHttpUrlConnection();
            testByApacheHttpClient();
            testByNettyClient();

            req.setAttribute("clientAction", "SAMPLE rest server (REST async)");
        } catch (Exception e) {
            req.setAttribute("failureMessage", e.getMessage() != null ? e.getMessage() : e.toString());
        }

        req.getRequestDispatcher("simple.jsp").forward(req, resp);
    }

    private void testByHttpUrlConnection() throws URISyntaxException, InterruptedException, ExecutionException {
        AsyncRestTemplate restTemplate = new AsyncRestTemplate();
        sendRequest(restTemplate);
    }
    
    private void testByApacheHttpClient() throws URISyntaxException, InterruptedException, ExecutionException {
        AsyncRestTemplate restTemplate = new AsyncRestTemplate(new HttpComponentsAsyncClientHttpRequestFactory());
        sendRequest(restTemplate);
    }
    
    private void testByNettyClient() throws URISyntaxException, InterruptedException, ExecutionException {
        AsyncRestTemplate restTemplate = new AsyncRestTemplate(new Netty4ClientHttpRequestFactory());
        sendRequest(restTemplate);
    }
    
    private void sendRequest(AsyncRestTemplate restTemplate) throws InterruptedException, ExecutionException {
        List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
        converters.add(new TestMessageConverter());
        converters.add(new StringHttpMessageConverter());
        restTemplate.setMessageConverters(converters);
        
        ListenableFuture<ResponseEntity<SampleResult>> result;
        
        result = restTemplate.getForEntity(ENDPOINT_STRING, SampleResult.class, "2", 100);
        System.out.println(result.get().getBody().method);
        
//        HttpEntity<Object> entity = new RequestEntity<Object>(HttpMethod.POST, url);
        result = restTemplate.postForEntity(ENDPOINT_STRING, null, SampleResult.class, "2", 100);
        System.out.println(result.get().getBody().method);
        
        restTemplate.delete(ENDPOINT_STRING, "2", 100);
        restTemplate.put(ENDPOINT_STRING, null, "2", 100);
        
        restTemplate.getForEntity(ENDPOINT_STRING + "&exception=true", SampleResult.class, "2", 100); //test exception
        
        restTemplate.exchange(ENDPOINT_STRING, HttpMethod.GET, null, SampleResult.class, "2", 100).get();
        
        
    }

    private static class TestMessageConverter implements HttpMessageConverter<SampleResult> {

        public boolean canRead(Class<?> clazz, MediaType mediaType) {
            return true;
        }

        public boolean canWrite(Class<?> clazz, MediaType mediaType) {
            return false;
        }

        public List<MediaType> getSupportedMediaTypes() {
            return Collections.singletonList(new MediaType("application", "json"));
        }

        public SampleResult read(Class<? extends SampleResult> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
            
            Reader reader = null;
            try {
                reader = new InputStreamReader(inputMessage.getBody());
                JSONObject object = new JSONObject(new JSONTokener(reader));
                
                object = object.getJSONObject("result");
                
                SampleResult result = new SampleResult();
                result.path = object.getString("path");
                result.method = object.getString("method");
                
                return result;
            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                if (reader != null) {
                    reader.close();
                }
            }
            
            return null;
        }

        public void write(SampleResult t, MediaType contentType, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
            throw new UnsupportedOperationException();            
        }
        
    }
}
