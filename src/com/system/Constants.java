package com.system;

import java.io.File;

public class Constants {
    /**
     * Current Path
     */
    public final static String CURRENT_DIR = System.getProperty("user.dir");

    /**
     * Properties Path
     */
    public final static String CONFIG_DATABASE = CURRENT_DIR + File.separator + "config" + File.separator
            + "LevelDB";

    public final static String WORK_BOOK = CURRENT_DIR + File.separator + "xls" + File.separator
            + "test.xls";
}
