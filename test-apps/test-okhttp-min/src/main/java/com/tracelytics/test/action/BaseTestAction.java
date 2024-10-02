package com.tracelytics.test.action;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.Preparable;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@SuppressWarnings("serial")
public abstract class BaseTestAction extends ActionSupport implements Preparable {
    private List<String> extendedOutput;
    private String urlString;
    private boolean isAsync;
    
    private OkHttpClient client;
    
    protected static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final int MAX_READ_LINE_COUNT = 10;
    
    static {
//        final HostnameVerifier originalHostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
//        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
//            
//            public boolean verify(String hostname, SSLSession session) {
//                if ("localhost".equals(hostname)) { //exception for localhost
//                    return true;
//                } else {
//                    if (originalHostnameVerifier != null) {
//                        return originalHostnameVerifier.verify(hostname, session);
//                    } else {
//                        return true;
//                    }
//                }
//            }
//        });
//        
//     // Create a trust manager that does not validate certificate chains
//        TrustManager[] trustAllCerts = new TrustManager[]{
//            new X509TrustManager() {
//                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
//                    return null;
//                }
//                public void checkClientTrusted(
//                        java.security.cert.X509Certificate[] certs, String authType) {
//                }
//   
//                public void checkServerTrusted(
//                        java.security.cert.X509Certificate[] certs, String authType) {
//                }
//            }
//        };
//        
//        SSLContext sslContext;
//        try {
//            sslContext = SSLContext.getInstance("SSL");
//            sslContext.init(null, trustAllCerts, new SecureRandom());
//            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
//        } catch (NoSuchAlgorithmException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        } catch (KeyManagementException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//        
        
    }
    
    public BaseTestAction() {
        client = new OkHttpClient();
        
    }
   
    @Override
    public String execute() throws Exception {
        Request request = buildRequest(urlString);
        
        
        if (isAsync) {
            final CountDownLatch latch = new CountDownLatch(2);
            AsyncCallback callback = new AsyncCallback(latch);
            client.newCall(request).enqueue(callback);
            client.newCall(request).enqueue(callback);
            latch.await();
            
        } else {
            Response response = client.newCall(request).execute();
            readResponse(response);
        }
        
        return SUCCESS;
     
    }
    
    private class AsyncCallback implements Callback {
        private final CountDownLatch latch;

        private AsyncCallback(CountDownLatch latch) {
            this.latch = latch;
        }
        @Override
        public void onResponse(Call call, Response response) throws IOException {
            readResponse(response);
            latch.countDown();
        }
        
        @Override
        public void onFailure(Call call, IOException e) {
            readException(e);
            latch.countDown();
        }
    }
    
    protected abstract Request buildRequest(String urlString);

    
    
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
    
    public boolean isAsync() {
        return isAsync;
    }

    public void setAsync(boolean isAsync) {
        this.isAsync = isAsync;
    }
        
    
    protected void readResponse(Response response) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream()));
        
        StringBuilder body = new StringBuilder();
        int lineCount = 0;
        String line = null;
        while ((line = reader.readLine()) != null) {
            ++lineCount;
            if (lineCount <= MAX_READ_LINE_COUNT) {
                body.append(line + System.lineSeparator());
            } 
        }
        
        if (lineCount > MAX_READ_LINE_COUNT) {
            body.append("..." + System.lineSeparator());
        }
        printToOutput(lineCount + " line(s) read");
        
        String responseMessage = response.message();
        int responseCode = response.code();
        
        printToOutput(body.toString());
        printToOutput(responseCode + " : " + responseMessage);
    }
    
    protected void readException(Exception e) {
        printToOutput(e.toString());
    }
}
