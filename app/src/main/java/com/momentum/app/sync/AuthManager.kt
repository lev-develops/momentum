package com.momentum.app.sync

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
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

    fun signOut() {
        auth?.signOut()
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
