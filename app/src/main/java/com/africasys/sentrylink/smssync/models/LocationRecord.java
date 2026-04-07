package com.africasys.sentrylink.smssync.models;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Entité pour stocker les données de localisation.
 */
@Entity(tableName = "location_records")
public class LocationRecord {

    @PrimaryKey(autoGenerate = true)
    private Long id;

    @ColumnInfo(name = "latitude")
    private double latitude;

    @ColumnInfo(name = "longitude")
    private double longitude;

    @ColumnInfo(name = "accuracy")
    private float accuracy;

    @ColumnInfo(name = "source")
    private String source; // GPS, GSM, NETWORK

    @ColumnInfo(name = "timestamp")
    private long timestamp;

    @ColumnInfo(name = "cell_info")
    private String cellInfo; // JSON des infos pylônes si source = GSM

    @ColumnInfo(name = "sent")
    private boolean sent; // Envoyé au centre de contrôle ?

    public LocationRecord() {}

    @Ignore
    public LocationRecord(double latitude, double longitude, float accuracy, String source, long timestamp) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
        this.source = source;
        this.timestamp = timestamp;
        this.sent = false;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public float getAccuracy() { return accuracy; }
    public void setAccuracy(float accuracy) { this.accuracy = accuracy; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getCellInfo() { return cellInfo; }
    public void setCellInfo(String cellInfo) { this.cellInfo = cellInfo; }

    public boolean isSent() { return sent; }
    public void setSent(boolean sent) { this.sent = sent; }
}

