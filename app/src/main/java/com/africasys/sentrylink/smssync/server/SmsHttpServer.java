package com.africasys.sentrylink.smssync.server;

import android.content.Context;
import android.os.Build;
import android.telephony.SmsManager;
import android.util.Log;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * Mini-serveur HTTP embarqué (NanoHTTPD) hébergé directement dans l'application.
 *
 * <p>Point d'entrée unique :
 * <pre>
 *   POST /send-sms
 *   Header : X-Api-Token: &lt;token&gt;
 *   Body   : {"numero": "+33612345678", "message": "Texte du SMS"}
 * </pre>
 *
 * <p>Réponses JSON :
 * <ul>
 *   <li>{@code {"status":"ok","numero":"..."}} — SMS envoyé</li>
 *   <li>{@code {"status":"error","message":"..."}} — erreur avec détail</li>
 * </ul>
 *
 * <p>Codes HTTP :
 * <ul>
 *   <li>200 — succès</li>
 *   <li>400 — paramètres manquants ou JSON invalide</li>
 *   <li>401 — token absent ou incorrect</li>
 *   <li>404 — endpoint inconnu</li>
 *   <li>500 — erreur interne (envoi SMS échoué)</li>
 * </ul>
 */
public class SmsHttpServer extends NanoHTTPD {

    private static final String TAG = "SL-SmsHttpServer";

    private static final String ENDPOINT_SEND_SMS = "/send-sms";
    /** En minuscules : NanoHTTPD normalise les headers en lowercase. */
    private static final String HEADER_TOKEN = "x-api-token";

    private final Context context;
    private final String apiToken;

    public SmsHttpServer(Context context, int port, String apiToken) {
        super(port);
        this.context = context.getApplicationContext();
        this.apiToken = apiToken;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Routeur principal
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public Response serve(IHTTPSession session) {
        String uri    = session.getUri();
        Method method = session.getMethod();

        Log.d(TAG, "▶ " + method + " " + uri
                + " | from: " + session.getRemoteIpAddress());

        // ── Vérification du token (toutes les routes) ─────────────────────
        String incomingToken = session.getHeaders().get(HEADER_TOKEN);
        if (incomingToken == null || !incomingToken.equals(apiToken)) {
            Log.w(TAG, "✗ Token invalide ou manquant (reçu: "
                    + (incomingToken == null ? "null" : "'" + incomingToken + "'") + ")");
            return jsonResponse(Response.Status.UNAUTHORIZED,
                    error("Token invalide ou manquant"));
        }

        // ── Routage ───────────────────────────────────────────────────────
        if (Method.POST.equals(method) && ENDPOINT_SEND_SMS.equals(uri)) {
            return handleSendSms(session);
        }

        return jsonResponse(Response.Status.NOT_FOUND,
                error("Endpoint inconnu. Utilisez POST /send-sms"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Handler POST /send-sms
    // ─────────────────────────────────────────────────────────────────────────

    private Response handleSendSms(IHTTPSession session) {
        // Lecture du body JSON via parseBody() (méthode NanoHTTPD recommandée)
        Map<String, String> files = new HashMap<>();
        try {
            session.parseBody(files);
        } catch (IOException | ResponseException e) {
            Log.e(TAG, "✗ Erreur lecture body", e);
            return jsonResponse(Response.Status.INTERNAL_ERROR,
                    error("Impossible de lire le corps de la requête"));
        }

        String rawBody = files.get("postData");
        if (rawBody == null || rawBody.trim().isEmpty()) {
            return jsonResponse(Response.Status.BAD_REQUEST,
                    error("Corps JSON vide ou manquant"));
        }

        // Parse JSON
        String numero;
        String message;
        try {
            JSONObject json = new JSONObject(rawBody);
            numero  = json.optString("numero",  "").trim();
            message = json.optString("message", "").trim();
        } catch (Exception e) {
            Log.w(TAG, "✗ JSON invalide: " + rawBody);
            return jsonResponse(Response.Status.BAD_REQUEST,
                    error("JSON invalide: " + e.getMessage()));
        }

        if (numero.isEmpty()) {
            return jsonResponse(Response.Status.BAD_REQUEST,
                    error("Champ 'numero' manquant ou vide"));
        }
        if (message.isEmpty()) {
            return jsonResponse(Response.Status.BAD_REQUEST,
                    error("Champ 'message' manquant ou vide"));
        }

        // Envoi SMS
        try {
            sendSms(numero, message);
            Log.i(TAG, "✓ SMS envoyé → " + numero + " | \"" + message + "\"");

            JSONObject ok = new JSONObject();
            ok.put("status", "ok");
            ok.put("numero", numero);
            return jsonResponse(Response.Status.OK, ok.toString());

        } catch (Exception e) {
            Log.e(TAG, "✗ Échec envoi SMS vers " + numero, e);
            return jsonResponse(Response.Status.INTERNAL_ERROR,
                    error("Échec envoi SMS: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Envoi SMS (API 31+ compatible)
    // ─────────────────────────────────────────────────────────────────────────

    @SuppressWarnings("deprecation")
    private void sendSms(String numero, String message) {
        SmsManager smsManager;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            smsManager = context.getSystemService(SmsManager.class);
        } else {
            smsManager = SmsManager.getDefault();
        }

        ArrayList<String> parts = smsManager.divideMessage(message);
        if (parts.size() == 1) {
            smsManager.sendTextMessage(numero, null, message, null, null);
        } else {
            smsManager.sendMultipartTextMessage(numero, null, parts, null, null);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utilitaires
    // ─────────────────────────────────────────────────────────────────────────

    private Response jsonResponse(Response.Status status, String json) {
        Response r = newFixedLengthResponse(status, "application/json", json);
        r.addHeader("Access-Control-Allow-Origin", "*");
        return r;
    }

    private String error(String msg) {
        try {
            JSONObject j = new JSONObject();
            j.put("status", "error");
            j.put("message", msg);
            return j.toString();
        } catch (Exception e) {
            return "{\"status\":\"error\",\"message\":\"internal\"}";
        }
    }
}
