package com.africasys.sentrylink.models;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Entité pour stocker les alertes SOS.
 */
@Entity(tableName = "sos_alerts")
public class SosAlert {

    @PrimaryKey(autoGenerate = true)
    private Long id;

    @ColumnInfo(name = "latitude")
    private double latitude;

    @ColumnInfo(name = "longitude")
    private double longitude;

    @ColumnInfo(name = "cell_info")
    private String cellInfo;

    @ColumnInfo(name = "location_source")
    private String locationSource; // GPS, GSM

    @ColumnInfo(name = "timestamp")
    private long timestamp;

    @ColumnInfo(name = "status")
    private String status; // PENDING, SENT, DELIVERED, FAILED

    @ColumnInfo(name = "sent_via")
    private String sentVia; // SMS, API

    @ColumnInfo(name = "message")
    private String message;

    public SosAlert() {}

    @Ignore
    public SosAlert(double latitude, double longitude, String locationSource, long timestamp) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.locationSource = locationSource;
        this.timestamp = timestamp;
        this.status = "PENDING";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getCellInfo() { return cellInfo; }
    public void setCellInfo(String cellInfo) { this.cellInfo = cellInfo; }

    public String getLocationSource() { return locationSource; }
    public void setLocationSource(String locationSource) { this.locationSource = locationSource; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSentVia() { return sentVia; }
    public void setSentVia(String sentVia) { this.sentVia = sentVia; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    @NonNull
    @Override
    public String toString() {
        return "SosAlert{" +
                "id=" + id +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", cellInfo='" + cellInfo + '\'' +
                ", locationSource='" + locationSource + '\'' +
                ", timestamp=" + timestamp +
                ", status='" + status + '\'' +
                ", sentVia='" + sentVia + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}

