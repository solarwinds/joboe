package com.appoptics.test.action;

import com.appoptics.test.model.RequestForm;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.net.ssl.*;
import javax.validation.Valid;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("serial")
public abstract class BaseTestAction {
    private static final int MAX_READ_LINE_COUNT = 10;
    private List<String> extendedOutput;

    private static final SSLContext ignoreAllContext = createIgnoreAllContext();

    protected boolean callConnect;
    private HttpClient client = HttpClient.newBuilder().sslContext(ignoreAllContext).build();

    public BaseTestAction() {

    }



    @PostMapping("")
    final String execute(@Valid @ModelAttribute("request") RequestForm input, BindingResult result, ModelMap model) {
        extendedOutput = null;
        try {
            HttpRequest request = buildRequest(HttpRequest.newBuilder(URI.create(input.getUrl())));

            if (input.isAsync()) {
                CompletableFuture<HttpResponse<InputStream>> responseFuture = client.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream());
                readResponse(responseFuture.get());
            } else {
                HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                readResponse(response);
            }

        } catch (Exception e) {
            printToOutput(e.getMessage() != null ? e.getMessage() : e.getClass().getName(), (Object[])e.getStackTrace());

        }

        if (extendedOutput != null) {
            model.addAttribute("extendedOutput", extendedOutput);
        }


        return "index";

    }

    
    protected abstract HttpRequest buildRequest(HttpRequest.Builder requestBuilder) throws IOException;
    
    /**
     * Extra data to be written to the output stream (sent to remote host)
     * @param connection
     * @throws IOException
     */
    protected void writePayload(HttpURLConnection connection) throws IOException {} 
    
    public List<String> getExtendedOutput() {
        return extendedOutput;
    }

    public void setExtendedOutput(List<String> extendedOutput) {
        this.extendedOutput = extendedOutput;
    }

    public void appendExtendedOutput(String text) {
        if (extendedOutput == null) {
            extendedOutput = new LinkedList<String>();
        }
        extendedOutput.add(text);
    }

    public void prepare() throws Exception {
        extendedOutput = null; //clear the output       
    }

    protected void printToOutput(String title, Map<?, ?> map) {
        if (title != null) {
            appendExtendedOutput(title);
        }
        
        for (Object element : map.entrySet()) {
            appendExtendedOutput(element != null ? element.toString() : "null");
        }
    }
    
    protected void printToOutput(String title, List<?> keys) {
        if (title != null) {
            appendExtendedOutput(title);
        }
        
        for (Object element : keys) {
            appendExtendedOutput(element != null ? element.toString() : "null");
        }
    }
    
    protected void printToOutput(String title, Object...items) {
        printToOutput(title, Arrays.asList(items));
    }
    

    

    
    public boolean isCallConnect() {
        return callConnect;
    }

    public void setCallConnect(boolean callConnect) {
        this.callConnect = callConnect;
    }

    
    protected void readResponse(HttpResponse<InputStream> response) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()));

        int lineCount = 0;
        String line = null;
        StringBuilder body = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            ++lineCount;
            if (lineCount <= MAX_READ_LINE_COUNT) {
                body.append(line + System.getProperty("line.separator"));
            }
        }

        if (lineCount > MAX_READ_LINE_COUNT) {
            body.append("..." + System.getProperty("line.separator"));
        }
            
        printToOutput(lineCount + " line(s) read");

        int responseCode = response.statusCode();

        printToOutput(body.toString());
        printToOutput("status code: " + responseCode);
    }

    private static SSLContext createIgnoreAllContext() {
        final Properties props = System.getProperties();
        props.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());

        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
        };

        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }
        return sslContext;

    }
}
