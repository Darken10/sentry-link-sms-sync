package com.africasys.sentrylink.service;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.africasys.sentrylink.dtos.PrivateKeyResponse;
import com.africasys.sentrylink.models.EncryptionKey;
import com.africasys.sentrylink.network.ApiClient;
import com.africasys.sentrylink.repository.ConfigRepository;
import com.africasys.sentrylink.repository.ContactRepository;
import com.africasys.sentrylink.repository.EncryptionKeyDao;
import com.africasys.sentrylink.repository.AppDatabase;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Response;

/**
 * Service métier pour la gestion des clés de chiffrement.
 *
 * <p>
 * Responsabilités :
 * <ul>
 * <li>Récupération de la clé privée RSA-4096 chiffrée depuis l'API (GET
 * /api/v1/my-private-key), déchiffrement AES-256-GCM et stockage sécurisé dans
 * {@link ConfigRepository} (EncryptedSharedPreferences).</li>
 * <li>Lecture de la clé publique active d'un contact donné.</li>
 * <li>Invalidation et désactivation des clés expirées.</li>
 * </ul>
 *
 * <p>
 * Toutes les opérations réseau s'exécutent sur un thread d'arrière-plan. Les
 * callbacks sont dispatché sur le thread principal.
 *
 * <p>
 * Schéma de déchiffrement de la clé privée :
 * 
 * <pre>
 *   encryptedPrivateKey = base64url(iv):base64url(ciphertext+tag)
 *   aesKey              = SHA-256(credential)   // où credential = SHA-512(rawToken)
 * </pre>
 */
public class EncryptionKeyService {

    private static final String TAG = "SL-EncryptionKeyService";

    private static volatile EncryptionKeyService INSTANCE;

    private final Context appContext;
    private final ConfigRepository configRepository;
    private final EncryptionKeyDao keyDao;
    private final ContactRepository contactRepository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // -------------------------------------------------------------------------
    // Interfaces de callback
    // -------------------------------------------------------------------------

    /** Callback générique pour les opérations asynchrones. */
    public interface Callback<T> {
        void onSuccess(T result);

        void onError(String message, Throwable cause);
    }

    // -------------------------------------------------------------------------
    // Singleton
    // -------------------------------------------------------------------------

    private EncryptionKeyService(Context context) {
        appContext = context.getApplicationContext();
        configRepository = ConfigRepository.getInstance(appContext);
        AppDatabase db = AppDatabase.getInstance(appContext);
        keyDao = db.encryptionKeyDao();
        contactRepository = ContactRepository.getInstance(appContext);
    }

