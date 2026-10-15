<<<<<<< HEAD
import java.net.URI
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// Per-machine settings (API address, Firebase project) live in the untracked local.properties.
val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}

val configuredHost: String? = localProperties.getProperty("payceylon.host")?.trim()?.takeIf { it.isNotEmpty() }
val defaultApiUrl: String = when {
    configuredHost != null -> "http://$configuredHost:8000/"
    else -> "http://10.0.2.2:8000/"
}
val defaultEmulatorHost: String = configuredHost ?: run {
    val apiUrl = localProperties.getProperty("payceylon.apiBaseUrl") ?: ""
    val uriHost = try {
        URI(apiUrl).host
    } catch (_: Exception) {
        null
    }
    if (uriHost != null && uriHost.isNotEmpty() && uriHost != "10.0.2.2" && uriHost != "localhost" && uriHost != "127.0.0.1") {
        uriHost
    } else {
        ""
    }
}

fun localSetting(key: String, default: String = ""): String =
    "\"" + (localProperties.getProperty(key) ?: default).replace("\"", "\\\"") + "\""

=======
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

>>>>>>> dd9f1d175672de1daa0c6fec1db2025dcf550834
android {
    namespace = "com.payceylon.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.payceylon.app"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
<<<<<<< HEAD

        buildConfigField("String", "API_BASE_URL", localSetting("payceylon.apiBaseUrl", defaultApiUrl))
        buildConfigField("String", "FIREBASE_API_KEY", localSetting("payceylon.firebase.apiKey", "demo-api-key"))
        buildConfigField("String", "FIREBASE_APP_ID", localSetting("payceylon.firebase.appId", "1:123456789012:android:payceylon"))
        buildConfigField("String", "FIREBASE_PROJECT_ID", localSetting("payceylon.firebase.projectId", "demo-payceylon"))
        buildConfigField("String", "FIREBASE_AUTH_EMULATOR_HOST", localSetting("payceylon.firebase.authEmulatorHost", defaultEmulatorHost))
        val portStr = localProperties.getProperty("payceylon.firebase.authEmulatorPort") ?: "9099"
        buildConfigField("int", "FIREBASE_AUTH_EMULATOR_PORT", portStr)
    }

    buildTypes {
        debug {
            // The local API is plain HTTP on the developer's network.
            manifestPlaceholders["usesCleartextTraffic"] = "true"
        }
        release {
            manifestPlaceholders["usesCleartextTraffic"] = "false"
=======
    }

    buildTypes {
        release {
>>>>>>> dd9f1d175672de1daa0c6fec1db2025dcf550834
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.zxing.core)
    implementation(libs.haze)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
<<<<<<< HEAD
    implementation(libs.okhttp)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.play.services)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
=======
    implementation(libs.kotlinx.coroutines.play.services)
    testImplementation(libs.junit)
>>>>>>> dd9f1d175672de1daa0c6fec1db2025dcf550834
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
