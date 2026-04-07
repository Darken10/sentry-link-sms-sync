package com.africasys.sentrylink.network;

import com.africasys.sentrylink.config.ApiConfig;
import com.africasys.sentrylink.dtos.ContactAuthResponse;
import com.africasys.sentrylink.dtos.ContactDto;
import com.africasys.sentrylink.dtos.DefaultKeyResponse;
import com.africasys.sentrylink.dtos.PrivateKeyResponse;

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
     * Envoie un message chiffré.
     */
    @POST("/api/v1/messages/send")
    Call<Map<String, Object>> sendMessage(
            @Header("X-SentryLink-Signature") String signature,
            @Header("X-SentryLink-Device") String deviceCallsign,
            @Body Map<String, String> encryptedPayload
    );

    /**
     * Rapporte une position.
     */
    @POST("/api/v1/location/report")
    Call<Map<String, Object>> reportLocation(
            @Header("X-SentryLink-Signature") String signature,
            @Header("X-SentryLink-Device") String deviceCallsign,
            @Body Map<String, String> encryptedPayload
    );

    /**
     * Envoie une alerte SOS.
     */
    @POST("/api/v1/sos/alert")
    Call<Map<String, Object>> sendSosAlert(
            @Header("X-SentryLink-Signature") String signature,
            @Header("X-SentryLink-Device") String deviceCallsign,
            @Body Map<String, String> encryptedPayload
    );

    /**
     * Récupère les messages en attente pour cet appareil.
     */
    @POST("/api/v1/messages/poll")
    Call<Map<String, Object>> pollMessages(
            @Header("X-SentryLink-Signature") String signature,
            @Header("X-SentryLink-Device") String deviceCallsign,
            @Body Map<String, String> request
    );

    /**
     * Retourne la liste de tous les contacts avec leur clé publique active.
     *
     * @param authToken credential SHA-512 (header X-Contact-Auth)
     */
    @GET("/api/v1/contacts")
    Call<List<ContactDto>> getContacts(
            @Header(ApiConfig.CONTACT_AUTH_HEADER_KEY) String authToken
    );

    /**
     * Retourne la clé privée RSA-4096 du contact appelant, chiffrée en AES-256-GCM.
     * Réponse 204 si aucune clé active n'existe pour ce contact.
     *
     * @param authToken credential SHA-512 (header X-Contact-Auth)
     */
    @GET("/api/v1/my-private-key")
    Call<PrivateKeyResponse> getMyPrivateKey(
            @Header(ApiConfig.CONTACT_AUTH_HEADER_KEY) String authToken
    );

    /**
     * Retourne la clé publique RSA-4096 par défaut.
     * Utilisée pour chiffrer les messages destinés à un numéro hors répertoire.
     *
     * @param authToken credential SHA-512 (header X-Contact-Auth)
     */
    @GET("/api/v1/default-public-key")
    Call<DefaultKeyResponse> getDefaultPublicKey(
            @Header(ApiConfig.CONTACT_AUTH_HEADER_KEY) String authToken
    );
}

