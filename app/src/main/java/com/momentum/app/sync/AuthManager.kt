package com.momentum.app.sync

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Thin wrapper around Firebase Auth (email/password). Cloud sync ships fully optional: every
 * call here is safe even when no Firebase project has been wired up (no google-services.json at
 * build time) — [isConfigured] reports that so the UI can explain why sync is unavailable
 * instead of the app crashing trying to reach a project that doesn't exist.
 */
class AuthManager(context: Context) {

    private val appContext = context.applicationContext

    val isConfigured: Boolean by lazy {
        runCatching {
            FirebaseApp.getApps(appContext).isNotEmpty() || FirebaseApp.initializeApp(appContext) != null
        }.getOrDefault(false)
    }

    private val auth: FirebaseAuth? by lazy {
        if (!isConfigured) null else runCatching { FirebaseAuth.getInstance() }.getOrNull()
    }

    val currentUser: FirebaseUser? get() = auth?.currentUser

    /** Best-effort: reflects the state as of the last sign-in/reload, not a live check. */
    val isEmailVerified: Boolean get() = currentUser?.isEmailVerified ?: false

    /**
     * The OAuth "Web client" id the `google-services` plugin generates into
     * `R.string.default_web_client_id` once Google sign-in is turned on in the Firebase console
     * and a fresh `google-services.json` is downloaded — looked up by name (not by direct `R.`
     * reference) so a build with an older `google-services.json` that predates enabling Google
     * sign-in still compiles; it just won't offer the Google sign-in button.
     */
    fun googleWebClientId(): String? {
        val resId = appContext.resources.getIdentifier("default_web_client_id", "string", appContext.packageName)
        return if (resId != 0) appContext.getString(resId).takeIf { it.isNotBlank() } else null
    }

    val googleSignInAvailable: Boolean get() = isConfigured && googleWebClientId() != null

    suspend fun signInWithGoogleIdToken(idToken: String): Result<Unit> = runCatching {
        val firebaseAuth = auth ?: error("Cloud sync isn't set up yet")
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential).await()
        Unit
    }

    fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val firebaseAuth = auth
        if (firebaseAuth == null) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    suspend fun signUp(email: String, password: String): Result<Unit> = runCatching {
        val firebaseAuth = auth ?: error("Cloud sync isn't set up yet")
        firebaseAuth.createUserWithEmailAndPassword(email.trim(), password).await()
        Unit
    }

    suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        val firebaseAuth = auth ?: error("Cloud sync isn't set up yet")
        firebaseAuth.signInWithEmailAndPassword(email.trim(), password).await()
        Unit
    }

    /**
     * Firebase never has your plaintext password to send — it's never stored — so this sends a
     * one-time reset *link* to the account's email instead, the standard/secure "forgot password"
     * flow. Succeeds even for an email with no account, so this can't be used to probe which
     * emails are registered.
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = runCatching {
        val firebaseAuth = auth ?: error("Cloud sync isn't set up yet")
        firebaseAuth.sendPasswordResetEmail(email.trim()).await()
        Unit
    }

    fun signOut() {
        auth?.signOut()
    }

    /**
     * Deletes the Firebase Auth account itself. Local Room data is untouched — this only removes
     * the cloud account and (paired with [com.momentum.app.sync.CloudSyncRepository.
     * deleteAllRemoteData], called first by the caller) the data stored under it, per Play's
     * account-deletion policy. Firebase requires a *recent* sign-in for this; if the session is
     * stale it fails with a clear message asking the user to sign in again rather than a raw
     * exception.
     */
    suspend fun deleteAccount(): Result<Unit> {
        val user = auth?.currentUser ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            user.delete().await()
            Result.success(Unit)
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            Result.failure(Exception("For your security, please sign out, sign back in, then try deleting your account again."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendEmailVerification(): Result<Unit> = runCatching {
        val user = auth?.currentUser ?: error("Not signed in")
        user.sendEmailVerification().await()
        Unit
    }

    /** Refreshes [currentUser]'s cached fields (e.g. [isEmailVerified]) from the server. */
    suspend fun reloadCurrentUser(): Result<Unit> = runCatching {
        val user = auth?.currentUser ?: error("Not signed in")
        user.reload().await()
        Unit
    }
}
