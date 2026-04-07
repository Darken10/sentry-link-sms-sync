package com.africasys.sentrylink.smssync.enums;

public enum EncryptionAlgo {
    AES,
    ECC_HYBRID,
    ECC_ONLY,
    RSA_512_ONLY;

    public static EncryptionAlgo fromString(String value) {
        if (value == null) return null;
        try {
            return EncryptionAlgo.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
