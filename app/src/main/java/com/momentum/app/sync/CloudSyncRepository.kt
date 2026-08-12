package com.momentum.app.sync

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.momentum.app.data.export.CompletionDto
import com.momentum.app.data.export.HabitDto
import com.momentum.app.data.export.toDomain
import com.momentum.app.data.export.toDto
import com.momentum.app.data.repository.HabitRepository
import com.momentum.app.domain.model.Completion
import com.momentum.app.domain.model.Habit
import kotlinx.coroutines.tasks.await

sealed interface SyncResult {
    data class Success(val habitCount: Int, val completionCount: Int) : SyncResult
    data class Failure(val message: String) : SyncResult
}

/**
 * Best-effort two-way sync. Local Room stays the source of truth for everything the app reads
 * and writes day to day — this never runs on the hot path of ticking a habit off, only on a
 * manual "Sync now" or the periodic background worker.
 *
 * Habits merge last-write-wins on [Habit.updatedAt]. Completions merge as a straight union (a
 * date marked done on either device stays done after sync) since "done" has no natural LWW
 * ordering. That union approach means unchecking a habit on one device, while another device is
 * offline with the old (checked) state, can resurrect the completion once both sync — a known
 * limitation of this v1 scaffold; a real tombstone-based delete would close that gap.
 *
 * Bigger known limitation: habits and completions are keyed by Room's local autoincrement
 * [Habit.id], which is only unique per device. Two devices that each create habits *before ever
 * syncing* can independently produce the same id (both start counting from 1), and the first
 * sync would then merge those two unrelated habits into one instead of keeping both. This is
 * fine for one habit-tracking identity synced across a phone + tablet from day one, but isn't
 * safe yet for "two devices building up independent history, then linking them" — that needs a
 * device-independent id (e.g. a UUID assigned at creation) as the sync/merge key instead.
 */
class CloudSyncRepository(
    private val repository: HabitRepository,
    private val authManager: AuthManager,
) {
    private val firestore: FirebaseFirestore? by lazy {
        if (!authManager.isConfigured) null else runCatching { FirebaseFirestore.getInstance() }.getOrNull()
    }

    suspend fun sync(): SyncResult {
        val db = firestore ?: return SyncResult.Failure("Cloud sync isn't set up yet")
        val uid = authManager.currentUser?.uid ?: return SyncResult.Failure("Not signed in")

        return try {
            val habitsRef = db.collection("users").document(uid).collection("habits")
            val completionsRef = db.collection("users").document(uid).collection("completions")

            val remoteHabits = habitsRef.get().await().documents.mapNotNull { it.toHabitDto() }
            val remoteCompletions = completionsRef.get().await().documents.mapNotNull { it.toCompletionDto() }

            val localHabits = repository.getAllHabitsOnce()
            val localCompletions = repository.getAllCompletionsOnce()

            val mergedHabitsById = LinkedHashMap<Long, Habit>()
            localHabits.forEach { mergedHabitsById[it.id] = it }
            remoteHabits.forEach { dto ->
                val remote = dto.toDomain()
                val local = mergedHabitsById[remote.id]
                if (local == null || remote.updatedAt.isAfter(local.updatedAt)) {
                    mergedHabitsById[remote.id] = remote
                }
            }

            val mergedCompletionsByKey = LinkedHashMap<Pair<Long, String>, Completion>()
            localCompletions.forEach { mergedCompletionsByKey[it.habitId to it.date.toString()] = it }
            remoteCompletions.forEach { dto ->
                val key = dto.habitId to dto.date
                if (key !in mergedCompletionsByKey) {
                    mergedCompletionsByKey[key] = dto.toDomain()
                }
            }

            val mergedHabits = mergedHabitsById.values.toList()
            val mergedCompletions = mergedCompletionsByKey.values.toList()

            repository.replaceAllData(mergedHabits, mergedCompletions)

            val batch = db.batch()
            mergedHabits.forEach { habit -> batch.set(habitsRef.document(habit.id.toString()), habit.toDto().toFirestoreMap()) }
            mergedCompletions.forEach { completion ->
                val docId = "${completion.habitId}_${completion.date}"
                batch.set(completionsRef.document(docId), completion.toDto().toFirestoreMap())
            }
            batch.commit().await()

            SyncResult.Success(habitCount = mergedHabits.size, completionCount = mergedCompletions.size)
        } catch (e: Exception) {
            SyncResult.Failure(e.message ?: "Sync failed")
        }
    }
}

// Firestore's reflection-based toObject() needs a no-arg constructor, which these Dtos don't
// have, so conversion is done by hand against plain maps instead.

private fun HabitDto.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "name" to name,
    "iconKey" to iconKey,
    "colorKey" to colorKey,
    "frequency" to frequency,
    "targetDaysPerWeek" to targetDaysPerWeek,
    "reminderTime" to reminderTime,
    "sortOrder" to sortOrder,
    "createdAt" to createdAt,
    "archived" to archived,
    "updatedAt" to updatedAt,
)

private fun DocumentSnapshot.toHabitDto(): HabitDto? {
    val name = getString("name") ?: return null
    val id = getLong("id") ?: return null
    val iconKey = getString("iconKey") ?: return null
    val colorKey = getString("colorKey") ?: return null
    val frequency = getString("frequency") ?: return null
    val createdAt = getString("createdAt") ?: return null
    return HabitDto(
        id = id,
        name = name,
        iconKey = iconKey,
        colorKey = colorKey,
        frequency = frequency,
        targetDaysPerWeek = (getLong("targetDaysPerWeek") ?: 7L).toInt(),
        reminderTime = getString("reminderTime"),
        sortOrder = (getLong("sortOrder") ?: 0L).toInt(),
        createdAt = createdAt,
        archived = getBoolean("archived") ?: false,
        updatedAt = getString("updatedAt"),
    )
}

private fun CompletionDto.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "habitId" to habitId,
    "date" to date,
    "completedAt" to completedAt,
)

private fun DocumentSnapshot.toCompletionDto(): CompletionDto? {
    val habitId = getLong("habitId") ?: return null
    val date = getString("date") ?: return null
    val completedAt = getString("completedAt") ?: return null
    return CompletionDto(id = 0L, habitId = habitId, date = date, completedAt = completedAt)
}
