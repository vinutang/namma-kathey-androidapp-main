package com.example.myapplication.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {

    sealed interface Route {
        data object Login : Route
        data object Register : Route
        data object PhoneEnter : Route
        data object PhoneOtp : Route
    }

    data class UiMessage(val text: String)

    private val _route = MutableStateFlow<Route>(Route.Login)
    val route: StateFlow<Route> = _route.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _toast = MutableStateFlow<UiMessage?>(null)
    val toast: StateFlow<UiMessage?> = _toast.asStateFlow()

    private var verificationId: String? = null

    fun navigate(to: Route) {
        _route.value = to
    }

    fun clearToast() {
        _toast.value = null
    }

    fun showMessage(text: String) {
        _toast.value = UiMessage(text)
    }

    fun signInEmailPassword(email: String, password: String) {
        if (email.isBlank() || password.length < 6) {
            showMessage("Use a valid email and password (6+ letters).")
            return
        }
        viewModelScope.launch {
            try {
                _busy.value = true
                AuthRepository.signInEmailPassword(email, password)
            } catch (e: Exception) {
                Log.w("Auth", "signInEmail", e)
                showMessage(AuthRepository.parseFirebaseAuthError(e))
            } finally {
                _busy.value = false
            }
        }
    }

    fun registerEmailPassword(email: String, password: String, confirm: String) {
        if (password != confirm) {
            showMessage("Passwords don't match.")
            return
        }
        if (email.isBlank() || password.length < 6) {
            showMessage("Use email + password with at least 6 characters.")
            return
        }
        viewModelScope.launch {
            try {
                _busy.value = true
                AuthRepository.createUserEmailPassword(email, password)
            } catch (e: Exception) {
                Log.w("Auth", "register", e)
                showMessage(AuthRepository.parseFirebaseAuthError(e))
            } finally {
                _busy.value = false
            }
        }
    }

    fun forgotPassword(email: String) {
        if (email.isBlank()) {
            showMessage("Type your email first.")
            return
        }
        viewModelScope.launch {
            try {
                _busy.value = true
                AuthRepository.sendPasswordReset(email)
                showMessage("Check your inbox for the reset mail.")
            } catch (e: Exception) {
                Log.w("Auth", "reset", e)
                showMessage(AuthRepository.parseFirebaseAuthError(e))
            } finally {
                _busy.value = false
            }
        }
    }

    fun handleGoogleIntentResult(intent: android.content.Intent?) {
        viewModelScope.launch {
            try {
                _busy.value = true
                val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
                val account = task.await()
                AuthRepository.firebaseSignInWithGoogle(account)
            } catch (e: Exception) {
                Log.w("Auth", "google", e)
                showMessage(AuthRepository.parseGoogleSignInError(e))
            } finally {
                _busy.value = false
            }
        }
    }

    fun attachPhoneCallbacks(host: VerificationHost, e164Input: String) {
        val raw = e164Input.trim().replace("\\s+".toRegex(), "")
        val e164 = when {
            raw.startsWith("+") -> raw
            else -> "+91$raw"
        }
        if (!e164.startsWith("+") || e164.length < 10) {
            showMessage("Include country code (+91…) with your mobile number.")
            return
        }

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                signInWithSmsCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.w("Auth", "verifyPhoneFail", e)
                _busy.value = false
                showMessage(e.localizedMessage ?: "SMS verification failed.")
            }

            override fun onCodeSent(
                vid: String,
                token: PhoneAuthProvider.ForceResendingToken,
            ) {
                verificationId = vid
                _busy.value = false
                navigate(Route.PhoneOtp)
            }
        }

        _busy.value = true
        host.startFirebasePhoneVerification(callbacks, e164)
    }

    fun verifyOtpEntered(codeRaw: String) {
        val id = verificationId
        val code = codeRaw.trim()
        if (id == null || code.length < 4) {
            showMessage("Please enter the SMS code.")
            return
        }
        val credential = PhoneAuthProvider.getCredential(id, code)
        signInWithSmsCredential(credential)
    }

    private fun signInWithSmsCredential(credential: PhoneAuthCredential) {
        viewModelScope.launch {
            try {
                _busy.value = true
                AuthRepository.signInWithPhoneCredential(credential)
            } catch (e: Exception) {
                Log.w("Auth", "smsSignIn", e)
                showMessage(AuthRepository.parseFirebaseAuthError(e))
            } finally {
                _busy.value = false
            }
        }
    }

    fun interface VerificationHost {
        fun startFirebasePhoneVerification(
            callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks,
            e164Phone: String,
        )
    }
}
