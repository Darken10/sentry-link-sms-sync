
package com.africasys.sentrylink.dtos;

import android.util.Log;

import com.africasys.sentrylink.config.MessageConfig;
import com.africasys.sentrylink.enums.MessageType;

import java.util.List;

public class SMSDecryptedDTO {
    private String TAG = "SL-SMSDecryptedDTO";
    private final String plaintexte;
    private boolean signatureValid;
    private MessageType type;
    private String message;
    private long timestamp;
    private long unityId;

    public SMSDecryptedDTO(String plaintexte) {
        this.plaintexte = plaintexte;
        String[] parts = plaintexte.split(MessageConfig.MESSAGE_SEPARATOR, 2);

        if (parts.length != 2) {
            this.signatureValid = false;
            return;
        }

        this.message = parts[1];
        String header = parts[0];
        String[] headerParts = header.split(MessageConfig.MESSAGE_HEADER_SEPARATOR, 3);
        if (headerParts.length != 3) {
            this.signatureValid = false;
            return;
        }

        try {
            this.unityId = Long.parseLong(headerParts[0]);
            this.type = MessageType.valueOf(headerParts[1]);
            this.timestamp = Long.parseLong(headerParts[2]);
        } catch (NumberFormatException e) {
            this.signatureValid = false;
            Log.e(TAG, "Erreur de format dans l'en-tête", e);
            return;
        } catch (IllegalArgumentException e) {
            this.signatureValid = false;
            Log.e(TAG, "Type de message inconnu dans l'en-tête", e);
            return;
        }
        this.signatureValid = true;
    }

    public void setSignatureValid(boolean signatureValid) {
        this.signatureValid = signatureValid;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getUnityId() {
        return unityId;
    }

    public void setUnityId(long unityId) {
        this.unityId = unityId;
    }

    public String getPlaintexte() {
        return plaintexte;
    }

    public boolean isSignatureValid() {
        return this.signatureValid;
    }

}
