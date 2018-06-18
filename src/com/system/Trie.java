package com.system;

import com.db.Data;
import com.encoding.RLP;
import com.system.node.Node;

import java.io.IOException;
import java.util.Objects;

/**
 * Description
 * @author Yu.Mao
 * Created in 19:02 2018/6/17
 * Modified by Frankel.Y
 */
public class Trie {

    private final static String mHexStr = "0123456789ABCDEF";

    public static byte[][] searchForLeaf(byte[] headHash, String key) {
        key = key.toUpperCase();
        // Initialize Data Base
        try {
            Data.initialize();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Get Root
        byte[][] node = RLP.rlpDecoding(Data.get(headHash));
        byte[][] position = null;
        boolean find = true;
        StringBuilder nibbles = new StringBuilder();

        while(!Node.isLeafNode(Objects.requireNonNull(node))) {
            if(Node.isExtensionNode(node)) {
                nibbles.append(Node.getNibbles(node));
                if(key.startsWith(nibbles.toString())) {
                    if(key.equals(nibbles.toString())) {
                        find = false;
                        break;
                    }else {
                        position = node;
                        node = Node.getExtensionChild(node);
                    }
                }else {
                    // Situation 3 - Extension node needs to be split, position points to its parent
                    find = false;
                    break;
                }
            }else if(Node.isBranchNode(node)) {
                if(key.equals(nibbles.toString())) {
                    find = false;
                    break;
                }else {
                    char temp = key.charAt(nibbles.length());
                    int index = mHexStr.indexOf(temp);

                    if(Node.getBranchChild(node,index) == null) {
                        // Situation 1 - Branch exists, need to add leaf node
                        position = node;
                        find = false;
                        break;
                    }else {
                        // Situation 2 - Get to 3 or 4
                        nibbles.append(temp);
                        position = node;
                        node = Node.getBranchChild(node,index);
                    }
                }
            }
        }

        // Close Data Base
        try {
            Data.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Judge
        if(find) {
            if(Node.isLeafNode(node)) {
                nibbles.append(Node.getNibbles(node));
                if(key.equals(nibbles.toString())) {
                    return node;
                }else {
                    // Situation 4 - Leaf node unmatched, need to split its key-end and add new extension and branch node. Position points to its parent.
                    return null;
                }
            }else {
                return null;
            }
        }else {
            return null;
        }



    }

}