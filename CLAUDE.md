# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.africasys.sentrylink.ExampleUnitTest"

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Clean build artifacts
./gradlew clean
```

Build config: min SDK 30, target/compile SDK 36, Java 11, AGP 8.12.3.

## Architecture

SentryLink is a military-grade secure communication Android app with encrypted end-to-end messaging between field teams and a control tower, using a dual-channel fallback: primary REST API (HTTPS) and secondary encrypted SMS.

**4-tier layered architecture:**

1. **UI Layer** — Activities (`HomeActivity`, `MessagingActivity`, `LocationActivity`, `SosActivity`, `SettingsActivity`)
2. **Service Layer** — Business logic (`MessageDispatcher`, `SosService`, `LocationService`, `SmsMonitorService` foreground service)
3. **Repository Layer** — Data access via DAOs (`SMSRepository`, `ConfigRepository`)
4. **Model Layer** — Room entities (`SMSMessage`, `LocationRecord`, `SosAlert`)

**Message flow:**
- Outbound: `MessageDispatcher` tries REST API first, falls back to SMS if offline
- Inbound SMS: `SmsReceiver` BroadcastReceiver intercepts messages with `[SL]` prefix, decrypts with `CryptoManager`, verifies HMAC with `MessageSigner`
- API messages are signed via `X-SentryLink-Signature` header (HMAC-SHA256) and identified via `X-SentryLink-Device` header

**Security layer** (`crypto/` package):
- `CryptoManager` — AES-256-GCM encryption using Android Keystore (hardware-backed when available); IV is prepended to ciphertext, Base64-encoded
- `MessageSigner` — HMAC-SHA256 signing/verification
- `SmsEncryptor` — SMS-specific encryption wrapper
- `ConfigRepository` — uses `EncryptedSharedPreferences` (AES256_SIV keys, AES256_GCM values)

**Database:** Room ORM with `AppDatabase` singleton. Three DAOs: `SmsDao`, `LocationDao`, `SosDao`.
- `allowMainThreadQueries()` is currently enabled (dev convenience — not for production)
- `fallbackToDestructiveMigration()` is set — schema changes wipe data
- SQLCipher (AES-256 DB encryption) is commented out in `app/build.gradle.kts` due to a native library loading issue (TODO)

**Networking** (`network/` package): Retrofit 2 + OkHttp 4 singleton (`ApiClient`). Four endpoints all under `/api/v1/`: `messages/send`, `messages/poll`, `location/report`, `sos/alert`.

## Known Issues / TODOs

- **SQLCipher disabled**: dependency commented out in `app/build.gradle.kts` lines 54–56; database is unencrypted until resolved
- **SplashActivity disabled**: commented out in `AndroidManifest.xml` due to a database initialization issue
- **Main thread DB queries**: `allowMainThreadQueries()` must be removed and replaced with async (coroutines/LiveData) before production
