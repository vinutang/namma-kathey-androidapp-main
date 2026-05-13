package com.example.myapplication.auth

import android.app.Activity
import android.content.Context
import com.example.myapplication.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

object AuthRepository {

    private val firebaseAuth: FirebaseAuth get() = FirebaseAuth.getInstance()

    fun googleSignInOptions(context: Context): GoogleSignInOptions {
        val webId = context.getString(R.string.default_web_client_id).trim()
        val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        if (webId.isNotEmpty()) {
            builder.requestIdToken(webId).requestEmail()
        } else {
            builder.requestEmail()
        }
        return builder.build()
    }

    /** Used for sign-out; matches the client configuration used during sign-in. */
    fun defaultGoogleSignInOptions(activity: Activity): GoogleSignInOptions =
        googleSignInOptions(activity)

    fun hasWebClientConfigured(context: Context): Boolean =
        context.getString(R.string.default_web_client_id).trim().isNotEmpty()

    suspend fun firebaseSignInWithGoogle(account: GoogleSignInAccount) {
        val idToken = account.idToken
            ?: throw IllegalStateException("Missing Google ID token — add SHA-256 and OAuth Web client ID in Firebase, then rebuild.")
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential).await()
    }

    fun getGoogleIntent(context: Activity) =
        GoogleSignIn.getClient(context, googleSignInOptions(context)).signInIntent

    suspend fun signInEmailPassword(email: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(email.trim(), password).await()
    }

    suspend fun createUserEmailPassword(email: String, password: String) {
        firebaseAuth.createUserWithEmailAndPassword(email.trim(), password).await()
    }

    suspend fun sendPasswordReset(email: String) {
        firebaseAuth.sendPasswordResetEmail(email.trim()).await()
    }

    fun parseGoogleSignInError(e: Exception): String =
        when (e) {
            is ApiException -> when (e.statusCode) {
                10 -> "Google Sign-In needs release/debug SHA-256 in Firebase Console (Settings → App)."
                else -> e.localizedMessage ?: "Google Sign-In failed (${e.statusCode})."
            }
            else -> e.localizedMessage ?: "Sign-in failed."
        }

    fun parseFirebaseAuthError(e: Exception): String {
        val msg = e.localizedMessage ?: ""
        return when {
            msg.contains("BILLING_NOT_ENABLED") ->
                "Phone login failed. Please enable billing (Blaze plan) in Firebase Console."
            e is FirebaseAuthInvalidUserException ->
                "No account found with that email."
            e is FirebaseAuthInvalidCredentialsException ->
                "Wrong password or expired link."
            e is FirebaseTooManyRequestsException ->
                "Too many attempts. Try again in a minute."
            else -> e.localizedMessage ?: "Something went wrong."
        }
    }

    /** Starts SMS verification; [callbacks] receives verification id for OTP step. */
    fun verifyPhoneNumber(
        activity: Activity,
        e164Phone: String,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks,
    ) {
        val opts = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(e164Phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(opts)
    }

    suspend fun signInWithPhoneCredential(credential: PhoneAuthCredential) {
        firebaseAuth.signInWithCredential(credential).await()
    }
}
