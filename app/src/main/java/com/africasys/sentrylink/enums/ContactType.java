package com.africasys.sentrylink.enums;

/** Type de contact. */
public enum ContactType {
    CENTRAL_SERVER,
    UNIT;

    public static ContactType fromString(String value) {
        if (value == null) return null;
        try {
            return ContactType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