    public static EncryptionKeyService getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (EncryptionKeyService.class) {
                if (INSTANCE == null) {
                    INSTANCE = new EncryptionKeyService(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    // -------------------------------------------------------------------------
    // Synchronisation de la clé privée depuis l'API
    // -------------------------------------------------------------------------

    /**
     * Récupère la clé privée RSA-4096 depuis l'API, la déchiffre et la stocke dans
     * {@link ConfigRepository} (EncryptedSharedPreferences).
     *
     * <p>
     * Étapes :
     * <ol>
     * <li>GET /api/v1/my-private-key avec le credential SHA-512.</li>
     * <li>Déchiffrement AES-256-GCM : clé = SHA-256(credential).</li>
     * <li>Stockage du PEM déchiffré dans EncryptedSharedPreferences.</li>
     * </ol>
     *
     * <p>
     * Réponse 204 = aucune clé active côté serveur (non-erreur, ignorée).
     *
     * @param callback {@code true} si la clé a été récupérée et stockée,
     *                 {@code false} si le serveur retourne 204
     */
    /*
     * public void syncPrivateKey(Callback<Boolean> callback) { executor.execute(()
     * -> { try { String credential = configRepository.getAuthToken(); if
     * (credential == null || credential.isEmpty()) { dispatchError(callback,
     * "Non authentifié — aucun token disponible", null); return; }
     * 
     * Response<PrivateKeyResponse> response =
     * ApiClient.getInstance(appContext).getApi()
     * .getMyPrivateKey(credential).execute();
     * 
     * if (response.code() == 204) { Log.d(TAG,
     * "Aucune clé privée active sur le serveur (204)"); dispatchSuccess(callback,
     * false); return; }
     * 
     * if (response.isSuccessful() && response.body() != null) { String encryptedKey
     * = response.body().encryptedPrivateKey; if (encryptedKey == null ||
     * encryptedKey.isEmpty()) { dispatchError(callback,
     * "Réponse serveur vide : encryptedPrivateKey manquant", null); return; }
     * 
     * // Déchiffrement AES-256-GCM : clé = SHA-256(credential SHA-512) String
     * privateKeyPem = AesGcmHelper.decrypt(encryptedKey, credential);
     * configRepository.setPrivateKey(privateKeyPem); Log.d(TAG,
     * "Clé privée déchiffrée et stockée avec succès"); dispatchSuccess(callback,
     * true); } else { String msg = "Sync clé privée — HTTP " + response.code();
     * Log.w(TAG, msg); dispatchError(callback, msg, null); } } catch (Exception e)
     * { Log.e(TAG, "Sync clé privée échouée", e); dispatchError(callback,
     * "Erreur lors de la synchronisation de la clé privée", e); } }); }
     */

    // -------------------------------------------------------------------------
    // Lecture locale
    // -------------------------------------------------------------------------

    /**
     * Retourne la clé publique PEM active d'un contact (identifié par son UUID).
     *
     * @param contactUuid UUID du contact cible
     * @return la clé publique PEM active, ou null si introuvable
     */
    public String getActivePublicKey(String contactUuid) {
        return contactRepository.getActivePublicKey(contactUuid);
    }

    /**
     * Retourne la clé de chiffrement active d'un contact (identifié par son id
     * serveur).
     *
     * @param contactId identifiant serveur du contact
     * @return l'entité {@link EncryptionKey} active, ou null si introuvable
     */
    public EncryptionKey getActiveKeyForContact(long contactId) {
        return keyDao.getActiveKeyForContact(contactId);
    }

    /**
     * Retourne toutes les clés de chiffrement associées à un contact.
     *
     * @param contactId identifiant serveur du contact
     * @return liste des clés (peut être vide)
     */
    public List<EncryptionKey> getKeysForContact(long contactId) {
        return keyDao.getKeysForContact(contactId);
    }

    /**
     * Retourne la clé privée RSA-4096 déchiffrée stockée localement. La valeur est
     * lue depuis {@link ConfigRepository} (EncryptedSharedPreferences).
     *
     * @return le PEM de la clé privée, ou null si non encore synchronisée
     */
    public String getLocalPrivateKey() {
        return configRepository.getPrivateKey();
    }

    /**
     * Indique si une clé privée est disponible localement.
     *
     * @return {@code true} si la clé privée est présente et non vide
     */
    public boolean hasPrivateKey() {
        String key = configRepository.getPrivateKey();
        return key != null && !key.isEmpty();
    }

    // -------------------------------------------------------------------------
    // Gestion du cycle de vie des clés
    // -------------------------------------------------------------------------

    /**
     * Désactive toutes les clés ACTIVE d'un contact.
     *
     * <p>
     * À appeler avant d'activer une nouvelle clé pour garantir l'unicité de la clé
     * active par contact.
     *
     * @param contactId identifiant serveur du contact
     */
    public void deactivateAllKeysForContact(long contactId) {
        executor.execute(() -> {
            keyDao.deactivateAllForContact(contactId);
            Log.d(TAG, "Clés désactivées pour le contact id=" + contactId);
        });
    }

    /**
     * Supprime la clé privée stockée localement (nettoyage à la déconnexion).
     * Efface également le token d'authentification.
     */
    public void clearLocalPrivateKey() {
        configRepository.setPrivateKey(null);
        Log.d(TAG, "Clé privée locale supprimée");
    }

    // -------------------------------------------------------------------------
    // Helpers privés
    // -------------------------------------------------------------------------

    private <T> void dispatchSuccess(Callback<T> callback, T value) {
        if (callback == null)
            return;
        mainHandler.post(() -> callback.onSuccess(value));
    }

    private <T> void dispatchError(Callback<T> callback, String message, Throwable cause) {
        if (callback == null)
            return;
        mainHandler.post(() -> callback.onError(message, cause));
    }
}
