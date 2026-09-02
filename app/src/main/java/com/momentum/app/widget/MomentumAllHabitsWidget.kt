package com.momentum.app.widget

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.momentum.app.MomentumApplication
import com.momentum.app.domain.model.Habit
import com.momentum.app.ui.theme.HabitPalette
import com.momentum.app.ui.theme.NeutralTokens
import java.time.LocalDate

private val HabitIdKey = ActionParameters.Key<Long>("habitId")

private fun isSystemInDarkTheme(context: Context): Boolean {
    val nightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return nightMode == Configuration.UI_MODE_NIGHT_YES
}

/** Compact "all active habits today" widget — an alternative to [MomentumWidget]'s single-habit
 * contribution grid, for anyone who wants every habit's checkbox in one place. No configuration
 * step: it always shows every active habit, so there's nothing to bind per widget instance. */
class MomentumAllHabitsWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = (context.applicationContext as MomentumApplication).container
        val isDark = isSystemInDarkTheme(context)
        val today = LocalDate.now(container.clock)

        val habits = container.habitRepository.getAllHabitsOnce().filter { !it.archived }
        val completedTodayIds = container.habitRepository.getAllCompletionsOnce()
            .filter { it.date == today }
            .map { it.habitId }
            .toSet()

        provideContent {
            AllHabitsContent(habits = habits, completedTodayIds = completedTodayIds, isDark = isDark)
        }
    }

    companion object {
        suspend fun refreshAll(context: Context) {
            MomentumAllHabitsWidget().updateAll(context)
        }
    }
}

@Composable
private fun AllHabitsContent(habits: List<Habit>, completedTodayIds: Set<Long>, isDark: Boolean) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(NeutralTokens.surfaceArgb(isDark)))
            .padding(12.dp),
    ) {
        Text(
            text = "Today",
            style = TextStyle(
                color = ColorProvider(Color(NeutralTokens.textPrimaryArgb(isDark))),
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
            ),
            modifier = GlanceModifier.padding(bottom = 10.dp),
        )
        if (habits.isEmpty()) {
            Text(
                text = "No habits yet",
                style = TextStyle(color = ColorProvider(Color(NeutralTokens.textSecondaryArgb(isDark)))),
            )
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(habits, itemId = { it.id }) { habit ->
                    HabitRow(habit = habit, completed = habit.id in completedTodayIds, isDark = isDark)
                }
            }
        }
    }
}

@Composable
private fun HabitRow(habit: Habit, completed: Boolean, isDark: Boolean) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(actionRunCallback<ToggleAllHabitsAction>(actionParametersOf(HabitIdKey to habit.id))),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val accent = Color(HabitPalette.levelArgb(habit.colorKey, 4, isDark))
        val boxColor = if (completed) accent else Color(NeutralTokens.hairlineArgb(isDark))
        Box(modifier = GlanceModifier.size(22.dp).background(boxColor)) {}
        Text(
            text = habit.name,
            style = TextStyle(
                color = ColorProvider(Color(NeutralTokens.textPrimaryArgb(isDark))),
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
            ),
            modifier = GlanceModifier.padding(start = 12.dp),
        )
    }
}

class ToggleAllHabitsAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val habitId = parameters[HabitIdKey] ?: return
        val container = (context.applicationContext as MomentumApplication).container
        val today = LocalDate.now(container.clock)
        container.habitRepository.toggleCompletion(habitId, today)
        MomentumAllHabitsWidget().update(context, glanceId)
    }
}
