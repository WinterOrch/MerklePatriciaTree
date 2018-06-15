package com.encoding;

import java.util.Objects;

public class RLP {

    /**
     * RLP Encoding Process For Outside Use
     * @param input Two-dimension Byte Array As A Complete Node;
     * @return      One-dimension Byte Array As Its Encoding Result
     * created in 22:45 2018/6/15
     */
    public static byte[] rlpEncoding(byte[][] input) {
        byte[] output;
        byte[] temp = new byte[0];

        for (int i = 0; i < input.length; i++) {
            if(i == 0) {
                temp = rlpEncoding(input[i]);
            }else {
                byte[] temp2 = rlpEncoding(input[i]);
                byte[] temp3 = temp.clone();

                temp = new byte[temp3.length + temp2.length];
                System.arraycopy(temp3,0,temp,0,temp3.length);
                System.arraycopy(temp2,0,temp,temp3.length,temp2.length);
            }
        }

        byte[] prefix = encode_length(temp.length,192);
        output = new byte[Objects.requireNonNull(prefix).length + temp.length];
        System.arraycopy(prefix,0,output,0,prefix.length);
        System.arraycopy(temp,0,output,prefix.length,temp.length);

        return output;
    }

    /**
     * RLP Encoding Process For Outside Use
     * @param input One-dimension Byte Array As Encoding Result
     * @return      Two-dimension Byte Array As Node's Content
     * created in 22:45 2018/6/15
     */
    public static byte[][] rlpDecoding(byte[] input) {
        byte[][] output;
        int prefixLength;
        int length;

        if(((input[0] & 0xFF) >= 0xc0) && ((input[0] & 0xFF) <= 0xf7)) {
            prefixLength = 1;
            length = (int)input[0] - 0xc0;
        }else if((input[0] & 0xFF) >= 0xf8) {
            prefixLength = (input[0] & 0xFF) - 0xf7 + 1;
            length = 0;
            for(int i = 1; i < prefixLength; i++) {
                length = 256 * length + (input[i] & 0xFF);
            }
        }else return null;

        int i = prefixLength;
        int index = 0;
        byte[][] temp = new byte[20][];

        while( i < (prefixLength + length) ) {
            int l = getStrLength(input,i);
            temp[index] = rlpDecodingStr(input,i);
            index++;
            i = i + l;
        }

        output = new byte[index][];
        System.arraycopy(temp,0,output,0,index);
        return output;
    }

    /**
     * Decoding
     * Decode A Single String Inside A List
     * created in 22:21 2018/6/15
     */
    private static byte[] rlpDecodingStr(byte[] input, int index) {
        byte[] output;

        if((input[index] & 0xFF) < 0x80) {
            output = new byte[1];
            output[0] = input[index];

            return output;

        }else if((input[index] & 0xFF) == 0x80) {

            return null;

        }else if(((input[index] & 0xFF) > 0x80) && ((input[index] & 0xFF) <= 0xb7)) {
            int length = (input[index] & 0xFF) - 0x80;
            output = new byte[length];

            System.arraycopy(input,index + 1,output,0,length);

            return output;

        }else if(((input[index] & 0xFF) >= 0xb8) && ((input[index] & 0xFF) <= 0xbf)) {
            int byteLength = (input[index] & 0xFF) - 0xb7;
            int length = 0;

            for(int i = 0; i < byteLength; i++) {
                length = 256 * length + (input[index+i+1] & 0xFF);
            }
            output = new byte[length];

            System.arraycopy(input,1+byteLength+index,output,0,length);

            return output;

        }else
            return null;
    }

    /**
     * Decoding
     * Get Length For A Single String Inside A List
     * created in 22:19 2018/6/15
     */
    private static int getStrLength(byte[] input, int index) {
        int length;

        if((input[index] & 0xFF) < 0x80) {
            length = 1;
            return length;

        }else if((input[index] & 0xFF) == 0x80) {
            length = 0;
            return length + 1;

        }else if(((input[index] & 0xFF) > 0x80) && ((input[index] & 0xFF) <= 0xb7)) {
            length = (input[index] & 0xFF) - 0x80;
            return length + 1;

        }else if(((input[index] & 0xFF) >= 0xb8) && ((input[index] & 0xFF) <= 0xbf)) {
            int byteLength = (input[index] & 0xFF) - 0xb7;
            length = 0;

            for(int i = index; i < index + byteLength; i++) {
                length = 256 * length + (input[i+1] & 0xFF);
            }
            return length + byteLength + 1;

        }else
            return -1;
    }

    /**
     * Encoding
     * RLP Encoding with A One-dimension Byte Array as Input
     * created in 17:56 2018/6/15
     */
    private static byte[] rlpEncoding(byte[] input) {
        if(input == null) {
            byte[] output = new byte[1];
            output[0] = (byte)0x80;

            return output;
        }else if((input.length == 1) && ((int)input[0] < 0x80)) {

            return input;
        }else {
            byte[] output;
            byte[] prefix = encode_length(input.length, 128);

            output = new byte[Objects.requireNonNull(prefix).length + input.length];
            System.arraycopy(prefix,0,output,0,prefix.length);
            System.arraycopy(input,0,output,prefix.length,input.length);

            return output;
        }
    }

    /**
     * Encoding
     * Add Prefix For Byte Array
     * created in 11:42 2018/6/13
     */
    private static byte[] encode_length(int L, int offset) {
        byte[] output;

        if(L < 56) {
            output = new byte[1];
            output[0] = (byte)(L + offset);

            return output;
        }else if(L < Math.pow(0xFF, 8)) {
            byte[] temp = byteLength(L);

            output = new byte[1 + temp.length];
            output[0] = (byte)(temp.length + offset + 55);
            System.arraycopy(temp,0,output,1,temp.length);
            
            return output;
        }else {
            
            return null;
        }
    }

    /**
     * Encoding
     * Divide Integer Length into A Byte Array
     * created in 11:44 2018/6/13
     */
    private static byte[] byteLength(int x) {

        int byteLength = 0;
        byte[] temp = new byte[8];

        while(x != 0) {
            temp[byteLength] = (byte)(x & 0xFF);
            x = x / 256;
            byteLength++;
        }

        byte[] result = new byte[byteLength];
        System.arraycopy(temp, 0, result, 0, result.length);

        return result;
    }

    public static void main(String[] args) {
        byte[][] shit = new byte[3][];
        shit[0] = "吔屎啦，梁非凡".getBytes();
        shit[1] = "我要草你啊".getBytes();
        shit[2] = "不是你草我，是我要草你啊".getBytes();

        System.out.println(HexConver.byte2HexStr(shit[0],shit[0].length));
        System.out.println(HexConver.byte2HexStr(shit[1],shit[1].length));
        System.out.println(HexConver.byte2HexStr(shit[2],shit[2].length));

        byte[] k = rlpEncoding(shit);
        System.out.println(HexConver.byte2HexStr(k,k.length));
        System.out.println(k.length);

        byte[][] l = rlpDecoding(k);
        System.out.println(Objects.requireNonNull(l).length);
        System.out.println(HexConver.byte2HexStr(l[0],l[0].length));
        System.out.println(HexConver.byte2HexStr(l[1],l[1].length));
        System.out.println(HexConver.byte2HexStr(l[2],l[2].length));
    }

}
