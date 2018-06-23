package com.db;

import com.encoding.HexConver;
import com.encoding.RLP;
import com.system.Constants;
import com.system.Trie;
import com.system.node.Node;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.util.CellRangeAddress;
import org.iq80.leveldb.*;
import static org.fusesource.leveldbjni.JniDBFactory.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class Data {
    private final static String head = "ROOT";
    private final static String mHexStr = "0123456789ABCDEF";

    private static DB dataBase;
    private static byte[] headHash;
    private static String compareResult;

    /**
     * Usage:
     Data.initialize();
     * created in 13:10 2018/6/16
     */
    public static void initialize() throws IOException {
        Options options = new Options();
        options.createIfMissing(true);
        dataBase = factory.open(new File(Constants.CONFIG_DATABASE), options);
        headHash = get(bytes(head));
    }

    /**
     * Usage:
     Data.close();
     * created in 13:14 2018/6/16
     */
    public static void close() throws IOException {
        dataBase.close();
    }

    /**
     * Usage:
     WriteBatch batch = Data.createWriteBatch();
     try {
     batch.delete(bytes("Denver"));
     batch.put(bytes("Tampa"), bytes("green"));
     batch.put(bytes("London"), bytes("red"));

     Data.write(batch);
     } finally {
     // Make sure you close the batch to avoid resource leaks.
     batch.close();
     }
     * created in 13:14 2018/6/16
     */
    public static WriteBatch createWriteBatch() {
        return dataBase.createWriteBatch();
    }
    public static void write(WriteBatch batch) {
        dataBase.write(batch);
    }

    /**
     * Usage:
     Data.delete(key);
     * created in 13:21 2018/6/16
     */
    public static void delete(byte[] key) {
        dataBase.delete(key);
    }

    /**
     * Usage:
     Data.get(key);
     * created in 13:21 2018/6/16
     */
    public static byte[] get(byte[] key) {
        return dataBase.get(key);
    }

    /**
     * Usage:
     DBIterator iterator = Data.iterator();
     try {
     for(iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
     String key = asString(iterator.peekNext().getKey());
     String value = asString(iterator.peekNext().getValue());
     System.out.println(key+" = "+value);
     }
     } finally {
     // Make sure you close the iterator to avoid resource leaks.
     iterator.close();
     }
     * created in 13:37 2018/6/16
     */
    public static DBIterator iterator() {
        return dataBase.iterator();
    }

    /**
     * Usage:
     Data.put(key,value);
     * created in 13:21 2018/6/16
     */
    public static void put(byte[] key, byte[] value) {
        dataBase.put(key,value);
    }

    /**
     * Usage:
     *      int[] result = Data.survey();
     *@return      result[0]    Number of Leaf Nodes
     *              result[1]    Number of Branch Nodes
     *              result[2]    Number of Extension Nodes
     * Warning:
     *      1.Have to initialize the data base first(and close after that)
     * created in 13:37 2018/6/16
     */
    public static int[] survey() {
        int[] result = new int[4];
        DBIterator iterator = iterator();
        for(iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
            if(iterator.peekNext().getKey() == bytes("key")) {
                headHash = iterator.peekNext().getValue();
            }else if(Node.isLeafNode(Objects.requireNonNull(RLP.rlpDecoding(iterator.peekNext().getValue())))) {
                result[0]++;
            }else if(Node.isBranchNode(Objects.requireNonNull(RLP.rlpDecoding(iterator.peekNext().getValue())))) {
                result[1]++;
            }else if(Node.isExtensionNode(Objects.requireNonNull(RLP.rlpDecoding(iterator.peekNext().getValue())))) {
                result[2]++;
            }else
                result[3]++;
        }
        try {
            close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Tip:
     *      Pay attention to both @return and String compareResult
     * Warning:
     *      1.Have to initialize the data base first(and close after that)
     * created in 0:50 2018/6/21
     */
    public static List<byte[]> compare(byte[] hashHeadLocal, byte[] hashHeadRemote) {
        List<byte[]> result = new ArrayList<>();
        result.add(hashHeadRemote);

        byte[][] nodeLocal = RLP.rlpDecoding(Data.get(hashHeadLocal));
        byte[][] nodeRemote = RLP.rlpDecoding(Data.get(hashHeadRemote));
        byte[] hashPresent = new byte[0];

        // Nothing Wrong With The Root
        if(hashHeadLocal == hashHeadRemote) {
            compareResult = "Matched";
            return null;
        }
        // The Unmatched Node Is The Root
        else if(Node.isLeafNode(Objects.requireNonNull(nodeLocal)) || Node.isLeafNode(Objects.requireNonNull(nodeRemote))) {
            compareResult = "Root Unmatched";
            return result;
        }

        while(!Node.isLeafNode(Objects.requireNonNull(nodeLocal)) &&
                !Node.isLeafNode(Objects.requireNonNull(nodeRemote))) {

            // Node Matched, Hash Incorrect
            if(nodeLocal==nodeRemote) {
                if(hashPresent != new byte[0]) {
                    result.add(hashPresent);
                }
                compareResult = "Update Error";
                return result;
            }
            // Structure Incorrect
            else if((Node.getType(nodeRemote) == Node.UNKNOWN_TYPE) ||
                    (Node.getType(nodeLocal) != Node.getType(nodeRemote))) {
                if(hashPresent != new byte[0]) {
                    result.add(hashPresent);
                    compareResult = "Structure Incorrect";
                    return result;
                }else {
                    compareResult = "Root Structure Incorrect";
                    return result;
                }
            }else if(Node.isBranchNode(nodeRemote)) {
                boolean temp = true;
                for(int i = 0; i < 16; i++) {
                    if(nodeLocal[i] != nodeRemote[i]) {
                        // Something Wrong With Branch Structure
                        if(nodeLocal[i] == null || nodeRemote[i] == null) {
                            result.add(hashPresent);
                            compareResult = "Branch Node Structure Incorrect";
                            return result;
                        }else {
                            nodeLocal = Node.getBranchChild(nodeLocal,i);
                            nodeRemote = Node.getBranchChild(nodeRemote,i);
                            if(hashPresent != new byte[0]) {
                                result.add(hashPresent);
                            }
                            hashPresent = Objects.requireNonNull(nodeRemote)[i];
                            temp = false;
                            break;
                        }
                    }
                }

                // Something Wrong With Updating Value Of Branch Node
                if(temp) {
                    if(hashPresent != new byte[0]) {
                        result.add(hashPresent);
                    }
                    compareResult = "Branch Node Update Error";
                    return result;
                }
            }else if(Node.isExtensionNode(nodeRemote)) {
                // Extension Node Nibble Unmatched
                if(nodeLocal[1] != nodeRemote[1]) {
                    if(hashPresent != new byte[0]) {
                        result.add(hashPresent);
                    }
                    compareResult = "Extension Node Structure Incorrect";
                    return result;
                }else {
                    nodeLocal = Node.getExtensionChild(nodeLocal);
                    nodeRemote = Node.getExtensionChild(nodeRemote);
                    if(hashPresent != new byte[0]) {
                        result.add(hashPresent);
                    }
                    hashPresent = Objects.requireNonNull(nodeRemote)[2];
                }
            }
        }

        // Successfully Find The Unmatched Leaf Node
        if(Node.isLeafNode(nodeRemote) && Node.isLeafNode(nodeLocal)) {
            if (hashPresent != new byte[0]) {
                result.add(hashPresent);
            }
            compareResult = "Unmatched Leaf Node Found";
        }
        // Structure Incorrect
        else {
            if (hashPresent != new byte[0]) {
                result.add(hashPresent);
            }
            compareResult = "Structure Incorrect";
        }
        return result;
    }

    /**
     * Usage:
     *      System.out.println(Data.getCompareResult());
     * created in 0:50 2018/6/21
     */
    public static String getCompareResult() {
        return compareResult;
    }

    /**
     * Usage:
     *      result = Data.update(map);
     * Warning:
     *      1.Have to initialize the data base first(and close after that)
     * created in 0:50 2018/6/21
     */
    public static String update(List<byte[]> map) {
        // Remove Duplicated Elements
        LinkedHashSet<byte[]> set = new LinkedHashSet<>(map);
        map = new ArrayList<>(set);

        // Check Root
        if((headHash != null) && (headHash != map.get(0))) {
            return "Root Not Matched";
        }else {
            // Reverse
            Collections.reverse(map);
            // Get Iterator
            Iterator<byte[]> iterator = map.iterator();

            byte[] hashPresent;
            byte[] hashPreOrigin = new byte[0];
            byte[] hashPre = new byte[0];

            while(iterator.hasNext()) {
                hashPresent = iterator.next();

                if(hashPresent == map.get(0)) {
                    // Case When It's The Leaf
                    hashPre = Node.updateNode(hashPresent,null,null);
                }else {
                    // Case When It's A Middle Node
                    hashPre = Node.updateNode(hashPresent,hashPreOrigin,hashPre);
                }

                if(iterator.hasNext()) {
                    hashPreOrigin = hashPresent;
                }else {
                    // Case When It's The Root
                    if(headHash != null) {
                        Data.delete(bytes(head));
                    }
                    Data.put(bytes(head),hashPre);
                    headHash = hashPre;
                }
            }
        }
        return "Update Successful";
    }

    /**
     * Usage:
     *      Data.output();
     * created in 14:26 2018/6/23
     */
    public static void output() {
        try {
            initialize();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Create Work Book
        HSSFWorkbook workBook = new HSSFWorkbook();

        // Create Style
        HSSFCellStyle style = workBook.createCellStyle();
        HSSFFont font = workBook.createFont();
        font.setFontHeightInPoints((short) 16);
        font.setColor(HSSFColor.GREEN.index);
        style.setFont(font);

        HSSFCellStyle regular = workBook.createCellStyle();
        font.setFontHeightInPoints((short) 9);
        font.setColor(HSSFColor.BLACK.index);
        regular.setFont(font);

        // Create Work Sheet
        SimpleDateFormat time=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String TimeString = time.format(new java.util.Date());
        HSSFSheet sheet = workBook.createSheet(TimeString);

        // Write Root
        HSSFRow row = sheet.createRow(0);
        HSSFCell cell = row.createCell(0);
        cell.setCellValue(head);
        cell.setCellStyle(style);

        cell = row.createCell(1);
        byte[] root = get(bytes(head));
        if(root != null) {
            cell.setCellValue(HexConver.byte2HexStr(root,root.length));
        }else {
            cell.setCellValue("NULL");
        }
        cell.setCellStyle(style);

        int k = 1;
        DBIterator iterator = iterator();
        for(iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
            try {
                if(new String(iterator.peekNext().getKey(),"UTF-8").equals(head)) {
                    continue;
                }else {
                    row = sheet.createRow(k);
                    byte[] key = iterator.peekNext().getKey();
                    byte[][] node = RLP.rlpDecoding(iterator.peekNext().getValue());
                    if(Node.isLeafNode(Objects.requireNonNull(node))) {
                        cell = row.createCell(0);
                        cell.setCellValue("Leaf");
                        cell.setCellStyle(style);
                        cell = row.createCell(2);
                        cell.setCellValue("Key-End");
                        cell.setCellStyle(style);
                        cell = row.createCell(4);
                        cell.setCellValue("Value");
                        cell.setCellStyle(style);

                        cell = row.createCell(1);
                        cell.setCellValue(HexConver.byte2HexStr(key,key.length));
                        cell.setCellStyle(regular);
                        cell = row.createCell(3);
                        cell.setCellValue(HexConver.byte2HexStr(node[1],node[1].length));
                        cell.setCellStyle(regular);
                        cell = row.createCell(5);
                        cell.setCellValue(HexConver.byte2HexStr(node[2],node[2].length));
                        cell.setCellStyle(regular);
                    }else if(Node.isBranchNode(Objects.requireNonNull(RLP.rlpDecoding(iterator.peekNext().getValue())))) {
                        cell = row.createCell(0);
                        cell.setCellValue("Branch");
                        cell.setCellStyle(style);
                        cell = row.createCell(1);
                        cell.setCellValue(HexConver.byte2HexStr(key,key.length));
                        cell.setCellStyle(regular);
                        int j = 1;
                        for(int i = 0; i < 16; i++) {
                            if(node[i] != null) {
                                cell = row.createCell(2*j);
                                cell.setCellValue(mHexStr.charAt(i));
                                cell.setCellStyle(style);

                                cell = row.createCell(2*j+1);
                                cell.setCellValue(HexConver.byte2HexStr(node[i],node[i].length));
                                cell.setCellStyle(regular);

                                j++;
                            }
                        }
                        cell = row.createCell(2*j);
                        cell.setCellValue("Value");
                        cell.setCellStyle(style);

                        cell = row.createCell(2*j+1);
                        cell.setCellValue(HexConver.byte2HexStr(node[16],node[16].length));
                        cell.setCellStyle(regular);
                    }else if(Node.isExtensionNode(Objects.requireNonNull(RLP.rlpDecoding(iterator.peekNext().getValue())))) {
                        cell = row.createCell(0);
                        cell.setCellValue("Extension");
                        cell.setCellStyle(style);
                        cell = row.createCell(2);
                        cell.setCellValue("Shared-Nibbles");
                        cell.setCellStyle(style);
                        cell = row.createCell(4);
                        cell.setCellValue("Child");
                        cell.setCellStyle(style);

                        cell = row.createCell(1);
                        cell.setCellValue(HexConver.byte2HexStr(key,key.length));
                        cell.setCellStyle(regular);
                        cell = row.createCell(3);
                        cell.setCellValue(HexConver.byte2HexStr(node[1],node[1].length));
                        cell.setCellStyle(regular);
                        cell = row.createCell(5);
                        cell.setCellValue(HexConver.byte2HexStr(node[2],node[2].length));
                        cell.setCellStyle(regular);
                    }
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            k++;
        }

        try {
            // Output
            FileOutputStream outputStream = new FileOutputStream(new File(Constants.WORK_BOOK));
            workBook.write(outputStream);
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        byte[] value = DigestUtils.sha1("123".getBytes());
        Trie.createForLeaf(null,"1234",value);
        System.out.println("finish");
        output();
       /* try {
            initialize();
        } catch (IOException e) {
            e.printStackTrace();
        }
        int [] t = survey();
        */
        //System.out.println(String.valueOf(t[0])+String.valueOf(t[1])+String.valueOf(t[2])+String.valueOf(t[3]));
       /* byte[] value = DigestUtils.sha1("123".getBytes());
        Trie.createForLeaf(null,"1234",value);
        System.out.println("finish");
        int [] t = survey();
        try {
            close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        output();
        System.out.println(String.valueOf(t[0])+String.valueOf(t[1])+String.valueOf(t[2])+String.valueOf(t[3]));
        */

    }

}
