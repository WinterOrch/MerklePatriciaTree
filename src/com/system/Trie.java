package com.system;

import com.db.Data;
import com.encoding.BytesUtils;
import com.encoding.Hash;
import com.encoding.RLPUtils;
import com.encoding.StringUtils;
import com.system.node.Node;

import java.util.*;

import static org.fusesource.leveldbjni.JniDBFactory.bytes;

/**
 * Description
 * @author Yu.Mao
 * Created in 19:02 2018/6/17
 * Modified by Frankel.Y
 */
public class Trie {
    public static final String head = "ROOT";
    public static byte[] headHash;

    private final static String mHexStr = "0123456789ABCDEF";

    /**
     * Update All the Nodes in List.
     * @param map   List of Nodes to be Updated. Starts with ROOT and Ends with new Leaf or the Last Node to be Corrected
     * Warnings:
     *      1.New Nodes MUST be Put into DB before being Added to this List !!!!!!!!!!!!!!
     *      2.Abandoned Old Branch SHOULD be Deleted from DB before Updating the New One !
     *      3.In Conclusion, ONLY Nodes on the New Branch could be Added into this List.
     * created in 14:50 2019/5/9
     */
    public static String update(List<byte[]> map) {
        // Remove Duplicated Elements
        LinkedHashSet<byte[]> set = new LinkedHashSet<>(map);
        map = new ArrayList<>(set);
        // Check Root
        if ((headHash != null) && (headHash != map.get(0))) {
            return "Root Not Matched";
        } else {
            // Reverse
            Collections.reverse(map);
            // Get Iterator
            Iterator<byte[]> iterator = map.iterator();
            byte[] hashPresent;
            byte[] hashPreOrigin = new byte[0];
            byte[] hashPre = new byte[0];
            while (iterator.hasNext()) {
                hashPresent = iterator.next();
                if (Arrays.equals(hashPresent, map.get(0))) {
                    // Case When It's The Leaf
                    hashPre = Node.updateNode(hashPresent, null, null);
                } else {
                    // Case When It's A Middle Node
                    hashPre = Node.updateNode(hashPresent, hashPreOrigin, hashPre);
                }
                if (iterator.hasNext()) {
                    hashPreOrigin = hashPresent;
                } else {
                    // Case When It's The Root
                    if (headHash != null) {
                        Data.delete(bytes(head));
                    }
                    Data.put(bytes(head), hashPre);
                    headHash = hashPre;
                }
            }
        }
        return "Update Successful";
    }

