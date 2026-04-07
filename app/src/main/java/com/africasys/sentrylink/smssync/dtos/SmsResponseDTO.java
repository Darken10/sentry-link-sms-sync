package com.africasys.sentrylink.smssync.dtos;

/**
 * DTO pour la réponse d'un SMS
 */
public class SmsResponseDTO {
    private Long id;
    private String phoneNumber;
    private String messageBody;
    private long timestamp;
    private int messageType;
    private String formattedDate;

    public SmsResponseDTO() {}

    public SmsResponseDTO(Long id, String phoneNumber, String messageBody, long timestamp, int messageType, String formattedDate) {
        this.id = id;
        this.phoneNumber = phoneNumber;
        this.messageBody = messageBody;
        this.timestamp = timestamp;
        this.messageType = messageType;
        this.formattedDate = formattedDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public void setMessageBody(String messageBody) {
        this.messageBody = messageBody;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getMessageType() {
        return messageType;
    }

    public void setMessageType(int messageType) {
        this.messageType = messageType;
    }

    public String getFormattedDate() {
        return formattedDate;
    }

    public void setFormattedDate(String formattedDate) {
        this.formattedDate = formattedDate;
    }

    @Override
    public String toString() {
        return "SmsResponseDTO{" +
                "id=" + id +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", messageBody='" + messageBody + '\'' +
                ", timestamp=" + timestamp +
                ", messageType=" + messageType +
                ", formattedDate='" + formattedDate + '\'' +
                '}';
    }
}

