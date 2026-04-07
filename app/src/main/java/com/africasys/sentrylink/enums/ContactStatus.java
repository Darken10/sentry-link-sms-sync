package com.africasys.sentrylink.enums;

/** Statut d'un contact. */
public enum ContactStatus {
    ACTIVE,
    SUSPENDED,
    DESTROYED,
    AUTO_DESTROYED,
    INACTIVE;

    public static ContactStatus fromString(String value) {
        if (value == null) return null;
        try {
            return ContactStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
