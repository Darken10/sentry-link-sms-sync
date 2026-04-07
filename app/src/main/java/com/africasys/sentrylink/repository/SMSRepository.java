package com.africasys.sentrylink.repository;

import android.content.Context;
import com.africasys.sentrylink.models.SMSMessage;
import java.util.List;

/**
 * Implémentation du Repository pour l'accès aux données SMS
 * Utilise Room Database comme ORM
 */
public class SMSRepository implements ISmsRepository {

    private final SmsDao smsDao;

    public SMSRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        this.smsDao = database.smsDao();
    }

    @Override
    public long saveSMS(SMSMessage sms) {
        return smsDao.insert(sms);
    }

    @Override
    public List<SMSMessage> getAllSMS() {
        return smsDao.getAllSMS();
    }

    @Override
    public List<SMSMessage> getSmsByPhoneNumber(String phoneNumber) {
        return smsDao.getSmsByPhoneNumber(phoneNumber);
    }

    @Override
    public List<SMSMessage> getSmsByType(int type) {
        return smsDao.getSmsByType(type);
    }

    @Override
    public SMSMessage getSmsById(Long id) {
        return smsDao.getById(id);
    }

    @Override
    public int deleteSMS(Long id) {
        return smsDao.deleteById(id);
    }

    @Override
    public int updateSMS(SMSMessage sms) {
        return smsDao.update(sms);
    }

    @Override
    public int getTotalCount() {
        return smsDao.getTotalCount();
    }

    @Override
    public int getCountByType(int type) {
        return smsDao.getCountByType(type);
    }
}

