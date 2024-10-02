package com.tracelytics.test.action;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.Preparable;

@SuppressWarnings("serial")
public abstract class BaseTestAction extends ActionSupport implements Preparable {
    private static final int MAX_READ_LINE_COUNT = 10;
    private List<String> extendedOutput;
    private String urlString;
    protected boolean callConnect;
    
    protected enum InputMethod {
        GET_INPUT_STREAM, GET_CONTENT
    }
    private List<String> inputMethodStrings = new ArrayList<String>();
    protected String inputMethodString;
    
    static {
        final HostnameVerifier originalHostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
            
            public boolean verify(String hostname, SSLSession session) {
                if ("localhost".equals(hostname)) { //exception for localhost
                    return true;
                } else {
                    if (originalHostnameVerifier != null) {
                        return originalHostnameVerifier.verify(hostname, session);
                    } else {
                        return true;
                    }
                }
            }
        });
        
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
        
        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (KeyManagementException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        
    }
    
    public BaseTestAction() {
        for (InputMethod inputMethod : InputMethod.values()) {
            inputMethodStrings.add(inputMethod.name());
        }
    }
   
    @Override
    public String execute() throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection)new URL(urlString).openConnection();
            
            preConnect(connection);
            if (isCallConnect()) {
                connection.connect();
            }
            writePayload(connection);
            readResponse(connection);
            return SUCCESS;
        } catch (Exception e) {
            printToOutput(e.getMessage(), (Object[])e.getStackTrace());
            return SUCCESS;
        } 
    }
    
    protected abstract void preConnect(HttpURLConnection connection) throws IOException;
    
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
    
    public String getUrlString() {
        return this.urlString;
    }
    
    public void setUrlString(String urlString) {
        if (!(urlString.startsWith("http://") || urlString.startsWith("https://"))) {
            urlString = "http://" + urlString;
        }
        this.urlString = urlString;
    }
    
    public boolean isCallConnect() {
        return callConnect;
    }

    public void setCallConnect(boolean callConnect) {
        this.callConnect = callConnect;
    }
    
    public String getDefaultInputMethodString() {
        return InputMethod.GET_INPUT_STREAM.name();
    }


    public String getInputMethodString() {
        return inputMethodString;
    }


    public void setInputMethodString(String inputMethodString) {
        this.inputMethodString = inputMethodString;
    }


    public List<String> getInputMethodStrings() {
        return inputMethodStrings;
    }
    
    public void setInputMethodStrings(List<String> inputMethodStrings) {
        this.inputMethodStrings = inputMethodStrings;
    }
    
    protected InputMethod getInputMethod() {
        return InputMethod.valueOf(inputMethodString);
    }
    
    protected void readResponse(HttpURLConnection connection) throws IOException {
        StringBuilder body = new StringBuilder();
        if (getInputMethod() == InputMethod.GET_INPUT_STREAM) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            
            int lineCount = 0;
            String line = null;
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
        } else if (getInputMethod() == InputMethod.GET_CONTENT) {
            printToOutput("object " + connection.getContent().getClass().getName() + " read");
        }
        
        int responseCode = connection.getResponseCode();
        String responseMessage = connection.getResponseMessage();
        printToOutput(body.toString());
        printToOutput(responseCode + " : " + responseMessage);
    }
}
