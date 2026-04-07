package com.africasys.sentrylink.smssync.mapper;

import com.africasys.sentrylink.smssync.models.SMSMessage;
import com.africasys.sentrylink.smssync.dtos.SmsResponseDTO;
import com.africasys.sentrylink.smssync.dtos.SMSMessageDTO;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Mapper pour convertir entre SMSMessage (Entity) et DTOs
 */
public class SMSMapper {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    /**
     * Convertit une entité SMSMessage en DTO SmsResponseDTO
     */
    public static SmsResponseDTO toResponseDTO(SMSMessage sms) {
        if (sms == null) {
            return null;
        }

        return new SmsResponseDTO(
            sms.getId(),
            sms.getAddress(),
            sms.getBody(),
            sms.getTimestamp(),
            sms.getType(),
            formatDate(sms.getTimestamp())
        );
    }

    /**
     * Convertit une liste d'entités SMSMessage en liste de DTOs SmsResponseDTO
     */
    public static List<SmsResponseDTO> toResponseDTOList(List<SMSMessage> smsList) {
        if (smsList == null) {
            return new ArrayList<>();
        }

        List<SmsResponseDTO> dtoList = new ArrayList<>();
        for (SMSMessage sms : smsList) {
            dtoList.add(toResponseDTO(sms));
        }
        return dtoList;
    }

    /**
     * Convertit un DTO SmsRequestDTO en entité SMSMessage
     */
    public static SMSMessage toEntity(String phoneNumber, String messageBody, int type) {
        SMSMessage sms = new SMSMessage();
        sms.setAddress(phoneNumber);
        sms.setBody(messageBody);
        sms.setTimestamp(System.currentTimeMillis());
        sms.setType(type);
        return sms;
    }

    /**
     * Convertit une entité SMSMessage en DTO SMSMessageDTO
     */
    public static SMSMessageDTO toMessageDTO(SMSMessage sms) {
        if (sms == null) {
            return null;
        }

        return new SMSMessageDTO(
            sms.getAddress(),
            sms.getBody(),
            sms.getTimestamp(),
            sms.getType()
        );
    }

    /**
     * Convertit une liste d'entités SMSMessage en liste de DTOs SMSMessageDTO
     */
    public static List<SMSMessageDTO> toMessageDTOList(List<SMSMessage> smsList) {
        if (smsList == null) {
            return new ArrayList<>();
        }

        List<SMSMessageDTO> dtoList = new ArrayList<>();
        for (SMSMessage sms : smsList) {
            dtoList.add(toMessageDTO(sms));
        }
        return dtoList;
    }

    /**
     * Formate un timestamp en format lisible
     */
    private static String formatDate(long timestamp) {
        try {
            return dateFormat.format(new Date(timestamp));
        } catch (Exception e) {
            return new Date(timestamp).toString();
        }
    }
}
