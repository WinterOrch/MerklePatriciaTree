package com.system.node;

import com.db.Data;
import com.encoding.BytesUtils;
import com.encoding.Hash;
import com.encoding.HexConver;
import com.encoding.RLPUtils;

import java.util.Arrays;
import java.util.Objects;

/**
 * Tool Class For MPT Nodes
 * @author Frankel.Y
 * Created in 21:11 2018/6/17
 */
public class Node {
    public static final int EXTENSION_PREFIX = 0x00;
    public static final int LEAF_PREFIX = 0x02;

    public static final int LEAF_TYPE = 0;
    public static final int EXTENSION_TYPE = 1;
    public static final int BRANCH_TYPE = 2;
    public static final int UNKNOWN_TYPE = 3;

    public static final int LEAF_LEN = 3;
    public static final int EXTENSION_LEN = 3;
    public static final int BRANCH_LEN = 17;

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
            return RLPUtils.rlpDecoding(childRLP);
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
        if(branchNode.length == Node.BRANCH_LEN) {
            byte[] childRLP = Data.get(branchNode[index]);
            return RLPUtils.rlpDecoding(childRLP);
        }else {
            return null;
        }
    }

    /**
     * Usage:
     *      String keyEnd = Node.getNibbles(leafNode);
     * Warning:
     *      1.return "" when the input node is neither an extension node nor a leaf node
     * created in 21:11 2018/6/17
     */
    public static String getNibbles(byte[][] eNOrLN) {
        if(eNOrLN.length == 3 && null != eNOrLN[1]) {
            return HexConver.byte2HexStr(eNOrLN[1],eNOrLN[1].length);
        }else {
            return "";
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
        return null != node && ((node.length == 3) && ((node[0][0] & 0xFF) == EXTENSION_PREFIX));
    }

    /**
     * Usage:
     *      if(Node.isLeafNode(node)) {
     *          //something to do with the leaf node
     *      }
     * created in 21:11 2018/6/17
     */
    public static boolean isLeafNode(byte[][] node) {
        return null != node && (node.length == 3) && ((node[0][0] & 0xFF) == LEAF_PREFIX);
    }

    /**
     * Usage:
     *      if(Node.isBranchNode(node)) {
     *          //something to do with the branch node
     *      }
     * created in 21:11 2018/6/17
     */
    public static boolean isBranchNode(byte[][] node) {
        return null != node && node.length == 17;
    }

    /**
     * Usage:
     *      Node.setChild(extensionNode,branchNode);
     * created in 21:11 2018/6/17
     */
    public static void setChild(byte[][] parent, byte[][] child) {
        parent[2] = Hash.getHash(RLPUtils.rlpEncoding(child));
    }

    /**
     * Usage:
     *      Node.setChild(branchNode,0xf,leafNode);
     * created in 21:11 2018/6/17
     */
    public static void setChild(byte[][] parent, int index, byte[][] child) {
        parent[index] = Hash.getHash(RLPUtils.rlpEncoding(child));
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
            if(nibbles.isEmpty())
                eNOrLN[1] = null;
            else
                eNOrLN[1] = HexConver.hexStr2Bytes(nibbles);
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

    public static int setBranchValue(byte[][] branchNode, byte[] value) {
        if(Node.isBranchNode(branchNode)) {
            branchNode[Node.BRANCH_LEN - 1] = BytesUtils.copy(value);
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
     * @return  New Hash
     * Usage:
     *      Node.storeNode(leafNode);
     * Warning:
     *      1.have to initialize the data base first(and close after that)
     * created in 21:11 2018/6/17
     */
    public static byte[] storeNode(byte[][] node) {
        byte[] rlpNode = RLPUtils.rlpEncoding(node);
        byte[] newHash = Hash.getHash(rlpNode);
        Data.put(newHash,rlpNode);
        return newHash;
    }

    /**
     * Keep The Original Hash Unchanged. Used When Creating A Update List.
     * @return  Old Hash
     * Warning:
     *      1.have to initialize the data base first(and close after that)
     * created in 21:11 2018/6/17
     */
    public static byte[] overwriteNode(byte[] originHash, byte[][] node) {
        if(Data.get(originHash) == null) {
            throw new RuntimeException("Cannot Find originHash");
        }else {
            Data.put(originHash, RLPUtils.rlpEncoding(node));
            return originHash;
        }
    }

    /**
     * @return  New Hash
     * Usage:
     *      Node.storeNode(originHash,leafNode);
     * Warning:
     *      1.have to initialize the data base first(and close after that)
     * created in 21:11 2018/6/17
     */
    public static byte[] storeNode(byte[] originHash, byte[][] node) {
        if(Data.get(originHash) == null) {
            throw new RuntimeException("Cannot Find originHash");
        }else {
            Data.delete(originHash);
            return storeNode(node);
        }
    }

    /**
     * Usage:
     *      Node.updateNode(nodeHash, childHashOrigin, childHash);
     *
     *      @return New Hash of This Node
     * Tip:
     *      Could be Used to Refresh A Node Especially When It Has A Mismatched Hash by Setting childHashOrigin
     *      and childHash Both null
     * Warning:
     *      1.have to initialize the data base first(and close after that)
     * created in 21:11 2018/6/17
     */
    public static byte[] updateNode(byte[] hash, byte[] childHashOrigin, byte[] childHash) {
        byte[][] node = Objects.requireNonNull(RLPUtils.rlpDecoding(Data.get(hash)));
        byte[] rlpNode;
        byte[] h;

        if(isExtensionNode(node) && childHash != null){
            node[2] = childHash;
        }else if(isBranchNode(node) && childHash != null && childHashOrigin != null) {
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

        rlpNode = RLPUtils.rlpEncoding(node);
        h = Hash.getHash(rlpNode);

        // No Need to Refresh
        if(childHash == null && childHashOrigin == null) {
            if(Arrays.equals(h,hash))
                return hash;
        }

        Data.delete(hash);
        Data.put(h, rlpNode);
        return h;
    }


}