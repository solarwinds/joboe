package com.solarwinds.joboe.sampling;

public class HexUtils {
    private static final char[] hexTable = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
    
    public static String bytesToHex(byte[] bytes) {
        return bytesToHex(bytes, bytes.length);
    }

    public static String bytesToHex(byte[] bytes, int len) {
        char[] hexChars = new char[len * 2];
        int v;
        for (int j = 0; j < len; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j*2] = hexTable[v/16];
            hexChars[j*2 + 1] = hexTable[v%16];
        }
        return new String(hexChars);
    }

    public static byte[] hexToBytes(String s) throws SamplingException {
        int len = s.length();

        if ((len % 2) != 0 || len > Constants.MAX_METADATA_PACK_LEN) {
            throw new SamplingException("Invalid string length");
        }

        byte[] buf = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            buf[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return buf;
    }
}
