package com.africasys.sentrylink.smssync.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Classe utilitaire pour des fonctions communes
 */
public class SmsUtils {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    private static final SimpleDateFormat dateFormatLong = new SimpleDateFormat("EEEE dd MMMM yyyy HH:mm:ss", Locale.getDefault());

    /**
     * Vérifie si l'appareil a une connexion réseau
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager == null) {
            return false;
        }
        
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    /**
     * Formate une date pour l'affichage
     */
    public static String formatDate(long timestamp) {
        try {
            Date date = new Date(timestamp);
            return dateFormat.format(date);
        } catch (Exception e) {
            return new Date(timestamp).toString();
        }
    }

    /**
     * Formate une date en format long
     */
    public static String formatDateLong(long timestamp) {
        try {
            Date date = new Date(timestamp);
            return dateFormatLong.format(date);
        } catch (Exception e) {
            return new Date(timestamp).toString();
        }
    }

    /**
     * Vérifie si le message est court (peut être envoyé en une partie)
     */
    public static boolean isShortMessage(String message) {
        return message != null && message.length() <= SmsConstants.SMS_MAX_LENGTH;
    }

    /**
     * Tronque un message pour l'affichage
     */
    public static String truncateMessage(String message, int maxLength) {
        if (message == null) {
            return "";
        }
        if (message.length() <= maxLength) {
            return message;
        }
        return message.substring(0, maxLength) + "...";
    }

    /**
     * Obtient un résumé formaté d'un message SMS
     */
    public static String getSmsPreview(String message, int maxLength) {
        return truncateMessage(message, maxLength);
    }

    /**
     * Vérifie si le numéro est un numéro camerounais
     */
    public static boolean isCameroonianNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return false;
        }
        String cleaned = phoneNumber.replaceAll("[^\\d+]", "");
        return cleaned.contains("237") || cleaned.startsWith("6") || cleaned.startsWith("2370") || cleaned.startsWith("+237");
    }

    /**
     * Convertit un timestamp en durée relative (ex: "Il y a 5 minutes")
     */
    public static String getRelativeTime(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        if (diff < 60000) {
            return "À l'instant";
        } else if (diff < 3600000) {
            long minutes = diff / 60000;
            return "Il y a " + minutes + " minute(s)";
        } else if (diff < 86400000) {
            long hours = diff / 3600000;
            return "Il y a " + hours + " heure(s)";
        } else if (diff < 604800000) {
            long days = diff / 86400000;
            return "Il y a " + days + " jour(s)";
        } else {
            return formatDate(timestamp);
        }
    }

    /**
     * Obtient le nombre de SMS nécessaires pour un message
     */
    public static int getSmsCount(String message) {
        return SmsConstants.getSmsPartCount(message);
    }

    /**
     * Nettoie un numéro de téléphone (enlève les caractères spéciaux)
     */
    public static String cleanPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return "";
        }
        return phoneNumber.replaceAll("[^\\d+]", "");
    }

    /**
     * Ajoute le préfixe international si absent
     */
    public static String ensureInternationalFormat(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return "";
        }

        String cleaned = cleanPhoneNumber(phoneNumber);

        // Si commence déjà par + ou 00, laisser tel quel
        if (cleaned.startsWith("+") || cleaned.startsWith("00")) {
            return cleaned;
        }

        // Sinon ajouter +
        return "+" + cleaned;
    }

    /**
     * Crée une description formatée pour un SMS
     */
    public static String createSmsDescription(String phoneNumber, String message, long timestamp) {
        StringBuilder sb = new StringBuilder();
        sb.append("De: ").append(phoneNumber).append("\n");
        sb.append("Message: ").append(truncateMessage(message, 50)).append("\n");
        sb.append("Date: ").append(formatDate(timestamp));
        return sb.toString();
    }

    /**
     * Vérifie si un message contient du texte sensible (noms, emails, etc.)
     */
    public static boolean containsSensitiveData(String message) {
        if (message == null) {
            return false;
        }

        String lowerCase = message.toLowerCase();

        // Chercher des patterns sensibles
        return lowerCase.contains("password") ||
               lowerCase.contains("mot de passe") ||
               lowerCase.contains("credit card") ||
               lowerCase.contains("ssn") ||
               lowerCase.matches(".*\\b\\d{16}\\b.*"); // Numéro de carte (16 chiffres)
    }

    /**
     * Génère un ID unique basé sur le timestamp
     */
    public static long generateUniqueId() {
        return System.currentTimeMillis();
    }

    /**
     * Vérifie si deux numéros sont identiques (après nettoyage)
     */
    public static boolean arePhoneNumbersEqual(String phone1, String phone2) {
        String cleaned1 = cleanPhoneNumber(phone1);
        String cleaned2 = cleanPhoneNumber(phone2);
        return cleaned1.equals(cleaned2);
    }

    /**
     * Obtient les statistiques d'un message
     */
    public static String getMessageStats(String message) {
        if (message == null) {
            return "Aucun message";
        }

        int length = message.length();
        int smsCount = SmsConstants.getSmsPartCount(message);

        return "Caractères: " + length + " | SMS: " + smsCount;
    }



}
