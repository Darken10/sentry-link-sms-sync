package com.africasys.sentrylink.smssync.enums;

/** Statut d'une clé de chiffrement. */
public enum KeyStatus {
    ACTIVE,
    EXPIRED,
    INACTIVE;

    public static KeyStatus fromString(String value) {
        if (value == null) return null;
        try {
            return KeyStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
