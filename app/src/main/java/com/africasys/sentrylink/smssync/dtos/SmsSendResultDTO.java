package com.africasys.sentrylink.smssync.dtos;

/**
 * DTO pour les résultats d'envoi de SMS
 */
public class SmsSendResultDTO {
    private boolean success;
    private String message;
    private Long smsId;

    public SmsSendResultDTO() {}

    public SmsSendResultDTO(boolean success, String message, Long smsId) {
        this.success = success;
        this.message = message;
        this.smsId = smsId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getSmsId() {
        return smsId;
    }

    public void setSmsId(Long smsId) {
        this.smsId = smsId;
    }

    @Override
    public String toString() {
        return "SmsSendResultDTO{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", smsId=" + smsId +
                '}';
    }
}

