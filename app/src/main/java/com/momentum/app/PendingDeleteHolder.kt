package com.momentum.app

import com.momentum.app.domain.model.Habit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Backs the "Undo" snackbar on habit deletion. Deleting a habit doesn't touch the database right
 * away — it registers the real delete here and the caller (Today screen, which the user always
 * lands back on) drives the snackbar; the delete only actually commits once that snackbar times
 * out or is dismissed, or immediately if the user undoes it, nothing happens at all.
 *
 * Lives on [AppContainer] rather than a screen's ViewModel because the Detail screen (where
 * delete is initiated) navigates away and its ViewModel is torn down well before the undo window
 * closes — this needs to outlive that navigation.
 */
class PendingDeleteHolder {

    data class Pending(val habit: Habit)

    private val _pending = MutableStateFlow<Pending?>(null)
    val pending: StateFlow<Pending?> = _pending.asStateFlow()

    private var onCommit: (suspend () -> Unit)? = null

    fun begin(habit: Habit, commit: suspend () -> Unit) {
        _pending.value = Pending(habit)
        onCommit = commit
    }

    /** No-op if [habitId] doesn't match the currently pending delete (e.g. already resolved). */
    suspend fun commitIfPending(habitId: Long) {
        if (_pending.value?.habit?.id != habitId) return
        val commit = onCommit
        _pending.value = null
        onCommit = null
        commit?.invoke()
    }

    fun undo(habitId: Long) {
        if (_pending.value?.habit?.id != habitId) return
        _pending.value = null
        onCommit = null
    }
}
