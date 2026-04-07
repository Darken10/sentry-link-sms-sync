package com.africasys.sentrylink.service;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.CellIdentity;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityNr;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoNr;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthNr;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.util.List;

public class LocationService {

    private static final String TAG = "SL-LocationService";

    public static void scanCellTowers(Context context) {

        // Vérification de la permission
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            Log.d(TAG, "Permission ACCESS_FINE_LOCATION non accordée");
            return;
        }

        try {

            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            List<CellInfo> cellInfoList = telephonyManager.getAllCellInfo();

            if (cellInfoList != null) {

                for (CellInfo info : cellInfoList) {

                    if (info instanceof CellInfoLte) {

                        CellInfoLte lteInfo = (CellInfoLte) info;

                        CellIdentityLte identity = lteInfo.getCellIdentity();
                        CellSignalStrengthLte signal = lteInfo.getCellSignalStrength();

                        int cid = identity.getCi();
                        String mcc = identity.getMccString();
                        String mnc = identity.getMncString();
                        int tac = identity.getTac();
                        int rssi = signal.getDbm();

                        Log.d(TAG, "LTE CELL DETECTED");
                        Log.d(TAG, "CID: " + cid);
                        Log.d(TAG, "MCC: " + mcc);
                        Log.d(TAG, "MNC: " + mnc);
                        Log.d(TAG, "TAC: " + tac);
                        Log.d(TAG, "RSSI: " + rssi + " dBm");
                    }
                }

            } else {
                Log.d(TAG, "Aucune cellule détectée");
            }

        } catch (SecurityException e) {
            Log.e(TAG, "Permission refusée pour accéder aux cellules", e);
        }
    }

    @NonNull
    public static String scanCellTowersToString(Context context) {
        // Vérification de la permission
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            Log.d(TAG, "Permission ACCESS_FINE_LOCATION non accordée");
            return "";
        }

        StringBuilder text = new StringBuilder();
        int cellCount = 0;

        try {

            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            List<CellInfo> cellInfoList = telephonyManager.getAllCellInfo();

            if (cellInfoList != null && !cellInfoList.isEmpty()) {
                text.append("=== PYLÔNES DÉTECTÉS ===\n");
                text.append("Nombre total: ").append(cellInfoList.size()).append("\n\n");

                for (CellInfo info : cellInfoList) {

                    if (info instanceof CellInfoLte) {
                        text.append(parseLteCell((CellInfoLte) info));
                        cellCount++;
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && info instanceof CellInfoNr) {
                        text.append(parse5GCell((CellInfoNr) info));
                        cellCount++;
                    } else if (info instanceof CellInfoWcdma) {
                        text.append(parse3GCell((CellInfoWcdma) info));
                        cellCount++;
                    } else if (info instanceof CellInfoGsm) {
                        text.append(parse2GCell((CellInfoGsm) info));
                        cellCount++;
                    }
                }

                if (cellCount == 0) {
                    text.append("Aucun pylône supporté détecté");
                }
                return text.toString();

            } else {
                Log.d(TAG, "Aucune cellule détectée");
                return "Aucune cellule détectée";
            }

        } catch (SecurityException e) {
            Log.e(TAG, "Permission refusée pour accéder aux cellules", e);
            return "Erreur: Permission refusée";
        }
    }

    @NonNull
    private static String parseLteCell(CellInfoLte lteInfo) {
        StringBuilder sb = new StringBuilder();
        try {
            CellIdentityLte identity = lteInfo.getCellIdentity();
            CellSignalStrengthLte signal = lteInfo.getCellSignalStrength();

            int cid = identity.getCi();
            String mcc = identity.getMccString();
            String mnc = identity.getMncString();
            int tac = identity.getTac();
            int rssi = signal.getDbm();

            sb.append("📡 LTE (4G)\n");
            sb.append("  CID: ").append(cid).append("\n");
            sb.append("  MCC: ").append(mcc != null ? mcc : "N/A").append("\n");
            sb.append("  MNC: ").append(mnc != null ? mnc : "N/A").append("\n");
            sb.append("  TAC: ").append(tac).append("\n");
            sb.append("  Signal: ").append(rssi).append(" dBm\n");
            sb.append("  Registré: ").append(lteInfo.isRegistered()).append("\n");
            sb.append("---\n");
        } catch (Exception e) {
            Log.e(TAG, "Erreur parsing LTE", e);
        }
        return sb.toString();
    }

    @NonNull
    private static String parse5GCell(CellInfoNr nrInfo) {
        StringBuilder sb = new StringBuilder();
        try {
            // Correction : Utiliser CellIdentityNr
            CellIdentityNr identity = (CellIdentityNr) nrInfo.getCellIdentity();
            // Correction : Cast explicite pour le signal
            CellSignalStrengthNr signal = (CellSignalStrengthNr) nrInfo.getCellSignalStrength();

            String mcc = identity.getMccString();
            String mnc = identity.getMncString();
            int rssi = signal.getDbm();

            sb.append("📡 5G (NR)\n");
            sb.append("  MCC: ").append(mcc != null ? mcc : "N/A").append("\n");
            sb.append("  MNC: ").append(mnc != null ? mnc : "N/A").append("\n");
            sb.append("  Signal: ").append(rssi).append(" dBm\n");
            sb.append("  Registré: ").append(nrInfo.isRegistered()).append("\n");
            sb.append("---\n");
        } catch (Exception e) {
            Log.e(TAG, "Erreur parsing 5G", e);
        }
        return sb.toString();
    }

    @NonNull
    private static String parse3GCell(CellInfoWcdma wcdmaInfo) {
        StringBuilder sb = new StringBuilder();
        try {
            // Correction : Utiliser CellIdentityWcdma au lieu de CellIdentityGsm
            CellIdentityWcdma identity = wcdmaInfo.getCellIdentity();

            int cid = identity.getCid();
            String mcc = identity.getMccString();
            String mnc = identity.getMncString();
            int lac = identity.getLac();

            sb.append("📡 3G (WCDMA)\n");
            sb.append("  CID: ").append(cid).append("\n");
            sb.append("  MCC: ").append(mcc != null ? mcc : "N/A").append("\n");
            sb.append("  MNC: ").append(mnc != null ? mnc : "N/A").append("\n");
            sb.append("  LAC: ").append(lac).append("\n");
            sb.append("  Registré: ").append(wcdmaInfo.isRegistered()).append("\n");
            sb.append("---\n");
        } catch (Exception e) {
            Log.e(TAG, "Erreur parsing 3G", e);
        }
        return sb.toString();
    }

    @NonNull
    private static String parse2GCell(CellInfoGsm gsmInfo) {
        StringBuilder sb = new StringBuilder();
        try {
            CellIdentityGsm identity = gsmInfo.getCellIdentity();

            int cid = identity.getCid();
            String mcc = identity.getMccString();
            String mnc = identity.getMncString();
            int lac = identity.getLac();

            sb.append("📡 2G (GSM)\n");
            sb.append("  CID: ").append(cid).append("\n");
            sb.append("  MCC: ").append(mcc != null ? mcc : "N/A").append("\n");
            sb.append("  MNC: ").append(mnc != null ? mnc : "N/A").append("\n");
            sb.append("  LAC: ").append(lac).append("\n");
            sb.append("  Registré: ").append(gsmInfo.isRegistered()).append("\n");
            sb.append("---\n");
        } catch (Exception e) {
            Log.e(TAG, "Erreur parsing 2G", e);
        }
        return sb.toString();
    }
}