package com.appoptics.test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class TestDriver {
    private static final String DEFAULT_URL = "http://localhost:8080/test-mongodb";
    private static final byte[] SECRET = "8mZ98ZnZhhggcsUmdMbS".getBytes(Charset.forName("US-ASCII"));

    public static void main(String[] args) throws IOException, InvalidKeyException, NoSuchAlgorithmException {
        StringBuilder result = new StringBuilder();
        String urlString = args.length >= 1 ? args[0] : DEFAULT_URL;

//        for (int i = 0 ; i < 20 ; i ++) {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
            String xTraceOptions = "trigger-trace;ts=" + timestamp;
            conn.setRequestProperty("x-trace-options", xTraceOptions);

            String signature = getHmacSignature(xTraceOptions, SECRET);
            System.out.println(xTraceOptions + " " + signature);

            conn.setRequestProperty("x-trace-options-signature", signature);
            try (BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = rd.readLine()) != null) {
                    result.append(line);
                }
            }
            System.out.println(conn.getResponseCode());
            System.out.println(conn.getHeaderFields());
            System.out.println(result.toString());
//        }
    }

    private static String getHmacSignature(String optionsString, byte[] secret) throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec signingKey = new SecretKeySpec(secret, "HMACSHA1");
        Mac mac = Mac.getInstance("HMACSHA1");
        mac.init(signingKey);
        byte[] rawHmac = mac.doFinal(optionsString.getBytes());
        return bytesToHex(rawHmac);
    }

    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
}
