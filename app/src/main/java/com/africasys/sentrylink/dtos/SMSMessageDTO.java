package com.africasys.sentrylink.dtos;

public class SMSMessageDTO {
    private String phoneNumber;
    private String messageBody;
    private long date;
    private int messageType;

    public SMSMessageDTO() {}

    public SMSMessageDTO(String phoneNumber, String messageBody, long date, int messageType) {
        this.phoneNumber = phoneNumber;
        this.messageBody = messageBody;
        this.date = date;
        this.messageType = messageType;
    }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getMessageBody() { return messageBody; }
    public void setMessageBody(String messageBody) { this.messageBody = messageBody; }

    public long getDate() { return date; }
    public void setDate(long date) { this.date = date; }

    public int getMessageType() { return messageType; }
    public void setMessageType(int messageType) { this.messageType = messageType; }
}
