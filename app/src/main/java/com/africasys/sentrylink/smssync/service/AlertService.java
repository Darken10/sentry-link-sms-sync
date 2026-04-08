package com.africasys.sentrylink.smssync.service;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.africasys.sentrylink.smssync.dtos.ContactDto;
import com.africasys.sentrylink.smssync.network.ApiClient;
import com.africasys.sentrylink.smssync.repository.ConfigRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Response;

public class AlertService {

    private static final String TAG = "SL-AlertService";
    private static volatile AlertService INSTANCE;

    private final Context appContext;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());


    private AlertService(Context context) {
        appContext = context.getApplicationContext();
    }

    public static AlertService getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AlertService.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AlertService(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    public interface Callback<T> {
        void onSuccess(T result);

        void onError(String message, Throwable cause);
    }


    public void sentToControlServer(AlertService.Callback<Integer> callback) {
        executor.execute(() -> {
            try {
                String credential = ConfigRepository.getInstance(appContext).getAuthToken();
                if (credential == null || credential.isEmpty()) {
                    dispatchError(callback, "Non authentifié — aucun token disponible", null);
                    return;
                }

                Response<List<ContactDto>> response = ApiClient.getInstance(appContext).getApi().getContacts(credential)
                        .execute();

                if (response.isSuccessful() && response.body() != null) {
                    List<ContactDto> dtos = response.body();

                    Log.d(TAG, "Contacts synchronisés : " + dtos.size());
                    dispatchSuccess(callback, dtos.size());
                } else {
                    String msg = "Sync contacts — HTTP " + response.code();
                    Log.w(TAG, msg);
                    dispatchError(callback, msg, null);
                }
            } catch (Exception e) {
                Log.e(TAG, "Sync contacts échouée", e);
                dispatchError(callback, "Erreur réseau lors de la synchronisation des contacts", e);
            }
        });
    }


    private <T> void dispatchSuccess(AlertService.Callback<T> callback, T value) {
        if (callback == null)
            return;
        mainHandler.post(() -> callback.onSuccess(value));
    }

    private <T> void dispatchError(AlertService.Callback<T> callback, String message, Throwable cause) {
        if (callback == null)
            return;
        mainHandler.post(() -> callback.onError(message, cause));
    }



}
