package com.africasys.sentrylink.smssync.repository;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.africasys.sentrylink.smssync.models.LocationRecord;

import java.util.List;

@Dao
public interface LocationDao {

    @Insert
    long insert(LocationRecord location);

    @Update
    int update(LocationRecord location);

    @Query("SELECT * FROM location_records ORDER BY timestamp DESC")
    List<LocationRecord> getAll();

    @Query("SELECT * FROM location_records ORDER BY timestamp DESC LIMIT 1")
    LocationRecord getLatest();

    @Query("SELECT * FROM location_records WHERE sent = 0 ORDER BY timestamp DESC")
    List<LocationRecord> getUnsent();

    @Query("SELECT COUNT(*) FROM location_records")
    int getCount();

    @Query("DELETE FROM location_records WHERE id = :id")
    int deleteById(Long id);
}