    public static byte[][] searchForLeaf(byte[] headHash, String key) {
        key = key.toUpperCase();
        // Get Root
        byte[][] node = RLPUtils.rlpDecoding(Data.get(headHash));
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
                        node = Node.getExtensionChild(node);
                    }
                } else {
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
                        find = false;
                        break;
                    } else {
                        nibbles.append(temp);
                        node = Node.getBranchChild(node, index);
                    }
                }
            }
        }
        // Judge
        if (find) {
            if (Node.isLeafNode(node)) {
                nibbles.append(Node.getNibbles(node));
                if (key.equals(nibbles.toString())) {
                    return node;
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } else {
            return null;
        }
    }


    public static String createForLeaf(byte[] headHash, String key, byte[] value) {
        byte[][] store = new byte[3][];
        int matchedLen = 0;
        key = key.toUpperCase();

        if (headHash == null) {
            // Empty Tree
            Node.setPrefix(store, Node.LEAF_PREFIX);
            Node.setNibbles(store, key);
            store[2] = value;
            Node.storeNode(store);
            Data.put(bytes(head), Hash.getHash(RLPUtils.rlpEncoding(store)));

            return "We have a new head now!";
        } else {
            // Node -> head
            byte[][] node = RLPUtils.rlpDecoding(Data.get(headHash));
            byte[] nodeHash = Data.get(headHash);

            if( null == node )
                return "Root cannot be found in DB.";
            else {
                List<byte[]> path = new ArrayList<>();
                path.add(headHash);
                if (searchForLeaf(headHash, key) == null) {
                    while (matchedLen <= key.length()) {
                        //  Find An Extension Node
                        if (Node.isExtensionNode(node)) {
                            String currentNibble = Objects.requireNonNull(Node.getNibbles(node));
                            int sharedKeyLen = StringUtils.findSharedLenth(currentNibble, key.substring(matchedLen));

                            //  Situation 0: Extension Matched, Move to Its Branch Child
                            if (sharedKeyLen == currentNibble.length()) {
                                matchedLen = matchedLen + sharedKeyLen;

                                //  Situation 0.1: Key Out, Value Will be Put in Branch
                                if(matchedLen == key.length()) {
                                    byte[][] childBranchNode = Node.getExtensionChild(node);
                                    byte[] childHash = BytesUtils.copy(node[2]);
                                    Node.setBranchValue(childBranchNode, value);
                                    //  Load Update List
                                    path.add(nodeHash);
                                    path.add(Node.overwriteNode(childHash,childBranchNode));

                                    break;
                                }
                                //  Situation 0.2: Move On
                                else {
                                    //  Node -> Extension Child
                                    path.add(nodeHash);
                                    nodeHash = BytesUtils.copy(node[1]);
                                    node = Node.getExtensionChild(node);
                                }
                            }
                            //  Situation 1: Extension Unmatched, Split With a New Branch Node
                            else if (sharedKeyLen == 0) {
                                //  Situation 1.1: Extension is Only 1-Char Long, Thus Will be Replaced with A Branch Node
                                if(currentNibble.length()==1) {
                                    //  Hang Original Child
                                    byte[][] newBranchNode = new byte[Node.BRANCH_LEN][];
                                    newBranchNode[mHexStr.indexOf(currentNibble.charAt(0))] = BytesUtils.copy(node[1]);
                                    //  Situation 1.1.0: Key Out, Put Value in Branch
                                    if(key.length() == (matchedLen + sharedKeyLen)) {
                                        Node.setBranchValue(newBranchNode, value);
                                        //  Load Update List
                                        path.add(Node.overwriteNode(nodeHash, newBranchNode));     // Extension Node Replaced

                                        break;
                                    }
                                    //  Situation 1.1.1: Create Leaf
                                    else {
                                        //  Create Leaf
                                        byte[][] newLeafNode = new byte[Node.LEAF_LEN][];
                                        Node.setPrefix(newLeafNode, Node.LEAF_PREFIX);
                                        Node.setNibbles(newLeafNode, key.substring(matchedLen + 1));
                                        newLeafNode[2] = BytesUtils.copy(value);
                                        //  Hang Leaf
                                        Node.setChild(newBranchNode,mHexStr.indexOf(currentNibble.charAt(matchedLen)),newLeafNode);
                                        //  Load Update List
                                        path.add(Node.overwriteNode(nodeHash, newBranchNode));     // Extension Node Replaced
                                        path.add(Node.storeNode(newLeafNode));

                                        break;
                                    }
                                }
                                //  Situation 1.2: Extension is Longer than 1-Char, Thus will be Split into A Branch Node and A Extension Node
                                else {
                                    //  Update Current
                                    String newNibble = currentNibble.substring(1);
                                    Node.setNibbles(node,newNibble);
                                    byte[] newHash = Node.storeNode(node);
                                    //  Create Branch
                                    byte[][] newBranchNode = new byte[Node.BRANCH_LEN][];
                                    newBranchNode[mHexStr.indexOf(currentNibble.charAt(0))] = BytesUtils.copy(newHash);

                                    //  Situation 1.2.0: Key Out, Put Value in Branch
                                    if(key.length() == (matchedLen + sharedKeyLen)) {
                                        Node.setBranchValue(newBranchNode, value);
                                        //  Load Update List
                                        path.add(Node.overwriteNode(nodeHash, newBranchNode));     // Extension Node Replaced

                                        break;
                                    }
                                    //  Situation 1.2.1: Create Leaf
                                    else {
                                        //  Create Leaf
                                        byte[][] newLeafNode = new byte[Node.LEAF_LEN][];
                                        Node.setPrefix(newLeafNode, Node.LEAF_PREFIX);
                                        Node.setNibbles(newLeafNode, key.substring(matchedLen + 1));
                                        byte[] leafHash = Node.storeNode(newLeafNode);
                                        newBranchNode[mHexStr.indexOf(currentNibble.charAt(matchedLen))] = BytesUtils.copy(leafHash);
                                        //  Load Update List
                                        path.add(Node.overwriteNode(nodeHash, newBranchNode));     // Extension Node Replaced
                                        path.add(leafHash);

                                        break;
                                    }
                                }
                            }
                            //  Situation 1.3: Extension Longer than 1-Char, to be Split into Two Branch Nodes and An Extension Node
                            else {
                                //  Shorten Current Extension Nibble
                                Node.setNibbles(node, currentNibble.substring(0, sharedKeyLen - 1));
                                //  Create New Extension Node
                                byte[][] newExtensionNode = new byte[Node.EXTENSION_LEN][];
                                //  New Nibble is 0-Char Long, Hang Child Directly
                                if(sharedKeyLen == currentNibble.length() - 1) {
                                    newExtensionNode = Node.getExtensionChild(node);
                                }
                                //  Create New Extension
                                else {
                                    Node.setPrefix(newExtensionNode, Node.EXTENSION_PREFIX);
                                    Node.setNibbles(newExtensionNode, currentNibble.substring(sharedKeyLen + 1));
                                    Node.setChild(newExtensionNode,Node.getExtensionChild(node));
                                }
                                //  Create New Branch Node
                                byte[][] newBranchNode = new byte[Node.BRANCH_LEN][];
                                //  Hang New Extension Node
                                Node.setChild(newBranchNode,mHexStr.indexOf(currentNibble.charAt(sharedKeyLen)),newExtensionNode);

                                //  Situation 1.3.0: Key Out, Put Value in Branch
                                if(key.length() == (matchedLen + sharedKeyLen)) {
                                    Node.setBranchValue(newBranchNode, value);
                                    Node.setChild(node,newBranchNode);
                                    //  Change Old Branch
                                    Node.storeNode(newExtensionNode);
                                    //  Load Update List
                                    path.add(Node.overwriteNode(nodeHash, node));
                                    path.add(Node.storeNode(newBranchNode));

                                    break;
                                }
                                //  Situation 1.3.1: Create Leaf
                                else{
                                    //  Create New Leaf Node
                                    byte[][] newLeafNode = new byte[Node.LEAF_LEN][];
                                    Node.setPrefix(newLeafNode, Node.LEAF_PREFIX);
                                    newLeafNode[2] = BytesUtils.copy(value);
                                    Node.setNibbles(newLeafNode,key.substring(matchedLen + sharedKeyLen + 1));
                                    //  Hang Leaf
                                    Node.setChild(newBranchNode,mHexStr.indexOf(key.charAt(matchedLen + sharedKeyLen)),newLeafNode);
                                    Node.setChild(node,newBranchNode);
                                    //  Change Old Branch
                                    Node.storeNode((newExtensionNode));
                                    //  Load Update List
                                    path.add(Node.overwriteNode(nodeHash, node));
                                    path.add(Node.storeNode(newBranchNode));
                                    path.add(Node.storeNode(newLeafNode));

                                    break;
                                }
                            }
                        }
                        //  Find A Branch Node
                        else if (Node.isBranchNode(node)) {
                            //  Situation DEBUG: Key Out
                            if(key.length() == matchedLen) {
                                Node.setBranchValue(node,value);
                                //  Update List
                                path.add(Node.overwriteNode(nodeHash,node));

                                break;
                            }
                            int nPos = mHexStr.indexOf(key.charAt(matchedLen));
                            byte[] posReservedHash = BytesUtils.copy(node[nPos]);
                            byte[][] posReserved = Node.getBranchChild(node, nPos);
                            //  Situation 2.0: Branch Has No Child in Reserved Position, Hang New Leaf
                            if (null == posReserved) {
                                //  Create Leaf
                                byte[][] newLeafNode = new byte[Node.LEAF_LEN][];
                                Node.setPrefix(newLeafNode, Node.LEAF_PREFIX);
                                Node.setNibbles(newLeafNode, key.substring(matchedLen + 1));
                                newLeafNode[2] = BytesUtils.copy(value);
                                Node.setChild(node, nPos, newLeafNode);
                                //  Update List
                                path.add(nodeHash);
                                path.add(Node.storeNode(newLeafNode));

                                break;
                            }
                            //  Situation 2.1: Branch Node in Reserved Position
                            else if (Node.isBranchNode(posReserved)) {
                                //  Situation 2.1.1: Key Out, Set Branch Value
                                if(key.length() == matchedLen + 1) {
                                    //  Set Value
                                    Node.setBranchValue(posReserved,value);
                                    //  Update Link
                                    path.add(nodeHash);
                                    path.add(Node.overwriteNode(node[nPos],posReserved));

                                    break;
                                }
                                //  Situation 2.1.0: Not Out, Move On
                                else {
                                    //  Node -> Branch Child
                                    matchedLen++;

                                    path.add(nodeHash);
                                    nodeHash = BytesUtils.copy(node[nPos]);
                                    node = Node.getBranchChild(node,nPos);
                                }
                            }
                            //  Situation 2.2: Extension Node in Reserved Position, Move On
                            else if(Node.isExtensionNode(posReserved)) {
                                //  Node -> Branch Child
                                matchedLen++;

                                path.add(nodeHash);
                                nodeHash = BytesUtils.copy(node[nPos]);
                                node = Node.getBranchChild(node,nPos);
                            }
                            //  Situation 2.3: Leaf Node in Reserved Position
                            else if(Node.isLeafNode(posReserved)) {
                                //  Create Branch Node
                                byte[][] newBranchNode = new byte[Node.BRANCH_LEN][];
                                //  Case 1: Original Leaf with Empty Nibble, Replace it With Branch Node
                                String oldLeafNibble = Node.getNibbles(posReserved);
                                if(oldLeafNibble.isEmpty()) {
                                    Node.setBranchValue(newBranchNode,BytesUtils.copy(posReserved[2]));
                                }
                                //  Case 2: Original Leaf Not Empty, Create A New One And Hang
                                else {
                                    byte[][] newShortLeafNode = new byte[Node.LEAF_LEN][];
                                    Node.setPrefix(newShortLeafNode,Node.LEAF_PREFIX);
                                    Node.setNibbles(newShortLeafNode,oldLeafNibble.substring(1));
                                    //  Hang
                                    Node.setChild(newBranchNode,mHexStr.indexOf(oldLeafNibble.charAt(0)),newShortLeafNode);
                                    //  Update Old Branch
                                    Node.storeNode(newShortLeafNode);
                                }
                                //  Situation 2.3.0: Key Out
                                if(key.length() == matchedLen + 1) {
                                    Node.setBranchValue(newBranchNode,value);
                                    //  Update List
                                    path.add(nodeHash);
                                    path.add(Node.overwriteNode(posReservedHash,newBranchNode));

                                    break;
                                }
                                //  Situation 2.3.1: Key Not Out, Create New Leaf
                                else {
                                    //  Create Leaf
                                    byte[][] newLeafNode  = new byte[Node.LEAF_LEN][];
                                    Node.setPrefix(newLeafNode,Node.LEAF_PREFIX);
                                    Node.setNibbles(newLeafNode,key.substring(matchedLen + 1));
                                    //  Hang
                                    Node.setChild(newBranchNode,mHexStr.indexOf(key.charAt(matchedLen)),newLeafNode);
                                    // Update List
                                    path.add(nodeHash);
                                    path.add(Node.overwriteNode(posReservedHash,newBranchNode));
                                    path.add(Node.storeNode(newLeafNode));

                                    break;
                                }
                            }
                        }
                        //  Find Leaf Node, Which Is Not Supposed to Happen
                        else if (Node.isLeafNode(node)) {
                            return "this leaf node is in a weird position";
                        }
                    }

                    return update(path);
                } else
                    return "we already have this data";
            }
        }
    }
}