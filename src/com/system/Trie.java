package com.system;

import com.db.Data;
import com.encoding.Hash;
import com.encoding.HexConver;
import com.encoding.RLP;
import com.system.node.Node;
import org.apache.commons.codec.digest.DigestUtils;

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

        while (!Node.isLeafNode(Objects.requireNonNull(node))) {
            if (Node.isExtensionNode(node)) {
                nibbles.append(Node.getNibbles(node));
                if (key.startsWith(nibbles.toString())) {
                    if (key.equals(nibbles.toString())) {
                        find = false;
                        break;
                    } else {
                        position = node;
                        node = Node.getExtensionChild(node);
                    }
                } else {
                    // Situation 3 - Extension node needs to be split, position points to its parent
                    find = false;
                    break;
                }
            } else if (Node.isBranchNode(node)) {
                if (key.equals(nibbles.toString())) {
                    find = false;
                    break;
                } else {
                    char temp = key.charAt(nibbles.length());
                    int index = mHexStr.indexOf(temp);

                    if (Node.getBranchChild(node, index) == null) {
                        // Situation 1 - Branch exists, need to add leaf node
                        position = node;
                        find = false;
                        break;
                    } else {
                        // Situation 2 - Get to 3 or 4
                        nibbles.append(temp);
                        position = node;
                        node = Node.getBranchChild(node, index);
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
        if (find) {
            if (Node.isLeafNode(node)) {
                nibbles.append(Node.getNibbles(node));
                if (key.equals(nibbles.toString())) {
                    return node;
                } else {
                    // Situation 4 - Leaf node unmatched, need to split its key-end and add new extension and branch node. Position points to its parent.
                    return null;
                }
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public static void createForLeaf(byte[] headHash, String key, byte[] value) {
        byte[][] store = new byte[3][40];
        int key_Length = 0;
        key = key.toUpperCase();
        // Initialize Data Base
        try {
            Data.initialize();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (headHash == null) {
            Node.setPrefix(store,Node.LEAF_PREFIX);
            Node.setNibbles(store,key);
            store[2] = value;
            Node.storeNode(store);
        } else {
            byte[][] node = RLP.rlpDecoding(Data.get(headHash));
            StringBuilder nibbles = new StringBuilder();
            byte[][] position = null;
            List<byte[]> path = new ArrayList<>();
            path.add(headHash);
            if (searchForLeaf(headHash, key) == null) {
                while (key_Length < key.length()) {//while条件可能要改成keyend
                    if (Node.isExtensionNode(node)) {
                        if (HexConver.compare2String(Node.getNibbles(node), key.substring(key_Length)) == Node.getNibbles(node).length()) {
                            position = node;
                            path.add(node[1]);
                            node = Node.getExtensionChild(node);
                            key_Length = key_Length + Node.getNibbles(node).length();
                        } else if (HexConver.compare2String(Node.getNibbles(node), key.substring(key_Length)) == 0)//shared signal is 0 bit similar;waiting to insert branchnode
                        {
                            byte[] extentionNodeOrigin = Hash.getHash(RLP.rlpEncoding(node));
                            Node.setNibbles(node, Node.getNibbles(node).substring(1));//此处改了原来的NIBBLES
                            byte[][] newBranchnode = new byte[15][40];
                            byte[][] newLeafnode = new byte[3][40];//新建叶子节点
                            Node.setPrefix(newLeafnode, Node.LEAF_PREFIX);
                            Node.setNibbles(newLeafnode, key.substring(key_Length+1));
                            newLeafnode[2] = value;
                            Node.setChild(newBranchnode, mHexStr.indexOf(key.charAt(key_Length)), newLeafnode);
                            Node.setChild(position, newBranchnode);
                            Node.setChild(newBranchnode, mHexStr.indexOf(Node.getNibbles(node).charAt(0)), node);//将原来的节点挂在新生成的branch上
                            Node.setValue(newBranchnode);
                            Node.storeNode(newBranchnode);
                            Node.storeNode(newLeafnode);
                            Node.storeNode(extentionNodeOrigin, node);
                            path.add(newBranchnode[mHexStr.indexOf(key.charAt(key_Length))]);
                            key_Length = key_Length + 1;//不算leafnode上的
                            break;
                        } else if (HexConver.compare2String(Node.getNibbles(node), key.substring(key_Length)) >= 1) {//waiting to insert extension node & branchnode shared signal>=1{
                            int sharedNibblesnum = HexConver.compare2String(Node.getNibbles(node), key.substring(key_Length));
                            String originalNibbles = Node.getNibbles(node);
                            byte[] extentionNodeOrigin = Hash.getHash(RLP.rlpEncoding(node));//get original hash
                            Node.setNibbles(node, originalNibbles.substring(sharedNibblesnum+1));//修改原来的Nibbles,由于要提extentionnode 和 branchnode
                            byte[][] newExtensionNode = new byte[3][40];
                            Node.setPrefix(newExtensionNode, Node.EXTENSION_PREFIX);
                            Node.setNibbles(newExtensionNode, originalNibbles.substring(0, sharedNibblesnum - 1));//insert repeated string
                            byte[][] newBranchnode = new byte[15][40];
                            byte[][] newLeafnode = new byte[3][40];
                            Node.setPrefix(newLeafnode, Node.LEAF_PREFIX);
                            newLeafnode[2] = value;
                            Node.setChild(position, newExtensionNode);
                            Node.setChild(newBranchnode, mHexStr.indexOf(originalNibbles.charAt(sharedNibblesnum)), node);//重复字后边的一位
                            key_Length = key_Length + sharedNibblesnum;
                            Node.setChild(newBranchnode, mHexStr.indexOf(key.charAt(key_Length)), newLeafnode);
                            Node.setNibbles(newLeafnode, key.substring(key_Length+1));//TODO KEY_END
                            Node.setValue(newBranchnode);
                            Node.storeNode(newExtensionNode);
                            Node.storeNode(newBranchnode);
                            Node.storeNode(newLeafnode);
                            Node.storeNode(extentionNodeOrigin, node);
                            key_Length = key_Length + 1;
                            path.add(newExtensionNode[1]);
                            path.add(newBranchnode[mHexStr.indexOf(originalNibbles.charAt(sharedNibblesnum))]);
                            path.add(node[1]);
                            break;
                        }

                    } else if (Node.isBranchNode(node)) {
                        if (Node.getBranchChild(node, mHexStr.indexOf(key.charAt(key_Length))) == null) {
                            path.add(node[mHexStr.indexOf(key.charAt(key_Length))]);
                            byte[][] newLeafNode = new byte[3][40];
                            Node.setPrefix(newLeafNode, Node.LEAF_PREFIX);
                            Node.setNibbles(newLeafNode, key.substring(key_Length + 1));
                            newLeafNode[2] = value;
                            Node.setChild(node, mHexStr.indexOf(key.charAt(key_Length)), newLeafNode);
                            Node.storeNode(newLeafNode);
                            key_Length = key_Length+1;
                            break;
                        } else if (Node.getBranchChild(node, mHexStr.indexOf(key.charAt(key_Length))) != null)//同一个branch挂了同一东西,那就挂个branch
                        {
                            /*byte[][] branchchild = Node.getBranchChild(node, mHexStr.indexOf(key.charAt(key_Length)));
                            if (Node.isBranchNode(branchchild)) {
                              /*  byte[] branchchildhash = Hash.getHash(RLP.rlpEncoding(branchchild));
                                byte[][] newLeafnode = new byte[3][40];
                                Node.setPrefix(newLeafnode, Node.LEAF_PREFIX);
                                Node.setNibbles(newLeafnode, key.substring(key_Length + 1));
                                newLeafnode[3] = value;
                                Node.setChild(branchchild, mHexStr.indexOf(key.charAt(key_Length)), newLeafnode);
                                Node.setValue(branchchild);
                                Node.storeNode(branchchildhash, branchchild);
                                Node.storeNode(newLeafnode);
                                break;

                                position = node;
                                node = Node.getBranchChild(node, mHexStr.indexOf(key.charAt(key_Length)));
                                key_Length = key_Length+1;
                            } else if (Node.isLeafNode(branchchild)) {
                                if (HexConver.compare2String(Node.getNibbles(node), key.substring(key_Length)) >= 1) {
                                    int sharednibbles = HexConver.compare2String(Node.getNibbles(node), key.substring(key_Length));
                                    byte[][] newExtensionnode = new byte[3][40];
                                    Node.setPrefix(newExtensionnode, Node.EXTENSION_PREFIX);
                                    Node.setNibbles(newExtensionnode, key.substring(0, sharednibbles - 1));
                                    key_Length = key_Length + sharednibbles;
                                    byte[][] newBranchnode = new byte[16][40];
                                    Node.setChild(newExtensionnode, newBranchnode);
                                    byte[][] newLeafnode = new byte[3][40];
                                    Node.setChild(newBranchnode, mHexStr.indexOf(key.charAt(key_Length)), newLeafnode);
                                    key_Length = key_Length + 1;
                                    Node.setPrefix(newLeafnode, Node.LEAF_PREFIX);
                                    Node.setNibbles(newLeafnode, key.substring(key_Length));
                                    newLeafnode[3] = value;//赋值
                                    Node.setChild(position, newExtensionnode);
                                    Node.setValue(newBranchnode);
                                    Node.storeNode(newLeafnode);//存值
                                    Node.storeNode(newBranchnode);
                                    Node.storeNode(newExtensionnode);
                                    break;
                                }

                            }*/
                            position = node;
                            path.add(node[mHexStr.indexOf(key.charAt(key_Length))]);
                            node = Node.getBranchChild(node, mHexStr.indexOf(key.charAt(key_Length)));
                            key_Length = key_Length+1;
                        }
                    } else if (Node.isLeafNode(node)) {//TODO 放进数据库
                        if (HexConver.compare2String(Node.getNibbles(node), key.substring(key_Length)) >= 1) {//KEY_END有1位相同
                            int sharednibbles = HexConver.compare2String(Node.getNibbles(node), key.substring(key_Length));
                            byte[][] newExtensionnode = new byte[3][40];
                            Node.setPrefix(newExtensionnode, Node.EXTENSION_PREFIX);
                            Node.setNibbles(newExtensionnode, key.substring(0, sharednibbles - 1));
                            key_Length = key_Length + sharednibbles;
                            byte[][] newBranchnode = new byte[16][40];
                            Node.setChild(newExtensionnode, newBranchnode);
                            byte[][] newLeafnode = new byte[3][40];
                            Node.setChild(newBranchnode, mHexStr.indexOf(key.charAt(key_Length)), newLeafnode);
                            Node.setPrefix(newLeafnode, Node.LEAF_PREFIX);
                            Node.setNibbles(newLeafnode, key.substring(key_Length+1));
                            newLeafnode[2] = value;//赋值
                            Node.setChild(position, newExtensionnode);
                            Node.setValue(newBranchnode);
                            Node.storeNode(newLeafnode);//存值
                            Node.storeNode(newBranchnode);
                            Node.storeNode(newExtensionnode);
                            path.add(newBranchnode[1]);
                            path.add(newBranchnode[mHexStr.indexOf(key.charAt(key_Length))]);
                            key_Length = key_Length + 1;
                            break;
                        }
                    }
                }

            } else
                System.out.println("we already have this data");
        }
    }

    public static void main(String[] args){
        byte[] value = DigestUtils.sha("123".getBytes());
        createForLeaf(null,"1234",value);
    }
}






