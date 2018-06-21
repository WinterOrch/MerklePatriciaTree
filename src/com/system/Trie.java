package com.system;

import com.db.Data;
import com.encoding.Hash;
import com.encoding.RLP;
import com.system.node.Node;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

    public static void createForLeaf(byte[] headHash, String key,byte[] value) {
        byte[][] store = new byte[3][60];
        int key_LeftLength = key.length();
        key = key.toUpperCase();
        // Initialize Data Base
        try {
            Data.initialize();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (headHash == null) {
            store[0][0] = (byte) (Node.EXTENSION_PREFIX & 0xFF);
            store[1] = key.getBytes();
            store[2] = value;
            Node.storeNode(store);
        } else {
            byte[][] node = RLP.rlpDecoding(Data.get(headHash));
            StringBuilder nibbles = new StringBuilder();
            byte[][] position = null;
            List<byte[]> path = new ArrayList<>();
            path.add(headHash);

            if (searchForLeaf(headHash, key) == null) {
                while (key_LeftLength!=0) {//while条件可能要改成keyend
                    if (Node.isExtensionNode(node)) {
                        nibbles.append(Node.getNibbles(node));
                        if (key.startsWith(nibbles.toString())) {
                            position = node;
                            path.add(node[2]);
                            node = Node.getExtensionChild(node);
                            key_LeftLength = key_LeftLength - Node.getNibbles(node).length();
                        } else {
                            if (Node.getNibbles(node) ==....)//shared signal is 0 bit similar;waiting to insert branchnode
                            {
                                position = new byte[15][40];
                                Node.setChild(position, node);
                                Node.setValue(position);
                            }
                            if (Node.getNibbles(node) ==.....)//waiting to insert extension node & branchnode
                            {
                                position = new byte[3][40];
                                Node.setChild(position, node);
                                Node.setPrefix(position, Node.EXTENSION_PREFIX);
                                key_LeftLength=key_LeftLength-sharedbits;

                            }
                            node = Node.getExtensionChild(node);
                        }
                    } else if (Node.isBranchNode(node)) {
                        if (key.equals(nibbles.toString())) {
                            position = node; //不存在的情况

                        } else {
                            char temp = key.charAt(nibbles.length());
                            int index = mHexStr.indexOf(temp);
                            if (Node.getBranchChild(node, index) == null) {
                                // Situation 1 - Branch exists, need to add leaf node
                                position = node;
                                byte[][] child = new byte[3][40];//挂上的叶子节点
                                Node.setChild(position,child);
                                Node.setPrefix(child, Node.LEAF_PREFIX);
                                Node.setNibbles(child, key.substring(key.length()-key_LeftLength));
                                child[3] = value;
                                Node.setChild(position, 1, child);
                            } else {
                                // Situation 2 - Get to 3 or 4
                                nibbles.append(temp);
                                position = node;
                                node = Node.getBranchChild(node, index);
                            }
                        }
                    }else if (Node.isLeafNode(node)){//TODO 放进数据库
                            if(Node.getNibbles(node)==.....){  //有至少一个相同的
                            byte[][] newExtensionnode = new byte[3][40];
                            Node.setChild(position,newExtensionnode);
                            Node.setPrefix(newExtensionnode, Node.EXTENSION_PREFIX);
                            Node.setNibbles(newExtensionnode,sharednibbles);
                            key_LeftLength=key_LeftLength-sharednibbles.length();
                            byte[][] newBranchnode = new byte[16][40];
                            Node.setChild(newExtensionnode, newBranchnode);
                            byte[][] newLeafnode = new byte[3][40];
                            Node.setChild(newBranchnode,mHexStr.indexOf(key.charAt(key.length()-key_LeftLength+1)),newLeafnode);
                            Node.setNibbles(newLeafnode,key.substring(key.length()-key_LeftLength+2));

                            }



                    }


                }
            } else
                System.out.println("we already have this data");
        }


    }




