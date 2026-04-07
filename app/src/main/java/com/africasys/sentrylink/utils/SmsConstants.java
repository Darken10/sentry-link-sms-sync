package com.africasys.sentrylink.utils;

/**
 * Constantes pour la gestion des SMS
 */
public class SmsConstants {

    // Types de SMS
    public static final int SMS_TYPE_INBOX = 1;      // SMS reçu
    public static final int SMS_TYPE_SENT = 2;       // SMS envoyé
    public static final int SMS_TYPE_DRAFT = 3;      // SMS brouillon
    public static final int SMS_TYPE_DELETED = 4;    // SMS supprimé

    // Limites
    public static final int SMS_MAX_LENGTH = 160;    // Longueur max SMS (standard)
    public static final int SMS_MAX_LENGTH_UNICODE = 70; // Longueur max SMS Unicode
    public static final int PHONE_NUMBER_MIN_LENGTH = 8;

    // Tags pour le logging
    public static final String TAG_SMS_SERVICE = "SmsService";
    public static final String TAG_SMS_RECEIVER = "SmsReceiver";
    public static final String TAG_SMS_REPOSITORY = "SmsRepository";
    public static final String TAG_MAIN_ACTIVITY = "MainActivity";

    // Clés de préférences
    public static final String PREF_NAME = "sentry_link_prefs";
    public static final String PREF_LAST_SMS_SYNC = "last_sms_sync";
    public static final String PREF_SMS_NOTIFICATIONS = "sms_notifications_enabled";

    // Intent Actions
    public static final String ACTION_SMS_SENT = "SMS_SENT";
    public static final String ACTION_SMS_DELIVERED = "SMS_DELIVERED";

    // Délais (en millisecondes)
    public static final long SMS_SEND_TIMEOUT = 30000;  // 30 secondes
    public static final long DATABASE_QUERY_TIMEOUT = 5000;  // 5 secondes

    // Codes d'erreur
    public static final int ERROR_CODE_INVALID_PHONE = 1001;
    public static final int ERROR_CODE_EMPTY_MESSAGE = 1002;
    public static final int ERROR_CODE_PERMISSION_DENIED = 1003;
    public static final int ERROR_CODE_NETWORK_ERROR = 1004;
    public static final int ERROR_CODE_DATABASE_ERROR = 1005;

    /**
     * Retourne le nom du type de SMS
     */
    public static String getSmsTypeName(int type) {
        switch (type) {
            case SMS_TYPE_INBOX:
                return "Reçu";
            case SMS_TYPE_SENT:
                return "Envoyé";
            case SMS_TYPE_DRAFT:
                return "Brouillon";
            case SMS_TYPE_DELETED:
                return "Supprimé";
            default:
                return "Inconnu";
        }
    }

    /**
     * Vérifie si un numéro de téléphone est valide
     */
    public static boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        
        // Enlever les espaces, tirets, parenthèses
        String cleaned = phoneNumber.replaceAll("[\\s\\-\\(\\)]", "");
        
        // Doit contenir au moins 8 chiffres
        if (cleaned.length() < PHONE_NUMBER_MIN_LENGTH) {
            return false;
        }
        
        // Doit commencer par + ou un chiffre
        return cleaned.matches("^\\+?\\d+$");
    }

    /**
     * Vérifie si un message est valide
     */
    public static boolean isValidMessage(String message) {
        return message != null && !message.trim().isEmpty();
    }

    /**
     * Compte le nombre de parts SMS (pour les messages longs)
     */
    public static int getSmsPartCount(String message) {
        if (message == null || message.isEmpty()) {
            return 0;
        }
        
        int length = message.length();
        
        if (length <= SMS_MAX_LENGTH) {
            return 1;
        }
        
        // Pour les messages longs, chaque part fait 153 caractères (160 - 7 pour l'en-tête)
        return (length + 152) / 153;
    }

    /**
     * Formate un numéro de téléphone
     */
    public static String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return "";
        }
        
        // Enlever tous les caractères non-numériques sauf le +
        String cleaned = phoneNumber.replaceAll("[^\\d+]", "");
        
        // Si c'est un numéro camerounais sans l'indicatif pays
        if (cleaned.startsWith("237")) {
            return "+" + cleaned;
        } else if (!cleaned.startsWith("+")) {
            // Si ce n'est pas un numéro international
            return "+" + cleaned;
        }
        
        return cleaned;
    }

    /**
     * Vérifie si une chaîne contient uniquement des chiffres et des caractères de formatage
     */
    public static boolean isNumericPhone(String phoneNumber) {
        if (phoneNumber == null) {
            return false;
        }
        return phoneNumber.matches("[\\d+\\s\\-\\(\\)]+");
    }
}
