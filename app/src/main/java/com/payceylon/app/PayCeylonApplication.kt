package com.payceylon.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.payceylon.app.data.AppServices

class PayCeylonApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Configured from local.properties instead of google-services.json, so no Gradle plugin is needed.
        if (FirebaseApp.getApps(this).isEmpty() && BuildConfig.FIREBASE_APP_ID.isNotEmpty()) {
            FirebaseApp.initializeApp(
                this,
                FirebaseOptions.Builder()
                    .setApiKey(BuildConfig.FIREBASE_API_KEY)
                    .setApplicationId(BuildConfig.FIREBASE_APP_ID)
                    .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                    .build(),
            )
        }

        if (BuildConfig.FIREBASE_AUTH_EMULATOR_HOST.isNotEmpty()) {
            runCatching {
                FirebaseAuth.getInstance().useEmulator(
                    BuildConfig.FIREBASE_AUTH_EMULATOR_HOST,
                    BuildConfig.FIREBASE_AUTH_EMULATOR_PORT,
                )
            }
        }

        AppServices.init(this, BuildConfig.API_BASE_URL)
    }
}
