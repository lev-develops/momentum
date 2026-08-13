package com.momentum.app.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.momentum.app.domain.model.Habit
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Schedules per-habit daily reminder notifications and the local-midnight rollover alarm with
 * [AlarmManager]. Alarms are one-shot; each firing reschedules its own next occurrence (exact
 * repeating alarms are not reliable across API levels). Falls back to an inexact alarm if exact
 * alarms aren't permitted (API 31+ requires user-granted SCHEDULE_EXACT_ALARM).
 */
class ReminderScheduler(private val context: Context, private val clock: Clock = Clock.systemDefaultZone()) {

    private val alarmManager: AlarmManager
        get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private val zone: ZoneId get() = clock.zone

    fun scheduleReminder(habit: Habit) {
        val time = habit.reminderTime ?: return
        val triggerAt = nextOccurrence(time)
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ACTION_REMINDER
            putExtra(EXTRA_HABIT_ID, habit.id)
            putExtra(EXTRA_HABIT_NAME, habit.name)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderRequestCode(habit.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        scheduleExactOrInexact(triggerAt, pendingIntent)
    }

    fun cancelReminder(habitId: Long) {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply { action = ACTION_REMINDER }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderRequestCode(habitId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    fun rescheduleAll(habits: List<Habit>) {
        habits.filter { !it.archived && it.reminderTime != null }.forEach { scheduleReminder(it) }
        scheduleMidnightRollover()
        scheduleWeeklySummary()
    }

    /** Arms (or re-arms) the next Sunday-evening week-in-review notification. */
    fun scheduleWeeklySummary() {
        val intent = Intent(context, WeeklySummaryReceiver::class.java).apply { action = ACTION_WEEKLY_SUMMARY }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            WEEKLY_SUMMARY_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        scheduleExactOrInexact(nextWeeklySummaryOccurrence(), pendingIntent)
    }

    private fun nextWeeklySummaryOccurrence(): Long {
        val now = LocalDateTime.now(clock)
        var candidateDate = now.toLocalDate()
        while (candidateDate.dayOfWeek != DayOfWeek.SUNDAY) candidateDate = candidateDate.plusDays(1)
        var candidate = candidateDate.atTime(WEEKLY_SUMMARY_HOUR, 0)
        if (!candidate.isAfter(now)) candidate = candidate.plusDays(7)
        return candidate.atZone(zone).toInstant().toEpochMilli()
    }

    fun scheduleMidnightRollover() {
        val today = LocalDateTime.now(clock).toLocalDate()
        val nextMidnight = today.plusDays(1).atStartOfDay(zone)
        val intent = Intent(context, MidnightRolloverReceiver::class.java).apply {
            action = ACTION_MIDNIGHT
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            MIDNIGHT_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        scheduleExactOrInexact(nextMidnight.toInstant().toEpochMilli(), pendingIntent)
    }

    private fun scheduleExactOrInexact(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        val canScheduleExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()
        if (canScheduleExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun nextOccurrence(time: LocalTime): Long {
        val now = LocalDateTime.now(clock)
        var candidate = now.toLocalDate().atTime(time)
        if (!candidate.isAfter(now)) candidate = candidate.plusDays(1)
        return candidate.atZone(zone).toInstant().toEpochMilli()
    }

    /**
     * Folds the full 64-bit habit id into the request code instead of truncating it, so ids
     * that differ only in their high bits (anything a plain `.toInt()` cast would collide on)
     * still land on distinct request codes and don't stomp each other's [PendingIntent].
     */
    private fun reminderRequestCode(habitId: Long): Int =
        REMINDER_REQUEST_CODE_BASE + (habitId xor (habitId ushr 32)).toInt()

    companion object {
        const val ACTION_REMINDER = "com.momentum.app.action.HABIT_REMINDER"
        const val ACTION_MIDNIGHT = "com.momentum.app.action.MIDNIGHT_ROLLOVER"
        const val ACTION_WEEKLY_SUMMARY = "com.momentum.app.action.WEEKLY_SUMMARY"
        const val EXTRA_HABIT_ID = "habit_id"
        const val EXTRA_HABIT_NAME = "habit_name"
        private const val REMINDER_REQUEST_CODE_BASE = 10_000
        private const val MIDNIGHT_REQUEST_CODE = 99_999
        private const val WEEKLY_SUMMARY_REQUEST_CODE = 88_888
        private const val WEEKLY_SUMMARY_HOUR = 18
    }
}
