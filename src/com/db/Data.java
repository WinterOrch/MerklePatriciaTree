package com.db;

import com.system.Constants;
import org.iq80.leveldb.*;
import static org.fusesource.leveldbjni.JniDBFactory.*;
import java.io.*;
import java.util.Objects;

public class Data {

    public static void main(String[] args) {
        Options options = new Options();
        options.createIfMissing(true);
        DB db = null;
        try {
            db = factory.open(new File(Constants.CONFIG_DATABASE), options);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            // Use the db in here....
        } finally {
            // Make sure you close the db to shutdown the
            // database and avoid resource leaks.
            try {
                Objects.requireNonNull(db).close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
