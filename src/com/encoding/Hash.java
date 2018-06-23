package com.encoding;

import org.apache.commons.codec.digest.DigestUtils;

public class Hash {

    public static byte[] getHash(byte[] rlpNode) {
        return DigestUtils.sha1(rlpNode);
    }
}