package com.africasys.sentrylink.smssync.service.message;

import android.content.Context;
import com.africasys.sentrylink.smssync.dtos.SMSDecryptedDTO;

/**
 * Interface de stratégie pour le traitement d'un message déchiffré selon son type.
 */
public interface MessageHandler {
    /**
     * Traite un message déchiffré.
     *
     * @param context Contexte Android
     * @param sender numéro expéditeur
     * @param dto objet DTO parsé (type, message, timestamp, unityId)
     * @param receivedTimestamp horodatage du SMS reçu
     * @param prefix préfixe SL0/SL1
     */
    void handle(Context context, String sender, SMSDecryptedDTO dto, long receivedTimestamp, String prefix);
}

