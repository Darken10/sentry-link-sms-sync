package com.africasys.sentrylink.service;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.util.Log;
import com.africasys.sentrylink.dtos.SmsRequestDTO;
import com.africasys.sentrylink.dtos.SmsResponseDTO;
import com.africasys.sentrylink.dtos.SmsSendResultDTO;
import com.africasys.sentrylink.mapper.SMSMapper;
import com.africasys.sentrylink.models.SMSMessage;
import com.africasys.sentrylink.repository.ISmsRepository;
import com.africasys.sentrylink.repository.SMSRepository;
import java.util.List;

/**
 * Implémentation du Service SMS avec la logique métier
 */
public class SmsService implements ISmsService {

    private static final String TAG = "SL-SmsService";
    private static final int SMS_TYPE_INBOX = 1;
    private static final int SMS_TYPE_SENT = 2;

    private Context context;
    private ISmsRepository repository;
    private SmsManager smsManager;

    public SmsService(Context context) {
        this.context = context;
        this.repository = new SMSRepository(context);
        this.smsManager = SmsManager.getDefault();
        // Load SMS from device on initialization
        loadSMSFromDeviceContentProvider();
    }

    @Override
    public SmsSendResultDTO sendSMS(SmsRequestDTO request) {
        try {
            // Validation
            if (request == null || request.getPhoneNumber() == null || request.getMessageBody() == null) {
                return new SmsSendResultDTO(false, "Données invalides", null);
            }

            String phoneNumber = request.getPhoneNumber().trim();
            String messageBody = request.getMessageBody().trim();

            if (phoneNumber.isEmpty() || messageBody.isEmpty()) {
                return new SmsSendResultDTO(false, "Numéro ou message vide", null);
            }

            // Créer l'entité SMS
            SMSMessage sms = SMSMapper.toEntity(phoneNumber, messageBody, SMS_TYPE_SENT);

            // Sauvegarder en base de données
            long smsId = repository.saveSMS(sms);

            // Envoyer le SMS
            java.util.ArrayList<String> partsList = smsManager.divideMessage(messageBody);
            String[] parts = partsList.toArray(new String[0]);
            PendingIntent sentIntent = PendingIntent.getBroadcast(context, 0, new Intent("SMS_SENT"),
                    PendingIntent.FLAG_IMMUTABLE);
            PendingIntent deliveredIntent = PendingIntent.getBroadcast(context, 0, new Intent("SMS_DELIVERED"),
                    PendingIntent.FLAG_IMMUTABLE);

            for (String part : parts) {
                smsManager.sendTextMessage(phoneNumber, null, part, sentIntent, deliveredIntent);
            }

            Log.d(TAG, "SMS envoyé avec succès: " + phoneNumber);
            return new SmsSendResultDTO(true, "SMS envoyé avec succès", smsId);

        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'envoi du SMS", e);
            return new SmsSendResultDTO(false, "Erreur: " + e.getMessage(), null);
        }
    }

    @Override
    public List<SmsResponseDTO> getAllSMS() {
        try {
            List<SMSMessage> smsList = repository.getAllSMS();
            return SMSMapper.toResponseDTOList(smsList);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la récupération des SMS", e);
            return List.of();
        }
    }

    @Override
    public List<SmsResponseDTO> getSMSByContact(String phoneNumber) {
        try {
            List<SMSMessage> smsList = repository.getSmsByPhoneNumber(phoneNumber);
            return SMSMapper.toResponseDTOList(smsList);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la récupération des SMS du contact", e);
            return List.of();
        }
    }

    @Override
    public List<SmsResponseDTO> getReceivedSMS() {
        try {
            List<SMSMessage> smsList = repository.getSmsByType(SMS_TYPE_INBOX);
            return SMSMapper.toResponseDTOList(smsList);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la récupération des SMS reçus", e);
            return List.of();
        }
    }

    @Override
    public List<SmsResponseDTO> getSentSMS() {
        try {
            List<SMSMessage> smsList = repository.getSmsByType(SMS_TYPE_SENT);
            return SMSMapper.toResponseDTOList(smsList);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la récupération des SMS envoyés", e);
            return List.of();
        }
    }

    @Override
    public SmsResponseDTO getSMSById(Long id) {
        try {
            SMSMessage sms = repository.getSmsById(id);
            return SMSMapper.toResponseDTO(sms);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la récupération du SMS", e);
            return null;
        }
    }

    @Override
    public boolean deleteSMS(Long id) {
        try {
            int result = repository.deleteSMS(id);
            return result > 0;
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la suppression du SMS", e);
            return false;
        }
    }

    @Override
    public int getTotalSMSCount() {
        try {
            return repository.getTotalCount();
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du comptage des SMS", e);
            return 0;
        }
    }

    @Override
    public int getReceivedSMSCount() {
        try {
            return repository.getCountByType(SMS_TYPE_INBOX);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du comptage des SMS reçus", e);
            return 0;
        }
    }

    @Override
    public int getSentSMSCount() {
        try {
            return repository.getCountByType(SMS_TYPE_SENT);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du comptage des SMS envoyés", e);
            return 0;
        }
    }

    /**
     * Load SMS from device ContentProvider and save to local database
     */
    private void loadSMSFromDeviceContentProvider() {
        try {
            // Check if database is empty
            if (repository.getTotalCount() > 0) {
                Log.d(TAG, "Database is not empty, skipping import");
                return;
            }

            Uri inboxUri = Telephony.Sms.Inbox.CONTENT_URI;
            Uri sentUri = Telephony.Sms.Sent.CONTENT_URI;

            // Load received SMS
            loadSmsFromUri(inboxUri, SMS_TYPE_INBOX);

            // Load sent SMS
            loadSmsFromUri(sentUri, SMS_TYPE_SENT);

            Log.d(TAG, "SMS import completed");
        } catch (Exception e) {
            Log.e(TAG, "Error loading SMS from ContentProvider", e);
        }
    }

    /**
     * Load SMS from a specific URI
     */
    private void loadSmsFromUri(Uri uri, int smsType) {
        try {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                    String body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY));
                    long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE));

                    // Create message entity
                    SMSMessage sms = new SMSMessage();
                    sms.setAddress(address);
                    sms.setBody(body);
                    sms.setTimestamp(timestamp);
                    sms.setType(smsType);

                    // Save to database
                    repository.saveSMS(sms);

                } while (cursor.moveToNext());
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading SMS from URI: " + uri, e);
        }
    }
}
