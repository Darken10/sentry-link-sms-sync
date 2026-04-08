package com.africasys.sentrylink.smssync.service.message;

import android.content.Context;
import android.util.Log;

import com.africasys.sentrylink.smssync.dtos.SMSDecryptedDTO;
import com.africasys.sentrylink.smssync.models.LocationRecord;
import com.africasys.sentrylink.smssync.repository.AppDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Handler pour les messages de localisation (LOC).
 * Parse le body JSON attendu et persiste la localisation en base.
 */
public class LocMessageHandler implements MessageHandler {
    private static final String TAG = "SL-LocHandler";

    @Override
    public void handle(Context context, String sender, SMSDecryptedDTO dto, long receivedTimestamp, String prefix) {
        try {
            String body = dto.getMessage();
            double lat = 0.0;
            double lon = 0.0;
            float acc = 0f;
            String src = "UNKNOWN";
            String cellInfo = null;

            try {
                JSONObject j = new JSONObject(body);
                src = j.optString("src", "UNKNOWN");
                if (j.has("lat")) lat = j.optDouble("lat", 0.0);
                if (j.has("lon")) lon = j.optDouble("lon", 0.0);
                if (j.has("acc")) acc = (float) j.optDouble("acc", 0.0);
                if (j.has("cell_towers")) {
                    JSONArray arr = j.optJSONArray("cell_towers");
                    if (arr != null) cellInfo = arr.toString();
                }
            } catch (Exception e) {
                Log.w(TAG, "LOC body non-JSON — sauvegarde brut");
            }

            LocationRecord record = new LocationRecord(lat, lon, acc, src, dto.getTimestamp() > 0 ? dto.getTimestamp() : System.currentTimeMillis());
            if (cellInfo != null) record.setCellInfo(cellInfo);

            AppDatabase.getInstance(context).locationDao().insert(record);
            Log.i(TAG, "Localisation persistée (src=" + src + ")");

        } catch (Exception e) {
            Log.e(TAG, "Erreur traitement LOC", e);
        }
    }
}
