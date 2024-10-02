package com.tracelytics.test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

/**
 * Tests sample rest server deployed locally, please see README.txt
 * @author Patson Luk
 *
 */
public class SampleRestServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {

        //Test Rest client on our sample server
        try {
            RestTemplate restTemplate = new RestTemplate();
            List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
            converters.add(new TestMessageConverter());
            converters.add(new StringHttpMessageConverter());
            restTemplate.setMessageConverters(converters);
            
            SampleResult result;
            
            result = restTemplate.getForObject("http://localhost:8080/test-rest-server/something/{param1}/?duration={param2}", SampleResult.class, "2", 100);
            System.out.println(result.method);
            
            result = restTemplate.postForObject("http://localhost:8080/test-rest-server/something/{param1}/?duration={param2}", "", SampleResult.class, "2", 100);
            System.out.println(result.method);
            
            restTemplate.delete("http://localhost:8080/test-rest-server/something/{param1}/?duration={param2}", "2", 100);
            restTemplate.put("http://localhost:8080/test-rest-server/something/{param1}/?duration={param2}", "", "2", 100);
            
            

            req.setAttribute("clientAction", "SAMPLE API (REST)");
        } catch (Exception e) {
            req.setAttribute("failureMessage", e.getMessage() != null ? e.getMessage() : e.toString());
        }

        req.getRequestDispatcher("simple.jsp").forward(req, resp);
    }

    private static class TestMessageConverter implements HttpMessageConverter<SampleResult> {

        public boolean canRead(Class<? extends SampleResult> clazz, MediaType mediaType) {
            return true;
        }

        public boolean canWrite(Class<? extends SampleResult> clazz, MediaType mediaType) {
            return false;
        }

        public List<MediaType> getSupportedMediaTypes() {
            return Collections.singletonList(new MediaType("application", "json"));
        }

        public SampleResult read(Class<SampleResult> clazz, HttpInputMessage inputMessage)
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
