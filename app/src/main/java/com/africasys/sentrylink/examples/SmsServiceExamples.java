package com.africasys.sentrylink.examples;

import android.content.Context;
import com.africasys.sentrylink.dtos.SmsRequestDTO;
import com.africasys.sentrylink.dtos.SmsResponseDTO;
import com.africasys.sentrylink.dtos.SmsSendResultDTO;
import com.africasys.sentrylink.service.SmsService;
import com.africasys.sentrylink.service.ISmsService;
import java.util.List;

/**
 * Exemples d'utilisation du service SMS
 */
public class SmsServiceExamples {

    /**
     * Exemple 1: Envoyer un SMS simple
     */
    public static void exempleEnvoyerSMS(Context context) {
        ISmsService smsService = new SmsService(context);
        
        // Créer la requête
        SmsRequestDTO request = new SmsRequestDTO("+237699999999", "Bonjour!");
        
        // Envoyer le SMS
        SmsSendResultDTO result = smsService.sendSMS(request);
        
        if (result.isSuccess()) {
            System.out.println("SMS envoyé avec succès. ID: " + result.getSmsId());
        } else {
            System.out.println("Erreur: " + result.getMessage());
        }
    }

    /**
     * Exemple 2: Récupérer tous les SMS
     */
    public static void exempleRecupererTousSMS(Context context) {
        ISmsService smsService = new SmsService(context);
        
        List<SmsResponseDTO> allSms = smsService.getAllSMS();
        
        for (SmsResponseDTO sms : allSms) {
            System.out.println("De: " + sms.getPhoneNumber());
            System.out.println("Message: " + sms.getMessageBody());
            System.out.println("Date: " + sms.getFormattedDate());
            System.out.println("Type: " + (sms.getMessageType() == 1 ? "Reçu" : "Envoyé"));
            System.out.println("---");
        }
    }

    /**
     * Exemple 3: Récupérer les SMS d'un contact spécifique
     */
    public static void exempleRecupererSMSContact(Context context) {
        ISmsService smsService = new SmsService(context);
        
        String phoneNumber = "+237699999999";
        List<SmsResponseDTO> contactSms = smsService.getSMSByContact(phoneNumber);
        
        System.out.println("SMS avec " + phoneNumber + ":");
        for (SmsResponseDTO sms : contactSms) {
            System.out.println(sms.getMessageBody());
        }
    }

    /**
     * Exemple 4: Récupérer les SMS reçus
     */
    public static void exempleRecupererSMSRecus(Context context) {
        ISmsService smsService = new SmsService(context);
        
        List<SmsResponseDTO> receivedSms = smsService.getReceivedSMS();
        System.out.println("SMS reçus: " + receivedSms.size());
        
        for (SmsResponseDTO sms : receivedSms) {
            System.out.println("Message reçu de " + sms.getPhoneNumber() + 
                             " à " + sms.getFormattedDate());
        }
    }

    /**
     * Exemple 5: Récupérer les SMS envoyés
     */
    public static void exempleRecupererSMSEnvoyes(Context context) {
        ISmsService smsService = new SmsService(context);
        
        List<SmsResponseDTO> sentSms = smsService.getSentSMS();
        System.out.println("SMS envoyés: " + sentSms.size());
        
        for (SmsResponseDTO sms : sentSms) {
            System.out.println("Message envoyé à " + sms.getPhoneNumber() + 
                             " le " + sms.getFormattedDate());
        }
    }

    /**
     * Exemple 6: Récupérer un SMS par ID
     */
    public static void exempleRecupererSMSParID(Context context) {
        ISmsService smsService = new SmsService(context);
        
        SmsResponseDTO sms = smsService.getSMSById(1L);
        
        if (sms != null) {
            System.out.println("SMS trouvé:");
            System.out.println(sms);
        } else {
            System.out.println("SMS non trouvé");
        }
    }

    /**
     * Exemple 7: Supprimer un SMS
     */
    public static void exempleSupprimerSMS(Context context) {
        ISmsService smsService = new SmsService(context);
        
        boolean deleted = smsService.deleteSMS(1L);
        if (deleted) {
            System.out.println("SMS supprimé avec succès");
        } else {
            System.out.println("Erreur lors de la suppression");
        }
    }

    /**
     * Exemple 8: Compter les SMS
     */
    public static void exempleCompterSMS(Context context) {
        ISmsService smsService = new SmsService(context);
        
        int total = smsService.getTotalSMSCount();
        int received = smsService.getReceivedSMSCount();
        int sent = smsService.getSentSMSCount();
        
        System.out.println("Total SMS: " + total);
        System.out.println("SMS reçus: " + received);
        System.out.println("SMS envoyés: " + sent);
    }

    /**
     * Exemple 9: Envoyer des SMS multiples
     */
    public static void exempleEnvoyerSMSMultiples(Context context) {
        ISmsService smsService = new SmsService(context);
        
        String[] phoneNumbers = {
            "+237699999999",
            "+237688888888",
            "+237677777777"
        };
        
        String message = "Bonjour, ceci est un message en masse!";
        
        for (String phoneNumber : phoneNumbers) {
            try {
                SmsRequestDTO request = new SmsRequestDTO(phoneNumber, message);
                SmsSendResultDTO result = smsService.sendSMS(request);
                
                if (result.isSuccess()) {
                    System.out.println("✓ SMS envoyé à " + phoneNumber);
                } else {
                    System.out.println("✗ Erreur pour " + phoneNumber + ": " + result.getMessage());
                }
            } catch (Exception e) {
                System.out.println("✗ Exception pour " + phoneNumber + ": " + e.getMessage());
            }
        }
    }

    /**
     * Exemple 10: Rechercher des SMS par contenu
     */
    public static void exempleRechercherSMS(Context context, String keyword) {
        ISmsService smsService = new SmsService(context);
        
        List<SmsResponseDTO> allSms = smsService.getAllSMS();
        
        System.out.println("SMS contenant '" + keyword + "':");
        for (SmsResponseDTO sms : allSms) {
            if (sms.getMessageBody().toLowerCase().contains(keyword.toLowerCase())) {
                System.out.println("De: " + sms.getPhoneNumber());
                System.out.println("Message: " + sms.getMessageBody());
            }
        }
    }
}
