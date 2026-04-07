package com.africasys.sentrylink.service;

import com.africasys.sentrylink.dtos.SmsRequestDTO;
import com.africasys.sentrylink.dtos.SmsResponseDTO;
import com.africasys.sentrylink.dtos.SmsSendResultDTO;
import java.util.List;

/**
 * Interface du Service SMS pour la logique métier
 */
public interface ISmsService {

    /**
     * Envoie un SMS via le système Android
     */
    SmsSendResultDTO sendSMS(SmsRequestDTO request);

    /**
     * Récupère tous les SMS stockés
     */
    List<SmsResponseDTO> getAllSMS();

    /**
     * Récupère les SMS d'un contact spécifique
     */
    List<SmsResponseDTO> getSMSByContact(String phoneNumber);

    /**
     * Récupère les SMS reçus (inbox)
     */
    List<SmsResponseDTO> getReceivedSMS();

    /**
     * Récupère les SMS envoyés
     */
    List<SmsResponseDTO> getSentSMS();

    /**
     * Récupère un SMS par son ID
     */
    SmsResponseDTO getSMSById(Long id);

    /**
     * Supprime un SMS
     */
    boolean deleteSMS(Long id);

    /**
     * Compte le nombre total de SMS
     */
    int getTotalSMSCount();

    /**
     * Compte les SMS reçus
     */
    int getReceivedSMSCount();

    /**
     * Compte les SMS envoyés
     */
    int getSentSMSCount();
}
