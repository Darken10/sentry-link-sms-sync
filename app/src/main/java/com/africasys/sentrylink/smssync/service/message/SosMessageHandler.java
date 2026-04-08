package com.africasys.sentrylink.smssync.service.message;

import android.content.Context;
import android.util.Log;

import com.africasys.sentrylink.smssync.dtos.SMSDecryptedDTO;
import com.africasys.sentrylink.smssync.dtos.AlertRequestDTO;
import com.africasys.sentrylink.smssync.dtos.AlertResponseDTO;
import com.africasys.sentrylink.smssync.models.SosAlert;
import com.africasys.sentrylink.smssync.repository.AppDatabase;
import com.africasys.sentrylink.smssync.repository.SosDao;
import com.africasys.sentrylink.smssync.network.ApiClient;
import com.africasys.sentrylink.smssync.repository.ConfigRepository;

import org.json.JSONObject;

import retrofit2.Response;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Handler pour les messages SOS : persist et tente d'envoyer au serveur HTTP.
 */
public class SosMessageHandler implements MessageHandler {
    private static final String TAG = "SL-SosHandler";

    @Override
    public void handle(Context context, String sender, SMSDecryptedDTO dto, long receivedTimestamp, String prefix) {
        try {
            String body = dto.getMessage();

            double lat = 0.0;
            double lon = 0.0;
            String src = "UNKNOWN";
            String messageText = body;

            try {
                JSONObject j = new JSONObject(body);
                if (j.has("lat")) lat = j.optDouble("lat", 0.0);
                if (j.has("lon")) lon = j.optDouble("lon", 0.0);
                if (j.has("src")) src = j.optString("src", "UNKNOWN");
                if (j.has("msg")) messageText = j.optString("msg", body);
            } catch (Exception e) {
                Log.w(TAG, "Corps SOS non-JSON — utilisation du texte brut");
            }

            SosDao dao = AppDatabase.getInstance(context).sosDao();
            SosAlert alert = new SosAlert(lat, lon, src, dto.getTimestamp() > 0 ? dto.getTimestamp() : System.currentTimeMillis());
            alert.setMessage(messageText);
            long id = dao.insert(alert);
            alert.setId(id);
            Log.i(TAG, "SosAlert persistée en DB (id=" + id + ")");

            String credential = ConfigRepository.getInstance(context).getAuthToken();
            if (credential != null && !credential.isEmpty()) {
                try {
                    String sentAt;
                    long ts = dto.getTimestamp() > 0 ? dto.getTimestamp() : System.currentTimeMillis();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                    sentAt = sdf.format(new Date(ts));

                    AlertRequestDTO req = new AlertRequestDTO(sender, "CRITICAL", messageText, lon, lat, sentAt);
                    Response<AlertResponseDTO> response = ApiClient.getInstance(context).getApi().sendAlert(credential, req).execute();
                    if (response.isSuccessful()) {
                        alert.setStatus("SENT");
                        alert.setSentVia("API");
                        dao.update(alert);
                        Log.i(TAG, "SosAlert envoyée au serveur (id=" + id + ")");
                    } else {
                        Log.w(TAG, "Envoi SOS HTTP échoué — HTTP " + response.code());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Erreur envoi SOS vers API", e);
                }
            } else {
                Log.d(TAG, "Aucun token HTTP — SOS enregistré localement (id=" + id + ")");
            }

        } catch (Exception e) {
            Log.e(TAG, "Erreur traitement SOS", e);
        }
    }
}
