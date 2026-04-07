package com.africasys.sentrylink.smssync.enums;

/** Type de clé de chiffrement. */
public enum KeyType {
    /** Clé unique propre à un appareil/contact. */
    DEVICE_UNIQUE,
    /** Clé universelle partagée. */
    UNIVERSAL;

    public static KeyType fromString(String value) {
        if (value == null) return null;
        try {
            return KeyType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
