package com.db;

import com.system.Constants;
import org.iq80.leveldb.*;
import static org.fusesource.leveldbjni.JniDBFactory.*;
import java.io.*;

public class Data {
    private static DB dataBase;

    /**
     * Usage:
     Data.initialize();
     * created in 13:10 2018/6/16
     */
    public static void initialize() throws IOException {
        Options options = new Options();
        options.createIfMissing(true);
        dataBase = factory.open(new File(Constants.CONFIG_DATABASE), options);
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
     Data.get(key);
     * created in 13:21 2018/6/16
     */
    public static byte[] get(byte[] key) {
        return dataBase.get(key);
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

    public static void main(String[] args) {
        try {
            initialize();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            try (WriteBatch batch = Data.createWriteBatch()) {
                batch.delete(bytes("Denver"));
                batch.put(bytes("Tampa"), bytes("green"));
                batch.put(bytes("London"), bytes("red"));

                Data.write(batch);
            } catch (IOException e) {
                e.printStackTrace();
            }
            // Make sure you close the batch to avoid resource leaks.
        } finally {
            // Make sure you close the db to shutdown the
            // database and avoid resource leaks.
            try {
                close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
