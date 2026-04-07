package com.africasys.sentrylink.smssync.models;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "sms_messages")
public class SMSMessage {
    @PrimaryKey(autoGenerate = true)
    private Long id;

    @ColumnInfo(name = "phone_number")
    private String address;

    @ColumnInfo(name = "message_body")
    private String body;

    @ColumnInfo(name = "timestamp")
    private long timestamp;

    @ColumnInfo(name = "message_type")
    private int type; // 1 for inbox, 2 for sent

    public SMSMessage() {}

    @Ignore
    public SMSMessage(String address, String body, long timestamp, int type) {
        this.address = address;
        this.body = body;
        this.timestamp = timestamp;
        this.type = type;
    }

    @Ignore
    public SMSMessage(Long id, String address, String body, long timestamp, int type) {
        this.id = id;
        this.address = address;
        this.body = body;
        this.timestamp = timestamp;
        this.type = type;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public int getType() { return type; }
    public void setType(int type) { this.type = type; }

    @NonNull
    @Override
    public String toString() {
        return "SMSMessage{" +
                "id=" + id +
                ", address='" + address + '\'' +
                ", body='" + body + '\'' +
                ", timestamp=" + timestamp +
                ", type=" + type +
                '}';
    }
}
