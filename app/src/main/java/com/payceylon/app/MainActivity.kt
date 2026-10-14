package com.payceylon.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.payceylon.app.ui.PayCeylonApp
import com.payceylon.app.ui.theme.PayCeylonTheme
import com.payceylon.app.ui.wallet.PaymentRequest
import com.payceylon.app.ui.wallet.parsePaymentRequest

class MainActivity : ComponentActivity() {
    private var pendingPaymentLink by mutableStateOf<PaymentRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(
                this,
                FirebaseOptions.Builder()
                    .setApiKey("demo-api-key")
                    .setApplicationId("1:1234567890:android:payceylon")
                    .setProjectId("demo-payceylon")
                    .build(),
            )
        }
        if (BuildConfig.DEBUG) {
            FirebaseAuth.getInstance().useEmulator(com.payceylon.app.data.BackendApi.HOST, 9099)
        }
        enableEdgeToEdge()
        // Only on a fresh launch; after recreation the link has already been handled.
        if (savedInstanceState == null) pendingPaymentLink = intent.toPaymentRequest()
        setContent {
            PayCeylonTheme {
                PayCeylonApp(
                    pendingPaymentLink = pendingPaymentLink,
                    onPaymentLinkHandled = { pendingPaymentLink = null },
                )
            }
        }
    }

    /** singleTask: links opened while the app is running arrive here. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.toPaymentRequest()?.let { pendingPaymentLink = it }
    }

    private fun Intent?.toPaymentRequest(): PaymentRequest? =
        this?.takeIf { it.action == Intent.ACTION_VIEW }?.dataString?.let(::parsePaymentRequest)
}
