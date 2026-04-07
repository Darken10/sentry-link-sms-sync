# SentryLink — Documentation des Endpoints API

> **Base URL** : configurable via `ConfigRepository.getApiBaseUrl()`  
> **Défaut** : `https://api.sentrylink.mil`  
> **Version** : v1

---

## Table des matières

1. [Authentification](#1-authentification)
2. [Messages](#2-messages)
3. [Localisation](#3-localisation)
4. [SOS](#4-sos)
5. [Contacts](#5-contacts)
6. [Clés de chiffrement](#6-clés-de-chiffrement)
7. [Mécanisme de fallback (Backend → SMS)](#7-mécanisme-de-fallback-backend--sms)
8. [Format du message SMS (protocole SL1)](#8-format-du-message-sms-protocole-sl1)
9. [Chiffrement de bout en bout (E2E)](#9-chiffrement-de-bout-en-bout-e2e)

---

## 1. Authentification

### `POST /api/v1/public/contact-auth`

Authentification d'un contact via le token issu du QR code.

**Headers** :
| Header | Description |
|--------|-------------|
| `Content-Type` | `application/json` |

**Query Parameter** :
| Paramètre | Type | Description |
|-----------|------|-------------|
| `token` | String | SHA-512 du token brut lu depuis le QR code |

**Request Body** : vide (`{}`)

**Réponse 200 OK** :
```json
{
  "contactId": 42,
  "name": "Unité Alpha",
  "identifier": "CONT-001",
  "status": "ACTIVE",
  "credential": "sha512_hash_du_token"
}
```

**Codes d'erreur** :
| Code | Description |
|------|-------------|
| 401 | Token invalide ou expiré |
| 403 | Contact suspendu ou détruit |

---

## 2. Messages

### `POST /api/v1/messages/send`

Envoie un message chiffré de bout en bout.

**Headers** :
| Header | Type | Description |
|--------|------|-------------|
| `X-SentryLink-Signature` | String | HMAC-SHA256 du message en clair (Base64) |
| `X-SentryLink-Device` | String | Indicatif de l'appareil expéditeur (ex: `UNIT-001`) |
| `Content-Type` | `application/json` | |

**Query Parameter** :
| Paramètre | Type | Description |
|-----------|------|-------------|
| `token` | String | Credential SHA-512 de l'expéditeur |

**Request Body** :
```json
{
  "recipient": "+22890001234",
  "data": "base64(rsa_wrapped_key):base64(iv):base64(ciphertext)",
  "type": "MSG",
  "timestamp": "1711536000000"
}
```

| Champ | Type | Description |
|-------|------|-------------|
| `recipient` | String | Numéro de téléphone du destinataire |
| `data` | String | Message chiffré (RSA-4096/OAEP + AES-256-GCM hybride) |
| `type` | String | Type de message : `MSG`, `SOS`, `LOC` |
| `timestamp` | String | Horodatage en millisecondes epoch |

**Réponse 200 OK** :
```json
{
  "status": "delivered",
  "messageId": "uuid-du-message",
  "channel": "API"
}
```

**Codes d'erreur** :
| Code | Description |
|------|-------------|
| 400 | Payload invalide ou champ manquant |
| 401 | Non authentifié |
| 404 | Destinataire inconnu |
| 502 | Destinataire injoignable (déclenche le fallback SMS côté client) |

---

### `POST /api/v1/messages/poll`

Récupère les messages en attente pour cet appareil.

**Headers** :
| Header | Type | Description |
|--------|------|-------------|
| `X-SentryLink-Signature` | String | HMAC-SHA256 de la requête |
| `X-SentryLink-Device` | String | Indicatif de l'appareil |

**Request Body** :
```json
{
  "lastPollTimestamp": "1711536000000"
}
```

**Réponse 200 OK** :
```json
{
  "messages": [
    {
      "id": "uuid",
      "sender": "+22890001234",
      "data": "base64(rsa_wrapped_key):base64(iv):base64(ciphertext)",
      "type": "MSG",
      "timestamp": 1711536000000,
      "signature": "base64_hmac_sha256"
    }
  ],
  "hasMore": false
}
```

**Déchiffrement côté client** :
1. Extraire les 3 segments du champ `data` (séparés par `:`)
2. Désenvelopper la clé AES avec la clé privée RSA-4096 du destinataire (OAEP/SHA-256)
3. Déchiffrer le contenu avec AES-256-GCM (IV + ciphertext)
4. Vérifier la signature HMAC-SHA256

---

## 3. Localisation

### `POST /api/v1/location/report`

Rapporte la position GPS/GSM de l'appareil.

**Headers** :
| Header | Type | Description |
|--------|------|-------------|
| `X-SentryLink-Signature` | String | HMAC-SHA256 du payload |
| `X-SentryLink-Device` | String | Indicatif de l'appareil |

**Request Body** :
```json
{
  "recipient": "TOWER",
  "data": "base64(rsa_wrapped_key):base64(iv):base64(ciphertext_contenant_coords_json)",
  "type": "LOC",
  "timestamp": "1711536000000"
}
```

Le champ `data` chiffré contient (une fois déchiffré) :
```json
{
  "latitude": 6.1725,
  "longitude": 1.2314,
  "accuracy": 12.5,
  "source": "GPS",
  "altitude": 150.0
}
```

**Réponse 200 OK** :
```json
{
  "status": "received"
}
```

---

## 4. SOS

### `POST /api/v1/sos/alert`

Envoie une alerte d'urgence avec la position.

**Headers** :
| Header | Type | Description |
|--------|------|-------------|
| `X-SentryLink-Signature` | String | HMAC-SHA256 du payload |
| `X-SentryLink-Device` | String | Indicatif de l'appareil |

**Request Body** :
```json
{
  "recipient": "TOWER",
  "data": "base64_encrypted_sos_payload",
  "type": "SOS",
  "timestamp": "1711536000000"
}
```

**Réponse 200 OK** :
```json
{
  "status": "alert_received",
  "alertId": "uuid-de-l-alerte"
}
```

---

## 5. Contacts

### `GET /api/v1/contacts`

Retourne la liste de tous les contacts avec leur clé publique RSA-4096 active.

**Headers** :
| Header | Type | Description |
|--------|------|-------------|
| `X-Contact-Auth` | String | Credential SHA-512 du contact authentifié |

**Réponse 200 OK** :
```json
[
  {
    "id": 42,
    "uuid": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Unité Alpha",
    "identifier": "CONT-001",
    "phoneNumber": "+22890001234",
    "status": "ACTIVE",
    "type": "UNIT",
    "activeKeyUuid": "key-uuid-123",
    "activePublicKey": "-----BEGIN PUBLIC KEY-----\nMIICIjANBg...\n-----END PUBLIC KEY-----",
    "activeKeyStatus": "ACTIVE"
  }
]
```

| Champ | Type | Description |
|-------|------|-------------|
| `id` | Long | Identifiant serveur (bigint) |
| `uuid` | String | UUID unique du contact |
| `name` | String | Nom complet |
| `identifier` | String | Identifiant métier (ex: CONT-001) |
| `phoneNumber` | String | Numéro de téléphone |
| `status` | String | `ACTIVE`, `SUSPENDED`, `DESTROYED`, `AUTO_DESTROYED`, `INACTIVE` |
| `type` | String | `CENTRAL_SERVER` ou `UNIT` |
| `activeKeyUuid` | String | UUID de la clé publique active (nullable) |
| `activePublicKey` | String | Clé publique RSA-4096 PEM (nullable) |
| `activeKeyStatus` | String | Statut de la clé : `ACTIVE`, `EXPIRED`, `INACTIVE` |

---

## 6. Clés de chiffrement

### `GET /api/v1/my-private-key`

Retourne la clé privée RSA-4096 du contact appelant, chiffrée en AES-256-GCM.

**Headers** :
| Header | Type | Description |
|--------|------|-------------|
| `X-Contact-Auth` | String | Credential SHA-512 |

**Réponse 200 OK** :
```json
{
  "encryptedPrivateKey": "base64url(iv):base64url(ciphertext+tag)"
}
```

**Déchiffrement côté client** :
1. Séparer par `:` → `iv` (base64url) et `ciphertext+tag` (base64url)
2. Dériver la clé AES : `SHA-256(credential)` → 32 octets
3. Déchiffrer en AES-256-GCM avec l'IV et la clé dérivée
4. Résultat : clé privée RSA-4096 au format PEM (PKCS#8)

**Réponse 204 No Content** : aucune clé active pour ce contact.

---

### `GET /api/v1/default-public-key`

Retourne la clé publique RSA-4096 par défaut. Utilisée pour chiffrer les messages destinés à un numéro non enregistré dans la base de contacts.

**Headers** :
| Header | Type | Description |
|--------|------|-------------|
| `X-Contact-Auth` | String | Credential SHA-512 |

**Réponse 200 OK** :
```json
{
  "publicKey": "-----BEGIN PUBLIC KEY-----\nMIICIjANBg...\n-----END PUBLIC KEY-----",
  "keyUuid": "default-key-uuid"
}
```

---

## 7. Mécanisme de fallback (Backend → SMS)

### Flux d'envoi d'un message

```
┌─────────────┐     ┌──────────────────────┐     ┌──────────────────┐
│  Expéditeur  │────▶│  MessageDispatcher   │────▶│  Résolution clé  │
└─────────────┘     └──────────────────────┘     └──────────────────┘
                              │                           │
                              │  Clé RSA résolue          │
                              │  (locale / cache / API)   │
                              ▼                           │
                    ┌──────────────────┐                  │
                    │ Internet dispo ? │                  │
                    └──────────────────┘                  │
                       │ OUI        │ NON                 │
                       ▼            ▼                     │
              ┌────────────┐  ┌─────────────┐            │
              │  API REST  │  │  SIM dispo? │            │
              │  (HTTPS)   │  └─────────────┘            │
              └────────────┘     │ OUI    │ NON          │
                  │ Succès?      ▼        ▼              │
                  │         ┌────────┐  ┌───────┐        │
                  │ OUI     │  SMS   │  │ERREUR │        │
                  ▼         │chiffré │  │aucun  │        │
              ┌────────┐    │  SL1   │  │canal  │        │
              │  FIN   │    └────────┘  └───────┘        │
              │ (API)  │                                  │
              └────────┘                                  │
                  │ NON (erreur API)                      │
                  ▼                                       │
              ┌─────────────┐                             │
              │  SIM dispo? │                             │
              └─────────────┘                             │
                 │ OUI    │ NON                           │
                 ▼        ▼                               │
            ┌────────┐  ┌───────┐                         │
            │  SMS   │  │ERREUR │                         │
            │chiffré │  └───────┘                         │
            │  SL1   │                                    │
            └────────┘                                    │
```

### Ordre de résolution de la clé publique du destinataire

1. **Base locale** : table `encryption_key` via `contact.phone_number` → clé ACTIVE
2. **Cache local** : `ConfigRepository.getDefaultPublicKey()` (clé par défaut mise en cache)
3. **Backend** : `GET /api/v1/default-public-key` → mise en cache pour les prochains envois
4. **Fallback** : si aucune clé RSA disponible → chiffrement AES-256-GCM local (Android Keystore)

### Priorité des canaux

| Priorité | Canal | Condition | Chiffrement |
|----------|-------|-----------|-------------|
| 1 | **API REST (HTTPS)** | Internet disponible | RSA-4096/OAEP + AES-256-GCM (E2E) |
| 2 | **SMS chiffré (SL1)** | SIM disponible, API échouée ou pas d'internet | RSA-4096/OAEP + AES-256-GCM (E2E) dans le corps SL1 |

### Flux de réception

```
┌──────────────┐     ┌──────────────────┐     ┌────────────────────┐
│  SMS entrant │────▶│  SmsReceiver     │────▶│  Préfixe SL1 ?     │
│  (broadcast) │     │  (priority 1000) │     └────────────────────┘
└──────────────┘     └──────────────────┘        │ OUI        │ NON
                                                  ▼            ▼
                                         ┌──────────────┐  ┌─────────┐
                                         │  Déchiffrer  │  │ Ignorer │
                                         │  SmsEncryptor│  └─────────┘
                                         └──────────────┘
                                                  │
                                                  ▼
                                         ┌──────────────┐
                                         │  Vérifier    │
                                         │  HMAC-SHA256 │
                                         └──────────────┘
                                            │ OK      │ KO
                                            ▼         ▼
                                     ┌──────────┐  ┌─────────┐
                                     │ Stocker  │  │ Rejeter │
                                     │ en BDD   │  └─────────┘
                                     └──────────┘
                                            │
                                            ▼
                                     ┌──────────────────┐
                                     │ Broadcast local  │
                                     │ NEW_SMS intent   │
                                     └──────────────────┘
```

---

## 8. Format du message SMS (protocole SL1)

### Structure

```
SL1<HEADER_CHIFFRE>::<BODY_CHIFFRE>
```

| Bloc | Contenu | Observation |
|------|---------|-------------|
| **Préfixe** | `SL1` | Identifie le protocole SentryLink v1 |
| **Header** | `base64(JSON)` — Unit_ID, Message_Type, Timestamp, Routing_Mode | Métadonnées de routage (non chiffré, encodé Base64) |
| **Séparateur** | `::` | Distingue l'en-tête du corps |
| **Body** | `base64(JSON)` — données chiffrées + signature | Bloc chiffré E2E pour le destinataire |

### Header (décodé Base64 → JSON)

```json
{
  "uid": "UNIT-001",
  "t": "MSG",
  "ts": 1711536000000,
  "r": "SMS"
}
```

| Champ | Type | Valeurs | Description |
|-------|------|---------|-------------|
| `uid` | String | - | Indicatif de l'appareil expéditeur |
| `t` | String | `MSG`, `SOS`, `LOC` | Type de message |
| `ts` | Long | - | Horodatage en millisecondes epoch |
| `r` | String | `SMS` | Mode de routage |

### Body (décodé Base64 → JSON)

```json
{
  "d": "base64(rsa_wrapped_key):base64(iv):base64(ciphertext)",
  "s": "base64_hmac_sha256_signature"
}
```

| Champ | Type | Description |
|-------|------|-------------|
| `d` | String | Message chiffré RSA-4096/OAEP + AES-256-GCM hybride (ou AES-256-GCM local si pas de clé RSA) |
| `s` | String | Signature HMAC-SHA256 du message en clair (Base64) |

### Exemple complet

```
SL1eyJ1aWQiOiJVTklULTAwMSIsInQiOiJNU0ciLCJ0cyI6MTcxMTUzNjAwMDAwMCwiciI6IlNNUyJ9::eyJkIjoiYWJjMTIzOnl6Nzg5OmRlZjQ1NiIsInMiOiJiYXNlNjRfaG1hYyJ9
```

---

## 9. Chiffrement de bout en bout (E2E)

### Schéma global

```
┌──────────────────────────────────────────────────────┐
│                    EXPÉDITEUR                         │
│                                                       │
│  1. Générer clé AES-256 éphémère                     │
│  2. Chiffrer le message avec AES-256-GCM              │
│  3. Envelopper la clé AES avec RSA-4096/OAEP          │
│     (clé publique du destinataire)                    │
│  4. Format: base64(wrapped_key):base64(iv):base64(ct) │
│  5. Signer le message clair en HMAC-SHA256            │
└──────────────────────┬───────────────────────────────┘
                       │
                       ▼
              ┌────────────────┐
              │  Transit       │
              │  (API ou SMS)  │
              └────────┬───────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────┐
│                   DESTINATAIRE                        │
│                                                       │
│  1. Extraire les 3 segments (wrapped_key, iv, ct)     │
│  2. Désenvelopper la clé AES avec RSA-4096/OAEP       │
│     (clé privée du destinataire)                      │
│  3. Déchiffrer le contenu avec AES-256-GCM            │
│  4. Vérifier la signature HMAC-SHA256                 │
└──────────────────────────────────────────────────────┘
```

### Algorithmes utilisés

| Composant | Algorithme | Taille de clé |
|-----------|-----------|---------------|
| Chiffrement asymétrique | RSA/ECB/OAEPWithSHA-256AndMGF1Padding | 4096 bits |
| Chiffrement symétrique | AES/GCM/NoPadding | 256 bits |
| Tag d'authentification GCM | - | 128 bits |
| Signature | HMAC-SHA256 | - |
| Stockage clé privée | EncryptedSharedPreferences (AES-256-GCM, Android Keystore) | - |
| Dérivation clé (déchiffrement clé privée serveur) | SHA-256(credential) | 256 bits |

### Gestion des clés

| Scénario | Clé utilisée | Source |
|----------|-------------|--------|
| Destinataire connu (dans la base locale) | Clé publique RSA-4096 du contact | Table `encryption_key` (statut ACTIVE) |
| Destinataire inconnu + clé par défaut en cache | Clé publique par défaut | `ConfigRepository` (cache local) |
| Destinataire inconnu + pas de cache | Clé publique par défaut | `GET /api/v1/default-public-key` → mise en cache |
| Aucune clé RSA disponible | AES-256-GCM local (Android Keystore) | `CryptoManager` (clé hardware-backed) |

### Rotation des clés

- Chaque contact ne peut avoir qu'**une seule clé ACTIVE** à la fois.
- Lors de la synchronisation (`GET /api/v1/contacts`), si une nouvelle clé active est reçue, les anciennes sont passées à `INACTIVE`.
- La clé privée est re-synchronisée automatiquement au démarrage si elle n'est pas déjà stockée localement.
