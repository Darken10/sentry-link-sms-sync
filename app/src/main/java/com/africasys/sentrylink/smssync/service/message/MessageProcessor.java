package com.africasys.sentrylink.smssync.service.message;

import android.content.Context;
import android.util.Log;

import com.africasys.sentrylink.smssync.dtos.SMSDecryptedDTO;
import com.africasys.sentrylink.smssync.enums.MessageType;

import java.util.HashMap;
import java.util.Map;

/**
 * Singleton responsable d'enregistrer et d'appeler les handlers pour chaque type
 * de message. Permet d'ajouter facilement de nouveaux types sans modifier le
 * code de déchiffrement / réception.
 */
public class MessageProcessor {

    private static final String TAG = "SL-MessageProcessor";
    private static final MessageProcessor INSTANCE = new MessageProcessor();

    private final Map<MessageType, MessageHandler> handlers = new HashMap<>();
    private final MessageHandler defaultHandler = new com.africasys.sentrylink.smssync.service.message.DefaultMessageHandler();

    private MessageProcessor() {
        // Enregistrer handlers par défaut (utilise noms pleinement qualifiés pour
        // éviter les problèmes de résolution d'ordre de création des fichiers)
        handlers.put(MessageType.MSG, new com.africasys.sentrylink.smssync.service.message.MsgMessageHandler());
        handlers.put(MessageType.SOS, new com.africasys.sentrylink.smssync.service.message.SosMessageHandler());
        handlers.put(MessageType.LOC, new com.africasys.sentrylink.smssync.service.message.LocMessageHandler());
    }

    public static MessageProcessor getInstance() {
        return INSTANCE;
    }

    public void registerHandler(MessageType type, MessageHandler handler) {
        if (type == null || handler == null) return;
        handlers.put(type, handler);
        Log.i(TAG, "Handler enregistré pour type: " + type);
    }

    public void unregisterHandler(MessageType type) {
        handlers.remove(type);
        Log.i(TAG, "Handler supprimé pour type: " + type);
    }

    /**
     * Délègue le traitement au handler approprié. Non-bloquant : lance sur un
     * thread de fond pour éviter de bloquer le BroadcastReceiver.
     */
    public void process(Context context, String sender, SMSDecryptedDTO dto, long receivedTimestamp, String prefix) {
        MessageHandler handler = (dto != null && dto.getType() != null) ? handlers.getOrDefault(dto.getType(), defaultHandler) : defaultHandler;
        new Thread(() -> {
            try {
                handler.handle(context, sender, dto, receivedTimestamp, prefix);
            } catch (Exception e) {
                Log.e(TAG, "Erreur dans handler pour type=" + (dto != null ? dto.getType() : "null"), e);
            }
        }, "SL-MessageProcessor").start();
    }

}

