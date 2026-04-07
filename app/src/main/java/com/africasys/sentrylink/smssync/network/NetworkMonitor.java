package com.africasys.sentrylink.smssync.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.telephony.TelephonyManager;

import androidx.annotation.NonNull;

/**
 * Moniteur réseau en temps réel.
 * Détecte la disponibilité d'internet et de la SIM.
 */
public class NetworkMonitor {

    private final Context context;
    private final ConnectivityManager connectivityManager;
    private boolean isConnected = false;
    private NetworkCallback callback;

    public interface NetworkCallback {
        void onNetworkAvailable();
        void onNetworkLost();
    }

    public NetworkMonitor(Context context) {
        this.context = context.getApplicationContext();
        this.connectivityManager = (ConnectivityManager) this.context.getSystemService(Context.CONNECTIVITY_SERVICE);
        updateConnectionStatus();
    }

    /**
     * Vérifie si Internet est disponible.
     */
    public boolean isInternetAvailable() {
        updateConnectionStatus();
        return isConnected;
    }

    /**
     * Vérifie si une SIM est insérée.
     */
    public boolean isSimAvailable() {
        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm == null) return false;
            int simState = tm.getSimState();
            return simState == TelephonyManager.SIM_STATE_READY;
        } catch (Exception e) {
            return false;
        }
    }

    private void updateConnectionStatus() {
        if (connectivityManager == null) {
            isConnected = false;
            return;
        }

        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) {
            isConnected = false;
            return;
        }

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        isConnected = capabilities != null &&
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                 capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                 capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }

    /**
     * Démarre la surveillance du réseau.
     */
    public void startMonitoring(NetworkCallback callback) {
        this.callback = callback;

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        connectivityManager.registerNetworkCallback(request, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                isConnected = true;
                if (NetworkMonitor.this.callback != null) {
                    NetworkMonitor.this.callback.onNetworkAvailable();
                }
            }

            @Override
            public void onLost(@NonNull Network network) {
                isConnected = false;
                if (NetworkMonitor.this.callback != null) {
                    NetworkMonitor.this.callback.onNetworkLost();
                }
            }
        });
    }
}

