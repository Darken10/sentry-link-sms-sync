package com.africasys.sentrylink.dtos;

import com.google.gson.annotations.SerializedName;

public class ContactAuthResponse {

    @SerializedName("contactId")
    public int contactId;

    @SerializedName("name")
    public String name;

    @SerializedName("identifier")
    public String identifier;

    @SerializedName("status")
    public String status;

    @SerializedName("credential")
    public String credential;
}
