package com.example.myapplication.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.example.myapplication.AuthActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ProgressOwner(
    val uid: String,
    val displayName: String,
)

class UserSessionStore(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _owner = MutableStateFlow(recomputeOwner())
    val owner: StateFlow<ProgressOwner> = _owner.asStateFlow()

    init {
        FirebaseAuth.getInstance().addAuthStateListener { fb ->
            _owner.value = recomputeOwner(fb.currentUser)
        }
        _owner.value = recomputeOwner(FirebaseAuth.getInstance().currentUser)
    }

    fun clearGuestPreference() {
        prefs.edit().putBoolean(KEY_GUEST, false).apply()
        _owner.value = recomputeOwner(FirebaseAuth.getInstance().currentUser)
    }

    /** Guest uses a stable pseudo-UID stored in badges/progress rows. */
    fun setGuestSession() {
        FirebaseAuth.getInstance().signOut()
        prefs.edit().putBoolean(KEY_GUEST, true).apply()
        _owner.value = recomputeOwner(null)
    }

    fun canAccessMainContent(): Boolean {
        val guest = prefs.getBoolean(KEY_GUEST, false)
        val loggedIn = FirebaseAuth.getInstance().currentUser != null
        return guest || loggedIn
    }

    fun signOutReturningToAuth(activity: Activity) {
        prefs.edit().putBoolean(KEY_GUEST, false).apply()
        FirebaseAuth.getInstance().signOut()
        val opts = AuthRepository.defaultGoogleSignInOptions(activity)
        com.google.android.gms.auth.api.signin.GoogleSignIn
            .getClient(activity, opts)
            .signOut()
            .addOnCompleteListener {
                activity.startActivity(
                    Intent(activity, AuthActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
                )
                activity.finish()
            }
    }

    private fun recomputeOwner(user: FirebaseUser? = FirebaseAuth.getInstance().currentUser): ProgressOwner {
        if (user != null && prefs.getBoolean(KEY_GUEST, false)) {
            prefs.edit().putBoolean(KEY_GUEST, false).apply()
        }
        val guest = prefs.getBoolean(KEY_GUEST, false)
        return when {
            user != null -> ProgressOwner(uid = user.uid, displayName = resolvedDisplayName(user))
            guest -> ProgressOwner(uid = GUEST_UID, displayName = "Guest")
            else -> ProgressOwner(uid = "", displayName = "")
        }
    }

    companion object {
        const val GUEST_UID = "guest"
        private const val PREFS = "namma_kathey_session"
        private const val KEY_GUEST = "guest_mode"

        private fun resolvedDisplayName(user: FirebaseUser): String =
            listOf(user.displayName, user.email, user.phoneNumber)
                .mapNotNull { it?.takeIf(String::isNotBlank) }
                .firstOrNull()
                ?: "Friend"
    }
}
