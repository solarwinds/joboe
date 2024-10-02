package com.tracelytics.test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
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
import org.springframework.web.client.RestTemplate;



/**
 * Really old impl using servlet using dispatcher!
 * @author Patson Luk
 *
 */
public class ApiRestServlet extends HttpServlet {


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
       
        try {
            //Start Rest test
            //Spring REST template client
            RestTemplate restTemplate = new RestTemplate();
            
            
            List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
            converters.add(new TestMessageConverter());
            restTemplate.setMessageConverters(converters);
            
            Data data = restTemplate.getForObject("https://api.tracelytics.com/api-v1/latency/{appName}/server/summary?key={apiKey}", Data.class, "Default", "9de3ed4c-5e33-4e26-b0b7-82c20ea01532");
            System.out.println(data.average);
            System.out.println(data.count);
            System.out.println(data.latest);
           //END REST test
            
            req.setAttribute("clientAction", "TraceView API (REST)");
        } catch (Exception e) {
            req.setAttribute("failureMessage", e.getMessage() != null ? e.getMessage() : e.toString());
        }
        req.getRequestDispatcher("simple.jsp").forward(req, resp);
    }

    
    private static class TestMessageConverter implements HttpMessageConverter<Data> {

        public boolean canRead(Class<?> clazz, MediaType mediaType) {
            return true;
        }

        public boolean canWrite(Class<?> clazz, MediaType mediaType) {
            return false;
        }

        public List<MediaType> getSupportedMediaTypes() {
            return Collections.singletonList(new MediaType("application", "json"));
        }

        public Data read(Class<? extends Data> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
            
            Reader reader = null;
            try {
                reader = new InputStreamReader(inputMessage.getBody());
                JSONObject object = new JSONObject(new JSONTokener(reader));
                
                object = object.getJSONObject("data");
                
                Data data = new Data();
                data.count = BigDecimal.valueOf(object.getDouble("count"));
                data.average = BigDecimal.valueOf(object.getDouble("average"));
                data.latest = object.isNull("latest") ? null : BigDecimal.valueOf(object.getDouble("latest"));
                
                return data;
            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                if (reader != null) {
                    reader.close();
                }
            }
            
            return null;
        }

        public void write(Data t, MediaType contentType, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
            throw new UnsupportedOperationException();            
        }
        
    }
}
