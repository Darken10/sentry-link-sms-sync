plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.africasys.sentrylink.smssync"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.africasys.sentrylink.smssync"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testNamespace = "com.africasys.sentrylink.smssync.test"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    packaging {
        jniLibs {
            // Required for 16 KB page-size compatibility (Android 15+).
            // Keeps native libs uncompressed so the OS can mmap them directly
            // at the correct alignment boundary.
            useLegacyPackaging = false
        }
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
    implementation(libs.recyclerview)
    implementation(libs.cardview)
    implementation(libs.zxing)
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")

    // TODO: réactiver SQLCipher une fois le problème de chargement natif résolu
    // implementation(libs.sqlcipher)
    // implementation(libs.sqlite)

    // Security - EncryptedSharedPreferences
    implementation(libs.security.crypto)

    // Location
    implementation(libs.play.services.location)

    // Network - Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // HTTP Server - NanoHTTPD (mini-serveur HTTP embarqué, zéro dépendance)
    implementation("org.nanohttpd:nanohttpd:2.3.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}