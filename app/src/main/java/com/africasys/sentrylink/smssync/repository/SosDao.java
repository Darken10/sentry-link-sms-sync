package com.africasys.sentrylink.smssync.repository;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.africasys.sentrylink.smssync.models.SosAlert;

import java.util.List;

@Dao
public interface SosDao {

    @Insert
    long insert(SosAlert alert);

    @Update
    int update(SosAlert alert);

    @Query("SELECT * FROM sos_alerts ORDER BY timestamp DESC")
    List<SosAlert> getAll();

    @Query("SELECT * FROM sos_alerts ORDER BY timestamp DESC LIMIT 1")
    SosAlert getLatest();

    @Query("SELECT * FROM sos_alerts WHERE status = 'PENDING' ORDER BY timestamp DESC")
    List<SosAlert> getPending();

    @Query("SELECT COUNT(*) FROM sos_alerts")
    int getCount();

    @Query("DELETE FROM sos_alerts WHERE id = :id")
    int deleteById(Long id);
}

