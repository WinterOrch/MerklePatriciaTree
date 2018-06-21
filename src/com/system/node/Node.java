package com.system.node;

import com.db.Data;
import com.encoding.Hash;
import com.encoding.RLP;

import java.util.Objects;

/**
 * Tool Class For MPT Nodes
 * @author Frankel.Y
 * Created in 21:11 2018/6/17
 */
public class Node {

    public static int EXTENSION_PREFIX = 0x00;
    public static int LEAF_PREFIX = 0x02;

    public static final int LEAF_TYPE = 0;
    public static final int EXTENSION_TYPE = 1;
    public static final int BRANCH_TYPE = 2;
    public static final int UNKNOWN_TYPE = 3;

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
     *      if(Node.getType(node_1) == (Node.getType(node_2))) {
     *          // Something to do
     *      }
     * created in 23:32 2018/6/20
     */
    public static int getType(byte[][] node) {
        if(isLeafNode(node)) {
            return LEAF_TYPE;
        }else if(isBranchNode(node)) {
            return BRANCH_TYPE;
        }else if(isExtensionNode(node)) {
            return EXTENSION_TYPE;
        }else {
            return UNKNOWN_TYPE;
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

    /**
     * Usage:
     *      Node.setChild(extensionNode,branchNode);
     * created in 21:11 2018/6/17
     */
    public static void setChild(byte[][] parent, byte[][] child) {
        parent[2] = Hash.getHash(RLP.rlpEncoding(child));
    }

    /**
     * Usage:
     *      Node.setChild(branchNode,0xf,leafNode);
     * created in 21:11 2018/6/17
     */
    public static void setChild(byte[][] parent, int index, byte[][] child) {
        parent[index] = Hash.getHash(RLP.rlpEncoding(child));
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
     *      Node.setValue(branchNode);
     * created in 21:11 2018/6/17
     */
    public static void setValue(byte[][] branchNode) {
        byte[] value = new byte[0];

        for(int i = 0; i < 16; i++) {
            if(branchNode[i] != null) {
                byte[] temp = new byte[value.length];
                System.arraycopy(value,0,temp,0,value.length);
                value = new byte[temp.length + branchNode[i].length];
                System.arraycopy(temp,0,value,0,temp.length);
                System.arraycopy(branchNode[i],0,value,temp.length,branchNode[i].length);
            }
        }

        branchNode[16] = Hash.getHash(value);
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
     *      Node.storeNode(originHash,leafNode);
     * Warning:
     *      1.have to initialize the data base first(and close after that)
     * created in 21:11 2018/6/17
     */
    public static void storeNode(byte[] originHash, byte[][] node) {
        if(Data.get(originHash) == null) {
            throw new RuntimeException("Cannot Find originHash");
        }else {
            byte[] rlpNode = RLP.rlpEncoding(node);
            Data.delete(originHash);
            Data.put(originHash,rlpNode);
        }
    }

    /**
     * Usage:
     *      Node.updateNode(nodeHash,childHashOrigin, childHash);
     * Warning:
     *      1.have to initialize the data base first(and close after that)
     * created in 21:11 2018/6/17
     */
    public static byte[] updateNode(byte[] hash, byte[] childHashOrigin, byte[] childHash) {
        byte[][] node = Objects.requireNonNull(RLP.rlpDecoding(Data.get(hash)));
        byte[] rlpNode;
        byte[] h;

        if(isExtensionNode(node)){
            node[2] = childHash;
        }else if(isBranchNode(node)) {
            int i = 0;
            boolean t = true;
            for(; i < 16; i++) {
                if(node[i] == childHashOrigin) {
                    t = false;
                    break;
                }
            }
            if(t) {
                throw new RuntimeException("Branch Node Cannot Find childHashOrigin");
            }else {
                node[i] = childHash;
            }
        }

        rlpNode = RLP.rlpEncoding(node);
        Data.delete(hash);
        h = Hash.getHash(rlpNode);
        Data.put(h, rlpNode);
        return h;
    }

}
