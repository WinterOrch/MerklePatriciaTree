package com.system.node;

import com.db.Data;
import com.encoding.Hash;
import com.encoding.RLP;

/**
 * Tool Class For MPT Nodes
 * @author Frankel.Y
 * Created in 21:11 2018/6/17
 */
public class Node {

    public static int EXTENSION_PREFIX = 0x00;
    public static int LEAF_PREFIX = 0x02;

    /**
     * Usage:
     *      byte[][] nextNode = Node.getExtensionChild(extensionNode);
     * Warning:
     *      1.return null when the input node is not an extension node
     *      2.have to initialize the data base first(and close after that)
     * created in 21:11 2018/6/17
     */
    public static byte[][] getExtensionChild(byte[][] extensionNode) {
        if((extensionNode[0][0] & 0xFF) == EXTENSION_PREFIX) {
            byte[] childRLP = Data.get(extensionNode[2]);
            return RLP.rlpDecoding(childRLP);
        }else {
            return null;
        }
    }

    /**
     * Usage:
     *      byte[][] nextNode = Node.getBranchChild(branchNode);
     * Warning:
     *      1.return null when the input node is not a branch node or the next node doesn't exist
     *      2.have to initialize the data base first(and close after that)
     * created in 21:11 2018/6/17
     */
    public static byte[][] getBranchChild(byte[][] branchNode, int index) {
        if(branchNode.length == 17) {
            byte[] childRLP = Data.get(branchNode[index]);
            return RLP.rlpDecoding(childRLP);
        }else {
            return null;
        }
    }

    /**
     * Usage:
     *      String keyEnd = Node.getNibbles(leafNode);
     * Warning:
     *      1.return null when the input node is neither an extension node nor a leaf node
     * created in 21:11 2018/6/17
     */
    public static String getNibbles(byte[][] eNOrLN) {
        if(eNOrLN.length == 3) {
            return new String(eNOrLN[1]);
        }else {
            return null;
        }
    }

    /**
     * Usage:
     *      Node.setNibbles(leafNode, keyEnd);
     * Warning:
     *      1.return 0 when the input node is neither an extension node nor a leaf node
     * created in 21:11 2018/6/17
     */
    public static int setNibbles(byte[][] eNOrLN, String nibbles) {
        if(eNOrLN.length == 3) {
            eNOrLN[1] = nibbles.getBytes();
            return 1;
        }else {
            return 0;
        }
    }

    /**
     * Usage:
     *      Node.storeNode(leafNode);
     * Warning:
     *      1.have to initialize the data base first(and close after that)
     * created in 21:11 2018/6/17
     */
    public static void storeNode(byte[][] node) {
        byte[] rlpNode = RLP.rlpEncoding(node);
        Data.put(Hash.getHash(rlpNode),rlpNode);
    }

    /**
     * Usage:
     *      Node.setPrefix(leafNode, Node.LEAF_NODE);
     * Warning:
     *      1.return 0 when the input node is neither an extension node nor a leaf node
     * created in 21:11 2018/6/17
     */
    public static int setPrefix(byte[][] eNOrLN, int prefix) {
        if(eNOrLN.length == 3) {
            byte[] pre = new byte[1];
            pre[0] = (byte)(prefix & 0xFF);
            eNOrLN[0] = pre;
            return 1;
        }else {
            return 0;
        }
    }

    /**
     * Usage:
     *      if(Node.isExtensionNode(node)) {
     *          //something to do with the extension node
     *      }
     * created in 21:11 2018/6/17
     */
    public static boolean isExtensionNode(byte[][] node) {
        return (node.length == 3) && ((node[0][0] & 0xFF) == EXTENSION_PREFIX);
    }

    /**
     * Usage:
     *      if(Node.isLeafNode(node)) {
     *          //something to do with the leaf node
     *      }
     * created in 21:11 2018/6/17
     */
    public static boolean isLeafNode(byte[][] node) {
        return (node.length == 3) && ((node[0][0] & 0xFF) == LEAF_PREFIX);
    }

    /**
     * Usage:
     *      if(Node.isBranchNode(node)) {
     *          //something to do with the branch node
     *      }
     * created in 21:11 2018/6/17
     */
    public static boolean isBranchNode(byte[][] node) {
        return node.length == 17;
    }
}
