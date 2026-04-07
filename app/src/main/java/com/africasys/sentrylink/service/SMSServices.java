package com.africasys.sentrylink.service;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.telephony.SmsManager;
import android.util.Log;

public class SMSServices {
    private final static String TAG = "SL-SMSServices";

    public static void sendSMS(String phoneNumber, String message) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            // Message envoyé avec succès
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Erreur lors de l'envoi du SMS", e);
            // Gérer l'échec (ex: pas de réseau, permission refusée)
        }
    }

    public static void readSMS(Context context) {
        Uri uri = Uri.parse("content://sms/inbox");
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);

        assert cursor != null;
        if (cursor.moveToFirst()) {
            do {
                String adresse = cursor.getString(cursor.getColumnIndexOrThrow("address"));
                String corps = cursor.getString(cursor.getColumnIndexOrThrow("body"));

                System.out.println("De : " + adresse + " | Message : " + corps);
            } while (cursor.moveToNext());
        }
        cursor.close();
    }

}
