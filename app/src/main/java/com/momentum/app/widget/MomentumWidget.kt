package com.momentum.app.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.momentum.app.MomentumApplication
import com.momentum.app.domain.model.Habit
import com.momentum.app.domain.streak.ContributionGridBuilder
import com.momentum.app.domain.streak.StreakCalculator
import com.momentum.app.ui.theme.HabitPalette
import com.momentum.app.ui.theme.NeutralTokens
import java.time.LocalDate
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.first

// Bigger, rounder cells and a bolder header — chunky "pixel-grid" look (HabitKit-style) rather
// than the small hairline grid this started as.
private val CellDp = 11.dp
private val GapDp = 3.dp
private val CornerRadiusDp = 4.dp
private val WidgetPaddingDp = 14.dp
private val HeaderHeightDp = 40.dp
private val BadgeSizeDp = 32.dp

private fun isSystemInDarkTheme(context: Context): Boolean {
    val nightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return nightMode == Configuration.UI_MODE_NIGHT_YES
}

class MomentumWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val container = (context.applicationContext as MomentumApplication).container

        val habitId = container.widgetPrefsDataStore.getHabitId(appWidgetId)
        val habit = habitId?.let { container.habitRepository.getHabit(it) }

        val isDark = isSystemInDarkTheme(context)

        if (habit == null) {
            provideContent { EmptyStateContent(appWidgetId, isDark) }
            return
        }

        val today = LocalDate.now(container.clock)
        val dates = container.habitRepository.getAllCompletionsOnce()
            .filter { it.habitId == habit.id }
            .map { it.date }
            .toSet()
        val frozenDates = container.habitRepository.observeFrozenDates(habit.id).first()
        val streakDates = dates + frozenDates

        provideContent {
            HabitGridContent(habit = habit, completedDates = streakDates, today = today, appWidgetId = appWidgetId, isDark = isDark)
        }
    }

    companion object {
        suspend fun refreshAll(context: Context) {
            MomentumWidget().updateAll(context)
        }
    }
}

@Composable
private fun EmptyStateContent(appWidgetId: Int, isDark: Boolean) {
    val context = LocalContext.current
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(NeutralTokens.surfaceArgb(isDark)))
            .clickable(actionStartActivity(configureIntent(context, appWidgetId))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Tap to choose a habit",
            style = TextStyle(
                color = ColorProvider(Color(NeutralTokens.textSecondaryArgb(isDark))),
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

@Composable
private fun HabitGridContent(
    habit: Habit,
    completedDates: Set<LocalDate>,
    today: LocalDate,
    appWidgetId: Int,
    isDark: Boolean,
) {
    val context = LocalContext.current
    val size = LocalSize.current
    val density = context.resources.displayMetrics.density

    val streak = StreakCalculator.currentStreak(completedDates, habit.frequency, habit.targetDaysPerWeek, today)
    val grid = ContributionGridBuilder.build(completedDates, habit.targetDaysPerWeek, today)

    val availableWidthDp = (size.width.value - WidgetPaddingDp.value * 2).coerceAtLeast(0f)
    val pitchDp = CellDp.value + GapDp.value
    val visibleColumns = floor((availableWidthDp + GapDp.value) / pitchDp).toInt().coerceIn(1, grid.weeks)

    val gridWidthPx = (visibleColumns * pitchDp - GapDp.value).coerceAtLeast(CellDp.value) * density
    val gridHeightPx = (7 * pitchDp - GapDp.value) * density

    val bitmap = WidgetGridRenderer.render(
        grid = grid,
        colorKey = habit.colorKey,
        isDark = isDark,
        widthPx = gridWidthPx.roundToInt(),
        heightPx = gridHeightPx.roundToInt(),
        visibleColumns = visibleColumns,
        cellPx = CellDp.value * density,
        gapPx = GapDp.value * density,
        cornerRadiusPx = CornerRadiusDp.value * density,
    )

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(NeutralTokens.surfaceArgb(isDark)))
            .padding(WidgetPaddingDp),
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(HeaderHeightDp)
                .clickable(actionStartActivity(configureIntent(context, appWidgetId))),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val accent = Color(HabitPalette.levelArgb(habit.colorKey, 4, isDark))
            Box(modifier = GlanceModifier.size(BadgeSizeDp).background(accent)) {}
            Column(modifier = GlanceModifier.defaultWeight().padding(start = 10.dp)) {
                Text(
                    text = habit.name,
                    style = TextStyle(
                        color = ColorProvider(Color(NeutralTokens.textPrimaryArgb(isDark))),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    ),
                )
                Text(
                    text = "$streak day streak",
                    style = TextStyle(color = ColorProvider(accent), fontWeight = FontWeight.Medium, fontSize = 12.sp),
                )
            }
        }
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .clickable(actionRunCallback<ToggleTodayAction>()),
        ) {
            Image(
                provider = ImageProvider(bitmap),
                contentDescription = habit.name,
                modifier = GlanceModifier.size(
                    width = (gridWidthPx / density).dp,
                    height = (gridHeightPx / density).dp,
                ),
            )
        }
    }
}

private fun configureIntent(context: Context, appWidgetId: Int): Intent =
    Intent(context, WidgetConfigActivity::class.java).apply {
        putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    }

class ToggleTodayAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        val container = (context.applicationContext as MomentumApplication).container
        val habitId = container.widgetPrefsDataStore.getHabitId(appWidgetId) ?: return
        // The bound habit may have been deleted since this widget last redrew (completions
        // cascade-delete with the habit, so inserting against a stale id would otherwise throw
        // a foreign-key constraint violation here). Bail and let the redraw below fall back to
        // the empty-state content instead.
        if (container.habitRepository.getHabit(habitId) == null) {
            container.widgetPrefsDataStore.clear(appWidgetId)
            MomentumWidget().update(context, glanceId)
            return
        }
        val today = LocalDate.now(container.clock)
        container.habitRepository.toggleCompletion(habitId, today)
        MomentumWidget().update(context, glanceId)
    }
}
