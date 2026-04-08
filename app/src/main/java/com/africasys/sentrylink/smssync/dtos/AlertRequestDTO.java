package com.africasys.sentrylink.smssync.dtos;

import com.google.gson.annotations.SerializedName;

public class AlertRequestDTO {

    @SerializedName("phoneNumber")
    public String phoneNumber;

    @SerializedName("level")
    public String level; //"HIGH", "MEDIUM", "LOW" ,"CRITICAL"

    @SerializedName("message")
    public String message;

    @SerializedName("lon")
    public double lon;

    @SerializedName("lat")
    public double lat;

    @SerializedName("sentAt")
    public String sentAt;

    public AlertRequestDTO(String phoneNumber, String level, String message, double lon, double lat, String sentAt) {
        this.phoneNumber = phoneNumber;
        this.level = level;
        this.message = message;
        this.lon = lon;
        this.lat = lat;
        this.sentAt = sentAt;
    }
}