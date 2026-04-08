package com.africasys.sentrylink.smssync.network;

import com.africasys.sentrylink.smssync.config.ApiConfig;
import com.africasys.sentrylink.smssync.dtos.AlertRequestDTO;
import com.africasys.sentrylink.smssync.dtos.AlertResponseDTO;
import com.africasys.sentrylink.smssync.dtos.ContactAuthResponse;
import com.africasys.sentrylink.smssync.dtos.ContactDto;
import com.africasys.sentrylink.smssync.dtos.DefaultKeyResponse;
import com.africasys.sentrylink.smssync.dtos.PrivateKeyResponse;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Interface Retrofit pour l'API SentryLink.
 * Tous les payloads sont chiffrés côté client avant envoi.
 */
public interface SentryLinkApi {


    /**
     * Authentification par cles public
     * Le token est ajouté automatiquement via l'interceptor ApiClient.
     */
    @POST("/api/v1/public/contact-auth")
    Call<ContactAuthResponse> contactAuth(
            @Header(ApiConfig.CONTACT_AUTH_HEADER_KEY) String authToken
    );

    /**
     * Retourne la liste de tous les contacts avec leur clé publique active.
     *
     * @param authToken credential
     */
    @GET("/api/v1/contacts")
    Call<List<ContactDto>> getContacts(
            @Header(ApiConfig.CONTACT_AUTH_HEADER_KEY) String authToken
    );

    @POST("/api/v1/alert")
    Call<AlertResponseDTO> sendAlert(
            @Header(ApiConfig.CONTACT_AUTH_HEADER_KEY) String authToken,
            @Body AlertRequestDTO body
    );

}

