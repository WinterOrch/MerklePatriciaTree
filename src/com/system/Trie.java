package com.system;

/**
 * Description
 * @author Yu.Mao
 * Created in 19:02 2018/6/17
 * Modified by
 */
public class Trie {
    public static String trie_Find(byte[] hash, String nibbles) {
        //TODO 数据库里利用hash查询
        //TODO 对数据进行RLP解码
        if (prefix == 0) {
            nibbles = nibbles + hash.解码得到的字符串;
            trie_Find(hash.getchild, nibbles);
        }
        if (prefix == 1) {
            nibbles = hash.nibbles + nibbles;
            trie_Find(hash.getchild, nibbles);
        }
        return nibbles;
    }


    public static byte[][] trie_FindFather(byte[][] s) {
        if (prefix == 0){

        }
        if(prefix == 1){
            if()
        }
        if(prefix == 2){
            if()
        }
    }


    public static void trie_Update(byte[][] s) {
        if (s != null) {
            if (s.prefix == 0)//更新的是分支节点
                s.nibble
                trie_Update(s);
            if (s.prefix == 1)

                if （s.prefix == 2）
            byte

            byte sFather = trie_FindFather(s);
            sFather.getchild = s;


        }
    }

    public static void trie_Modify(byte){

    }

}