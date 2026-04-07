package com.africasys.sentrylink.dtos;

/**
 * DTO pour la requête d'envoi de SMS
 */
public class SmsRequestDTO {
    private String phoneNumber;
    private String messageBody;

    public SmsRequestDTO() {}

    public SmsRequestDTO(String phoneNumber, String messageBody) {
        setPhoneNumber(phoneNumber);
        setMessageBody(messageBody);
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Le numéro de téléphone ne peut pas être vide");
        }
        this.phoneNumber = phoneNumber;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public void setMessageBody(String messageBody) {
        if (messageBody == null || messageBody.trim().isEmpty()) {
            throw new IllegalArgumentException("Le message ne peut pas être vide");
        }
        this.messageBody = messageBody;
    }

    @Override
    public String toString() {
        return "SmsRequestDTO{" +
                "phoneNumber='" + phoneNumber + '\'' +
                ", messageBody='" + messageBody + '\'' +
                '}';
    }
}

