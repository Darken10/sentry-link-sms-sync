package com.africasys.sentrylink.smssync.service;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.africasys.sentrylink.smssync.dtos.ContactDto;
import com.africasys.sentrylink.smssync.models.Contact;
import com.africasys.sentrylink.smssync.network.ApiClient;
import com.africasys.sentrylink.smssync.repository.ConfigRepository;
import com.africasys.sentrylink.smssync.repository.ContactRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Response;

/**
 * Service métier pour la gestion des contacts.
 *
 * <p>
 * Responsabilités :
 * <ul>
 * <li>Synchronisation depuis l'API (GET /api/v1/contacts) — upsert atomique en
 * base locale.</li>
 * <li>Lecture locale des contacts (tous, actifs, par UUID, par
 * identifiant).</li>
 * </ul>
 *
 * <p>
 * Toutes les opérations réseau s'exécutent sur un thread d'arrière-plan via
 * {@link ExecutorService}. Les callbacks sont dispatché sur le thread
 * principal.
 */
public class ContactService {

    private static final String TAG = "SL-ContactService";

    private static volatile ContactService INSTANCE;

    private final Context appContext;
    private final ContactRepository contactRepository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // -------------------------------------------------------------------------
    // Interfaces de callback
    // -------------------------------------------------------------------------

    /** Callback générique pour les opérations réseau/base. */
    public interface Callback<T> {
        void onSuccess(T result);

        void onError(String message, Throwable cause);
    }

    // -------------------------------------------------------------------------
    // Singleton
    // -------------------------------------------------------------------------

    private ContactService(Context context) {
        appContext = context.getApplicationContext();
        contactRepository = ContactRepository.getInstance(appContext);
    }

    public static ContactService getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (ContactService.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ContactService(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    // -------------------------------------------------------------------------
    // Synchronisation depuis l'API
    // -------------------------------------------------------------------------

    /**
     * Synchronise les contacts depuis l'API et les persiste en base locale.
     *
     * <p>
     * Appelle GET /api/v1/contacts avec le credential SHA-512 du contact
     * authentifié. En cas de succès, effectue un upsert atomique via
     * {@link ContactRepository#syncFromApi(List)}.
     *
     * @param callback résultat : nombre de contacts synchronisés, ou erreur
     */
    public void syncContacts(Callback<Integer> callback) {
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
                    contactRepository.syncFromApi(dtos);
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

    // -------------------------------------------------------------------------
    // Lecture locale
    // -------------------------------------------------------------------------

    /**
     * Retourne tous les contacts stockés en base locale.
     *
     * @return liste de tous les contacts (peut être vide)
     */
    public List<Contact> getAll() {
        return contactRepository.getAll();
    }

    /**
     * Retourne uniquement les contacts dont le statut est ACTIVE.
     *
     * @return liste des contacts actifs
     */
    public List<Contact> getActiveContacts() {
        return contactRepository.getActiveContacts();
    }

    /**
     * Recherche un contact par son UUID.
     *
     * @param uuid UUID du contact
     * @return le contact correspondant, ou null si introuvable
     */
    public Contact findByUuid(String uuid) {
        return contactRepository.getAll().stream().filter(c -> uuid != null && uuid.equals(c.uuid)).findFirst()
                .orElse(null);
    }

    /**
     * Recherche un contact par son identifiant métier (ex: CONT-001).
     *
     * @param identifier identifiant métier du contact
     * @return le contact correspondant, ou null si introuvable
     */
    public Contact findByIdentifier(String identifier) {
        return contactRepository.getAll().stream().filter(c -> identifier != null && identifier.equals(c.identifier))
                .findFirst().orElse(null);
    }

    /**
     * Retourne le nombre total de contacts en base locale.
     *
     * @return nombre de contacts
     */
    public int count() {
        return contactRepository.count();
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
