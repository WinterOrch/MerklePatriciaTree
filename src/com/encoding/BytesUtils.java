package com.encoding;

public class BytesUtils {

    public static byte[] merge(byte[] byt1, byte[] byt2) {
        int len1 = byt1.length;
        int len2 = byt2.length;

        byte[] byt3 = new byte[len1 + len2];
        System.arraycopy(byt1,0,byt3,0,len1);
        System.arraycopy(byt2,0,byt3,len1,len2);

        return byt3;
    }

    public static byte[] copy(byte[] byt1) {
        byte[] res = new byte[byt1.length];
        System.arraycopy(res,0,byt1,0,byt1.length);
        return res;
    }
}
