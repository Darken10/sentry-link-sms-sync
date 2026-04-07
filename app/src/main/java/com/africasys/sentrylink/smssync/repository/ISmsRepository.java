package com.africasys.sentrylink.smssync.repository;

import com.africasys.sentrylink.smssync.models.SMSMessage;
import java.util.List;

/**
 * Interface du Repository pour l'accès aux données SMS
 */
public interface ISmsRepository {

    /**
     * Sauvegarde un SMS en base de données
     */
    long saveSMS(SMSMessage sms);

    /**
     * Récupère tous les SMS
     */
    List<SMSMessage> getAllSMS();

    /**
     * Récupère les SMS d'un numéro de téléphone spécifique
     */
    List<SMSMessage> getSmsByPhoneNumber(String phoneNumber);

    /**
     * Récupère les SMS d'un type spécifique (1=inbox, 2=sent)
     */
    List<SMSMessage> getSmsByType(int type);

    /**
     * Récupère un SMS par son ID
     */
    SMSMessage getSmsById(Long id);

    /**
     * Supprime un SMS
     */
    int deleteSMS(Long id);

    /**
     * Met à jour un SMS
     */
    int updateSMS(SMSMessage sms);

    /**
     * Compte le nombre total de SMS
     */
    int getTotalCount();

    /**
     * Compte les SMS par type
     */
    int getCountByType(int type);
}
