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
import com.momentum.app.domain.model.HabitTombstone
import java.time.Duration
import java.time.Instant
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
 * ordering. That union approach means unchecking a single day on one device, while another
 * device is offline with the old (checked) state, can resurrect that one completion once both
 * sync — a known limitation of this v1 scaffold.
 *
 * Deleting a whole habit is handled with tombstones ([HabitTombstone]): deleting locally records
 * a tombstone, sync unions local + remote tombstones and drops any habit/completion whose id is
 * tombstoned from the merge (and from Firestore) instead of treating "missing locally" as "new
 * on the other device". Tombstones older than [tombstoneRetention] are pruned so the set doesn't
 * grow forever; that window needs to be longer than the longest gap between two devices syncing,
 * or a very-stale device can still resurrect a long-deleted habit.
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
            val tombstonesRef = db.collection("users").document(uid).collection("tombstones")

            val remoteHabits = habitsRef.get().await().documents.mapNotNull { it.toHabitDto() }
            val remoteCompletions = completionsRef.get().await().documents.mapNotNull { it.toCompletionDto() }
            val remoteTombstones = tombstonesRef.get().await().documents.mapNotNull { it.toTombstone() }

            val localHabits = repository.getAllHabitsOnce()
            val localCompletions = repository.getAllCompletionsOnce()
            val localTombstones = repository.getAllTombstones()

            val mergedTombstonesById = LinkedHashMap<Long, HabitTombstone>()
            (localTombstones + remoteTombstones).forEach { tombstone ->
                val existing = mergedTombstonesById[tombstone.habitId]
                if (existing == null || tombstone.deletedAt.isAfter(existing.deletedAt)) {
                    mergedTombstonesById[tombstone.habitId] = tombstone
                }
            }

            val mergedHabitsById = LinkedHashMap<Long, Habit>()
            localHabits.forEach { mergedHabitsById[it.id] = it }
            remoteHabits.forEach { dto ->
                val remote = dto.toDomain()
                val local = mergedHabitsById[remote.id]
                if (local == null || remote.updatedAt.isAfter(local.updatedAt)) {
                    mergedHabitsById[remote.id] = remote
                }
            }
            // A tombstone means this habit was deliberately deleted — don't let a remote copy
            // that predates the delete (or a local copy from before this device learned about
            // it) resurrect it.
            mergedTombstonesById.keys.forEach { mergedHabitsById.remove(it) }

            val mergedCompletionsByKey = LinkedHashMap<Pair<Long, String>, Completion>()
            localCompletions.forEach { mergedCompletionsByKey[it.habitId to it.date.toString()] = it }
            remoteCompletions.forEach { dto ->
                val key = dto.habitId to dto.date
                if (key !in mergedCompletionsByKey) {
                    mergedCompletionsByKey[key] = dto.toDomain()
                }
            }
            mergedCompletionsByKey.keys.removeAll { (habitId, _) -> habitId in mergedTombstonesById }

            val mergedHabits = mergedHabitsById.values.toList()
            val mergedCompletions = mergedCompletionsByKey.values.toList()
            val mergedTombstones = mergedTombstonesById.values.toList()

            repository.replaceAllData(mergedHabits, mergedCompletions)
            repository.saveTombstones(mergedTombstones)
            val retentionCutoff = Instant.now().minus(tombstoneRetention)
            repository.pruneTombstonesOlderThan(retentionCutoff)

            val batch = db.batch()
            mergedHabits.forEach { habit -> batch.set(habitsRef.document(habit.id.toString()), habit.toDto().toFirestoreMap()) }
            mergedCompletions.forEach { completion ->
                val docId = "${completion.habitId}_${completion.date}"
                batch.set(completionsRef.document(docId), completion.toDto().toFirestoreMap())
            }
            mergedTombstones.forEach { tombstone ->
                if (tombstone.deletedAt.isAfter(retentionCutoff)) {
                    batch.set(tombstonesRef.document(tombstone.habitId.toString()), tombstone.toFirestoreMap())
                }
                // Clean up the now-tombstoned habit on the server so a third, not-yet-synced
                // device doesn't pull it back down as "new".
                batch.delete(habitsRef.document(tombstone.habitId.toString()))
            }
            remoteCompletions
                .filter { it.habitId in mergedTombstonesById }
                .forEach { dto -> batch.delete(completionsRef.document("${dto.habitId}_${dto.date}")) }
            batch.commit().await()

            SyncResult.Success(habitCount = mergedHabits.size, completionCount = mergedCompletions.size)
        } catch (e: Exception) {
            SyncResult.Failure(e.message ?: "Sync failed")
        }
    }

    companion object {
        /** How long a tombstone is kept before it's pruned; must outlive the longest realistic gap between two devices syncing. */
        private val tombstoneRetention = Duration.ofDays(180)
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

private fun HabitTombstone.toFirestoreMap(): Map<String, Any?> = mapOf(
    "habitId" to habitId,
    "deletedAt" to deletedAt.toString(),
)

private fun DocumentSnapshot.toTombstone(): HabitTombstone? {
    val habitId = getLong("habitId") ?: return null
    val deletedAt = getString("deletedAt")?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return null
    return HabitTombstone(habitId = habitId, deletedAt = deletedAt)
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
