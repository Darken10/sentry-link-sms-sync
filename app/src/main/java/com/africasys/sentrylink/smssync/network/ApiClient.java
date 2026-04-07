package com.africasys.sentrylink.smssync.network;

import android.content.Context;
import android.util.Log;

import com.africasys.sentrylink.smssync.config.ApiConfig;
import com.africasys.sentrylink.smssync.repository.ConfigRepository;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

/**
 * Client API Retrofit singleton. Configuré avec TLS, timeouts stricts, et
 * logging interceptor.
 */
public class ApiClient {

    private static final String TAG = "SL-ApiClient";
    private static volatile ApiClient INSTANCE;

    private Retrofit retrofit;
    private SentryLinkApi api;

    private ApiClient(Context context) {
        buildClient(context);
    }

    public static ApiClient getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (ApiClient.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ApiClient(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    private void buildClient(Context context) {
        ConfigRepository config = ConfigRepository.getInstance(context);
        String baseUrl = config.getApiBaseUrl();
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder().connectTimeout(ApiConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(ApiConfig.READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(ApiConfig.WRITE_TIMEOUT, TimeUnit.SECONDS).addInterceptor(logging)
                .addInterceptor(chain -> {
                    String credential = config.getAuthToken();
                    Request original = chain.request();
                    Request.Builder requestBuilder = original.newBuilder();
                    if (credential != null && !credential.isEmpty()) {
                        requestBuilder.header(ApiConfig.CONTACT_AUTH_HEADER_KEY, credential);
                    }
                    Request request = requestBuilder.build();
                    return chain.proceed(request);
                }).build();

        retrofit = new Retrofit.Builder().baseUrl(baseUrl).client(client)
                .addConverterFactory(GsonConverterFactory.create()).build();

        api = retrofit.create(SentryLinkApi.class);
    }

    public SentryLinkApi getApi() {
        return api;
    }

    /**
     * Reconstruit le client (ex: après changement d'URL dans les paramètres).
     */
    public void rebuild(Context context) {
        buildClient(context);
    }
}
