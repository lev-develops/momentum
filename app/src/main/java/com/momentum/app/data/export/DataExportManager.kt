package com.momentum.app.data.export

import android.content.ContentResolver
import android.net.Uri
import com.momentum.app.data.repository.HabitRepository
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

sealed interface ImportResult {
    data class Success(val habitCount: Int, val completionCount: Int) : ImportResult
    data class Failure(val message: String) : ImportResult
}

/** Exports/imports the whole database as JSON through the system file picker (SAF). No network involved. */
class DataExportManager(
    private val repository: HabitRepository,
    private val contentResolver: ContentResolver,
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    suspend fun exportTo(uri: Uri) = withContext(Dispatchers.IO) {
        val habits = repository.getAllHabitsOnce()
        val completions = repository.getAllCompletionsOnce()
        val payload = ExportPayload(
            exportedAt = Instant.now().toString(),
            habits = habits.map { it.toDto() },
            completions = completions.map { it.toDto() },
        )
        val text = json.encodeToString(ExportPayload.serializer(), payload)
        contentResolver.openOutputStream(uri)?.use { output ->
            output.write(text.toByteArray(Charsets.UTF_8))
        } ?: error("Could not open output stream for $uri")
    }

    /** One row per completion — the flat, spreadsheet-friendly shape most CSV consumers expect.
     * Import isn't supported for CSV; JSON remains the round-trippable backup format. */
    suspend fun exportToCsv(uri: Uri) = withContext(Dispatchers.IO) {
        val habitsById = repository.getAllHabitsOnce().associateBy { it.id }
        val completions = repository.getAllCompletionsOnce()
            .sortedWith(compareBy({ it.habitId }, { it.date }))

        val builder = StringBuilder("habit,category,date,completed_at\n")
        completions.forEach { completion ->
            val habit = habitsById[completion.habitId]
            builder.append(csvField(habit?.name.orEmpty())).append(',')
                .append(csvField(habit?.category.orEmpty())).append(',')
                .append(completion.date).append(',')
                .append(completion.completedAt)
                .append('\n')
        }

        contentResolver.openOutputStream(uri)?.use { output ->
            output.write(builder.toString().toByteArray(Charsets.UTF_8))
        } ?: error("Could not open output stream for $uri")
    }

    private fun csvField(value: String): String = "\"${value.replace("\"", "\"\"")}\""

    suspend fun importFrom(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        try {
            val text = contentResolver.openInputStream(uri)?.use { input ->
                input.readBytes().toString(Charsets.UTF_8)
            } ?: return@withContext ImportResult.Failure("Could not open file")

            val payload = json.decodeFromString(ExportPayload.serializer(), text)
            val habits = payload.habits.map { it.toDomain() }
            val completions = payload.completions.map { it.toDomain() }
            repository.replaceAllData(habits, completions)
            ImportResult.Success(habits.size, completions.size)
        } catch (e: Exception) {
            ImportResult.Failure(e.message ?: "Unrecognized or corrupt file")
        }
    }
}
