package com.encoding;

import java.util.Random;

public class StringUtils {
        public static String getRandomString(int length) {
            String str="zxcvbnmlkjhgfdsaqwertyuiopQWERTYUIOPASDFGHJKLZXCVBNM1234567890.,/=+-";
            Random random=new Random();
            StringBuilder sb=new StringBuilder();
            for(int i=0; i<length; ++i){
                int number=random.nextInt(str.length());
                sb.append(str.charAt(number));
            }
            return sb.toString();
        }

        /**
         * Calculate the Length of Shared Nibbles between str_1 and str_2
         * created in 11:50 2019/5/9
         */
        public static int findSharedLenth(String str_1, String str_2) {
            int i = 0;
            int len = Integer.min(str_1.length(),str_2.length());

            while( i < len ) {
                if(str_1.charAt(i) == str_2.charAt(i)) {
                    i++;
                }else {
                    break;
                }
            }
            return i;
        }
}
