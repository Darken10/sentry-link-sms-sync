package com.africasys.sentrylink.smssync.service;

import android.content.Context;
import android.telephony.SmsManager;
import com.africasys.sentrylink.smssync.dtos.SMSMessageDTO;
import com.africasys.sentrylink.smssync.mapper.SMSMapper;
import com.africasys.sentrylink.smssync.models.SMSMessage;
import com.africasys.sentrylink.smssync.repository.SMSRepository;
import java.util.List;

public class SMSBusinessService {
    private final SMSRepository smsRepository;

    public SMSBusinessService(Context context) {
        this.smsRepository = new SMSRepository(context);
    }

    public void sendSMS(SMSMessageDTO smsDto) {
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(smsDto.getPhoneNumber(), null, smsDto.getMessageBody(), null, null);
        
        // Save to sent folder in local DB
        smsDto.setMessageType(2); // Sent
        smsDto.setDate(System.currentTimeMillis());
        saveSMS(smsDto);
    }

    public void saveSMS(SMSMessageDTO smsDto) {
        SMSMessage entity = SMSMapper.toEntity(smsDto.getPhoneNumber(), smsDto.getMessageBody(), smsDto.getMessageType());
        smsRepository.saveSMS(entity);
    }

    public List<SMSMessageDTO> getAllMessages() {
        List<SMSMessage> entities = smsRepository.getAllSMS();
        return SMSMapper.toMessageDTOList(entities);
    }
}
