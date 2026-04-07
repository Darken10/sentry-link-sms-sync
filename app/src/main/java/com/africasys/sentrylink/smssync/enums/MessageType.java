package com.africasys.sentrylink.smssync.enums;

public enum MessageType {
    MSG,
    SOS,
    LOC,
    CMD;

    public static MessageType fromString(String value) {
        if (value == null) return null;
        try {
            return MessageType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
