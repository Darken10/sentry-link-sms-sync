package com.africasys.sentrylink.smssync.dtos;

import com.google.gson.annotations.SerializedName;

public class AlertResponseDTO {

    @SerializedName("id")
    public int id;

    @SerializedName("contactId")
    public Integer contactId;

    @SerializedName("level")
    public String level;

    @SerializedName("message")
    public String message;

    @SerializedName("lon")
    public Double lon;

    @SerializedName("lat")
    public Double lat;

    @SerializedName("status")
    public String status; // Ex: "OPEN"

    @SerializedName("sentAt")
    public String sentAt;

    @SerializedName("createdAt")
    public String createdAt;

    @SerializedName("updatedAt")
    public String updatedAt;

    @SerializedName("resolvedAt")
    public String resolvedAt;

    @SerializedName("uuid")
    public String uuid;
}
