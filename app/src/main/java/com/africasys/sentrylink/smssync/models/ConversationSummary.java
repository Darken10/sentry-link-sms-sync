package com.africasys.sentrylink.smssync.models;

import androidx.room.ColumnInfo;

/**
 * POJO (non-entité Room) utilisé par
 * {@link com.africasys.sentrylink.repository.SmsDao}
 * pour retourner un résumé de conversation par numéro de téléphone.
 *
 * <p>
 * Seuls les SMS portant le préfixe {@code [SL]} (messages SentryLink) sont pris
 * en compte.
 */
public class ConversationSummary {

    /** Numéro de téléphone de l'interlocuteur. */
    @ColumnInfo(name = "phone_number")
    public String phoneNumber;

    /** Corps du dernier message [SL] (chiffré ou déchiffré selon le stockage). */
    @ColumnInfo(name = "lastMessageBody")
    public String lastMessageBody;

    /** Type du dernier message : 1 = reçu, 2 = envoyé. */
    @ColumnInfo(name = "lastMessageType")
    public int lastMessageType;

    /** Horodatage (ms) du dernier message [SL]. */
    @ColumnInfo(name = "lastTimestamp")
    public long lastTimestamp;

    /** Nombre total de messages [SL] échangés avec ce numéro. */
    @ColumnInfo(name = "messageCount")
    public int messageCount;
}
