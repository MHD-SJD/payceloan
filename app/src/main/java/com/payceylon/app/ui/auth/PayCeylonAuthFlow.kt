package com.payceylon.app.ui.auth

<<<<<<< HEAD
=======
import android.app.Activity
>>>>>>> dd9f1d175672de1daa0c6fec1db2025dcf550834
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
<<<<<<< HEAD
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
=======
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
>>>>>>> dd9f1d175672de1daa0c6fec1db2025dcf550834
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
<<<<<<< HEAD
import com.google.firebase.auth.PhoneAuthProvider
import com.payceylon.app.data.ApiException
import com.payceylon.app.data.CodeRequest
import com.payceylon.app.data.PhoneAuth
import com.payceylon.app.ui.components.findActivity
import com.payceylon.app.ui.components.rememberSubmitState

private enum class AuthDestination { Onboarding, Otp }

/** Phone sign-in: Firebase texts a code, and a confirmed code signs the user in. */
@Composable
fun PayCeylonAuthFlow(onAuthenticated: () -> Unit) {
=======
import android.widget.Toast
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.payceylon.app.data.BackendApi
import com.payceylon.app.data.LocalCache
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

private enum class AuthDestination { Onboarding, Otp }

@Composable
fun PayCeylonAuthFlow(onAuthenticated: (phoneNumber: String, profile: String) -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
>>>>>>> dd9f1d175672de1daa0c6fec1db2025dcf550834
    var destination by rememberSaveable { mutableStateOf(AuthDestination.Onboarding) }
    var onboardingPage by rememberSaveable { mutableIntStateOf(0) }
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var verificationId by rememberSaveable { mutableStateOf<String?>(null) }
<<<<<<< HEAD
    var resendToken by rememberSaveable { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }
    val context = LocalContext.current
    val submit = rememberSubmitState()

    /** Texts a code to the entered number; resending reuses Firebase's token. */
    fun requestCode(resend: Boolean) = submit.run {
        val activity = context.findActivity() ?: return@run
        val request = PhoneAuth.requestCode(activity, "+94${normalizeSriLankanMobileNumber(phoneNumber)}", resendToken.takeIf { resend })
        when (request) {
            is CodeRequest.Sent -> {
                verificationId = request.verificationId
                resendToken = request.resendToken
                destination = AuthDestination.Otp
            }
            is CodeRequest.Verified -> {
                PhoneAuth.signIn(request.credential)
                onAuthenticated()
=======
    var isVerifying by rememberSaveable { mutableStateOf(false) }
    var verificationError by rememberSaveable { mutableStateOf<String?>(null) }
    var signInCompleted by rememberSaveable { mutableStateOf(false) }

    fun finishSignIn(user: com.google.firebase.auth.FirebaseUser?) {
        if (signInCompleted) return
        scope.launch {
            try {
                val idToken = user?.getIdToken(false)?.await()?.token
                    ?: error("Firebase did not return a sign-in token.")
                val profile = BackendApi.verifyPhone(idToken).getJSONObject("user")
                val snapshots = mapOf(
                    "profile" to profile.toString(),
                    "balances" to BackendApi.get("/wallet/balances").toString(),
                    "transactions" to BackendApi.get("/transactions?limit=50").toString(),
                    "paymentMethods" to BackendApi.get("/payment-methods").toString(),
                    "notifications" to BackendApi.get("/notifications").toString(),
                    "settings" to BackendApi.get("/me/settings").toString(),
                )
                LocalCache(context).use { cache -> snapshots.forEach(cache::put) }
                signInCompleted = true
                onAuthenticated(phoneNumber, profile.toString())
            } catch (error: Exception) {
                val cachedProfile = LocalCache(context).use { it.get("profile") }
                if (cachedProfile != null) {
                    signInCompleted = true
                    onAuthenticated(phoneNumber, cachedProfile)
                } else {
                    isVerifying = false
                    verificationError = error.message ?: "Could not finish sign-in."
                }
>>>>>>> dd9f1d175672de1daa0c6fec1db2025dcf550834
            }
        }
    }

<<<<<<< HEAD
=======
    fun requestCode() {
        if (activity == null) {
            verificationError = "Phone verification is unavailable on this screen."
            Toast.makeText(context, verificationError, Toast.LENGTH_LONG).show()
            return
        }
        verificationError = null
        val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
            .setPhoneNumber("+94${normalizeSriLankanMobileNumber(phoneNumber)}")
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: com.google.firebase.auth.PhoneAuthCredential) {
                    isVerifying = true
                    FirebaseAuth.getInstance().signInWithCredential(credential)
                        .addOnSuccessListener { finishSignIn(it.user) }
                        .addOnFailureListener {
                            isVerifying = false
                            verificationError = it.message ?: "Phone verification failed."
                        }
                }

                override fun onVerificationFailed(error: FirebaseException) {
                    verificationError = error.message ?: "Could not send a verification code."
                    Toast.makeText(context, verificationError, Toast.LENGTH_LONG).show()
                }

                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                    verificationId = id
                    destination = AuthDestination.Otp
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    LaunchedEffect(Unit) {
        FirebaseAuth.getInstance().currentUser?.let(::finishSignIn)
    }

>>>>>>> dd9f1d175672de1daa0c6fec1db2025dcf550834
    BackHandler(enabled = destination == AuthDestination.Otp) {
        destination = AuthDestination.Onboarding
        onboardingPage = 2
    }
<<<<<<< HEAD
    BackHandler(
        enabled = destination == AuthDestination.Onboarding && onboardingPage > 0,
    ) {
=======
    BackHandler(enabled = destination == AuthDestination.Onboarding && onboardingPage > 0) {
>>>>>>> dd9f1d175672de1daa0c6fec1db2025dcf550834
        onboardingPage--
    }

    AnimatedContent(
        targetState = destination,
        transitionSpec = {
            val slideSpec = spring<IntOffset>(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            )
            if (targetState == AuthDestination.Otp) {
                (slideInHorizontally(slideSpec) { it } + fadeIn(tween(160))) togetherWith
                    (slideOutHorizontally(slideSpec) { -it / 3 } + fadeOut(tween(120)))
            } else {
                (slideInHorizontally(slideSpec) { -it / 3 } + fadeIn(tween(160))) togetherWith
                    (slideOutHorizontally(slideSpec) { it } + fadeOut(tween(120)))
            }
        },
        label = "authentication navigation",
    ) { target ->
        when (target) {
            AuthDestination.Onboarding -> OnboardingScreen(
                currentPage = onboardingPage,
                phoneNumber = phoneNumber,
                onPhoneNumberChange = { phoneNumber = it },
                onPageChange = { onboardingPage = it },
<<<<<<< HEAD
                onFinished = { requestCode(resend = false) },
                isSendingCode = submit.busy,
=======
                onFinished = ::requestCode,
>>>>>>> dd9f1d175672de1daa0c6fec1db2025dcf550834
            )

            AuthDestination.Otp -> OtpScreen(
                maskedPhoneNumber = maskSriLankanMobileNumber(phoneNumber),
                onBack = {
                    destination = AuthDestination.Onboarding
                    onboardingPage = 2
                },
                onCodeComplete = { code ->
                    val id = verificationId
                    if (id == null) {
<<<<<<< HEAD
                        "Request a new code."
                    } else {
                        try {
                            PhoneAuth.signIn(PhoneAuth.credential(id, code))
                            onAuthenticated()
                            null
                        } catch (e: ApiException) {
                            e.message
                        } catch (e: kotlinx.coroutines.CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            "Something went wrong. Please try again."
                        }
                    }
                },
                onResend = { requestCode(resend = true) },
                // Signing in and signing up are the same phone verification.
                onLogIn = {
                    destination = AuthDestination.Onboarding
                    onboardingPage = 2
=======
                        verificationError = "Request a new code and try again."
                    } else {
                        isVerifying = true
                        verificationError = null
                        FirebaseAuth.getInstance()
                            .signInWithCredential(PhoneAuthProvider.getCredential(id, code))
                            .addOnSuccessListener { finishSignIn(it.user) }
                            .addOnFailureListener {
                                isVerifying = false
                                verificationError = it.message ?: "That code did not match."
                            }
                    }
                },
                isVerifying = isVerifying,
                verificationError = verificationError,
                onResend = ::requestCode,
                onLogIn = {
                    destination = AuthDestination.Onboarding
                    onboardingPage = 2
                    verificationError = null
>>>>>>> dd9f1d175672de1daa0c6fec1db2025dcf550834
                },
            )
        }
    }
}
