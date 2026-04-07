package com.africasys.sentrylink.repository;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.africasys.sentrylink.models.ConversationSummary;
import com.africasys.sentrylink.models.SMSMessage;
import java.util.List;

@Dao
public interface SmsDao {

        @Insert
        long insert(SMSMessage sms);

        @Update
        int update(SMSMessage sms);

        @Delete
        int delete(SMSMessage sms);

        @Query("SELECT * FROM sms_messages WHERE id = :id LIMIT 1")
        SMSMessage getById(Long id);

        @Query("SELECT * FROM sms_messages ORDER BY timestamp DESC")
        List<SMSMessage> getAllSMS();

        @Query("SELECT * FROM sms_messages WHERE phone_number = :phoneNumber ORDER BY timestamp DESC")
        List<SMSMessage> getSmsByPhoneNumber(String phoneNumber);

        @Query("SELECT * FROM sms_messages WHERE message_type = :type ORDER BY timestamp DESC")
        List<SMSMessage> getSmsByType(int type);

        @Query("SELECT * FROM sms_messages WHERE phone_number = :phoneNumber AND message_type = :type ORDER BY timestamp DESC")
        List<SMSMessage> getSmsByPhoneNumberAndType(String phoneNumber, int type);

        @Query("DELETE FROM sms_messages WHERE id = :id")
        int deleteById(Long id);

        @Query("SELECT COUNT(*) FROM sms_messages")
        int getTotalCount();

        @Query("SELECT COUNT(*) FROM sms_messages WHERE message_type = :type")
        int getCountByType(int type);

        /**
         * Retourne le résumé des conversations SentryLink ([SL] uniquement). Un résumé
         * par numéro de téléphone, trié du plus récent au plus ancien. Utilise le
         * comportement SQLite «bare column in aggregate» : message_body et message_type
         * proviennent du même enregistrement que MAX(timestamp).
         */
        @Query("SELECT phone_number, message_body AS lastMessageBody, " + "message_type AS lastMessageType, "
                        + "MAX(timestamp) AS lastTimestamp, " + "COUNT(*) AS messageCount " + "FROM sms_messages "
                        + "GROUP BY phone_number " + "ORDER BY lastTimestamp DESC")
        List<ConversationSummary> getConversationSummaries();

        /**
         * Retourne tous les messages SentryLink (préfixe SL1) échangés avec un numéro
         * donné, triés du plus ancien au plus récent (affichage conversation).
         */
        @Query("SELECT * FROM sms_messages " + "WHERE phone_number = :phoneNumber " + "ORDER BY timestamp ASC")
        List<SMSMessage> getSlMessagesByPhone(String phoneNumber);

        /**
         * Vérifie si un SMS existe déjà en base (anti-doublon lors du scan inbox natif).
         */
        @Query("SELECT COUNT(*) FROM sms_messages WHERE phone_number = :address AND timestamp = :timestamp AND message_type = :type")
        int countByAddressAndTimestamp(String address, long timestamp, int type);
}
