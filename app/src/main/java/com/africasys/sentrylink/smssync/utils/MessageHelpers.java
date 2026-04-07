package com.africasys.sentrylink.smssync.utils;

import android.util.Log;

import com.africasys.sentrylink.smssync.config.MessageConfig;
import com.africasys.sentrylink.smssync.enums.MessageType;

public class MessageHelpers {
    private static final String TAG = "SL-MessageHelpers";

    public static String formatMessage(long unity_id, MessageType messageType, String content) {
        String header = generateMessageHeader(unity_id, messageType);
        String message = header + MessageConfig.MESSAGE_SEPARATOR + content;
        Log.d(TAG, "Formatted message: " + message);
        return message;
    }

    private static String generateMessageHeader(long unity_id, MessageType messageType) {
        long timestamp = System.currentTimeMillis();
        return unity_id + MessageConfig.MESSAGE_HEADER_SEPARATOR + messageType.name()
                + MessageConfig.MESSAGE_HEADER_SEPARATOR + timestamp;
    }

    /**
     * Préfixe un message chiffré avec la clé personnelle (SL0).
     */
    public static String addPersonalKeyPrefix(String message) {
        String result = MessageConfig.PERSONAL_KEY_PREFIX + message;
        Log.d(TAG, "Adding personal key prefix: " + MessageConfig.PERSONAL_KEY_PREFIX);
        return result;
    }

    /**
     * Préfixe un message chiffré avec la clé partagée de l'unité (SL1).
     */
    public static String addSharedKeyPrefix(String message) {
        String result = MessageConfig.SHARED_KEY_PREFIX + message;
        Log.d(TAG, "Adding shared key prefix: " + MessageConfig.SHARED_KEY_PREFIX);
        return result;
    }

    /**
     * @deprecated Utiliser {@link #addPersonalKeyPrefix} ou
     *             {@link #addSharedKeyPrefix}.
     */
    @Deprecated
    public static String addMessagePrefix(String message) {
        return addSharedKeyPrefix(message);
    }

    /**
     * Retourne true si le corps du SMS est un message SentryLink (SL0 ou SL1).
     */
    public static boolean isSentryLinkMessage(String messageBody) {
        if (messageBody == null || messageBody.length() < MessageConfig.PREFIX_LENGTH)
            return false;
        String prefix = messageBody.substring(0, MessageConfig.PREFIX_LENGTH);
        return MessageConfig.PERSONAL_KEY_PREFIX.equals(prefix) || MessageConfig.SHARED_KEY_PREFIX.equals(prefix);
    }

    /**
     * Extrait le préfixe SentryLink (SL0 ou SL1) du corps du SMS. Retourne null si
     * le message n'est pas un message SentryLink.
     */
    public static String getMessagePrefix(String messageBody) {
        if (!isSentryLinkMessage(messageBody))
            return null;
        return messageBody.substring(0, MessageConfig.PREFIX_LENGTH);
    }

    /**
     * Retourne true si le message a été chiffré avec la clé personnelle (préfixe
     * SL0).
     */
    public static boolean isPersonalKeyMessage(String messageBody) {
        return messageBody != null && messageBody.startsWith(MessageConfig.PERSONAL_KEY_PREFIX);
    }

    /**
     * Retourne true si le message a été chiffré avec la clé partagée (préfixe SL1).
     */
    public static boolean isSharedKeyMessage(String messageBody) {
        return messageBody != null && messageBody.startsWith(MessageConfig.SHARED_KEY_PREFIX);
    }

    /**
     * Extrait le payload chiffré en supprimant le préfixe SL0 ou SL1 (3
     * caractères).
     */
    public static String getMessageContent(String messageBody) {
        return messageBody.substring(MessageConfig.PREFIX_LENGTH);
    }
}
