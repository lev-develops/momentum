package com.momentum.app.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.momentum.app.AppContainer
import com.momentum.app.domain.insights.CategoryStat
import com.momentum.app.domain.insights.InsightsSummary
import com.momentum.app.domain.insights.WeekdayStat
import com.momentum.app.ui.components.EmptyState
import com.momentum.app.ui.theme.LocalMomentumColors
import com.momentum.app.ui.theme.MomentumColors
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(container: AppContainer, onAddHabit: () -> Unit) {
    val viewModel: InsightsViewModel = viewModel(
        factory = viewModelFactory { initializer { InsightsViewModel(container) } },
    )
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val colors = LocalMomentumColors.current

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("Insights") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background,
                    titleContentColor = colors.textPrimary,
                ),
            )
        },
    ) { padding ->
        val data = summary
        if (data == null || !data.hasData) {
            EmptyState(
                title = "Nothing to show yet",
                message = "Complete a few habits and your patterns — best days, streaks, and " +
                    "more — will show up here.",
                modifier = Modifier.fillMaxSize().padding(padding),
                actionLabel = "Add a habit",
                onAction = onAddHabit,
            )
        } else {
            InsightsContent(data, modifier = Modifier.fillMaxSize().padding(padding))
        }
    }
}

@Composable
private fun InsightsContent(data: InsightsSummary, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OverviewCard(data)
        WeeklyPatternCard(data)
        HighlightsCard(data)
        CategoryBreakdownCard(data)
        MonthComparisonCard(data)
        TipCard()
    }
}

@Composable
private fun InsightsCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalMomentumColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .padding(20.dp),
        content = content,
    )
}

@Composable
private fun CardTitle(text: String) {
    val colors = LocalMomentumColors.current
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = colors.textSecondary,
        modifier = Modifier.padding(bottom = 16.dp),
    )
}

@Composable
private fun OverviewCard(data: InsightsSummary) {
    InsightsCard {
        CardTitle("Overview")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StatTile(value = "${data.activeHabitCount}", label = "Habits")
            StatTile(value = "${(data.overallCompletionRate * 100).roundToInt()}%", label = "Avg. completion")
            StatTile(value = "${data.longestStreakEver}", label = "Best streak")
        }
    }
}

@Composable
private fun StatTile(value: String, label: String) {
    val colors = LocalMomentumColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
    }
}

@Composable
private fun WeeklyPatternCard(data: InsightsSummary) {
    val colors = LocalMomentumColors.current
    val maxRate = (data.weekdayBreakdown.maxOfOrNull { it.rate } ?: 0f).coerceAtLeast(0.01f)
    val barMaxHeight = 96.dp

    InsightsCard {
        CardTitle("Weekly pattern")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            data.weekdayBreakdown.forEach { stat ->
                WeekdayBar(stat = stat, maxRate = maxRate, maxHeight = barMaxHeight, colors = colors)
            }
        }
    }
}

@Composable
private fun WeekdayBar(stat: WeekdayStat, maxRate: Float, maxHeight: Dp, colors: MomentumColors) {
    val fraction = (stat.rate / maxRate).coerceIn(0f, 1f)
    val barHeight = (maxHeight * fraction).coerceAtLeast(4.dp)
    val fullDayName = stat.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(32.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "$fullDayName, ${(stat.rate * 100).roundToInt()} percent completion"
            },
    ) {
        Box(modifier = Modifier.height(maxHeight), contentAlignment = Alignment.BottomCenter) {
            Box(
                modifier = Modifier
                    .height(barHeight)
                    .width(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (stat.completions > 0) colors.textPrimary else colors.gridEmpty),
            )
        }
        Text(
            text = stat.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun HighlightsCard(data: InsightsSummary) {
    if (data.bestWeekday == null && data.worstWeekday == null && data.bestTimeOfDay == null && data.longestStreakEver <= 0) return
    val colors = LocalMomentumColors.current
    InsightsCard {
        CardTitle("Highlights")
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            data.bestWeekday?.let {
                HighlightRow(label = "Best day", value = it.dayOfWeek.displayName(), detail = "${(it.rate * 100).roundToInt()}% completion")
            }
            data.worstWeekday?.let {
                HighlightRow(label = "Toughest day", value = it.dayOfWeek.displayName(), detail = "${(it.rate * 100).roundToInt()}% completion")
            }
            data.bestTimeOfDay?.let {
                HighlightRow(label = "Best time of day", value = it.label)
            }
            if (data.longestStreakEver > 0) {
                HighlightRow(label = "Longest streak ever", value = "${data.longestStreakEver} days", detail = data.longestStreakHabitName)
            }
        }
    }
}

@Composable
private fun HighlightRow(label: String, value: String, detail: String? = null) {
    val colors = LocalMomentumColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        Column(horizontalAlignment = Alignment.End) {
            Text(text = value, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = colors.textPrimary)
            if (detail != null) {
                Text(text = detail, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun CategoryBreakdownCard(data: InsightsSummary) {
    if (data.categoryBreakdown.isEmpty()) return
    val colors = LocalMomentumColors.current
    InsightsCard {
        CardTitle("By category")
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            data.categoryBreakdown.forEach { stat -> CategoryRow(stat, colors) }
        }
    }
}

@Composable
private fun CategoryRow(stat: CategoryStat, colors: MomentumColors) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "${stat.category} (${stat.habitCount})",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary,
            )
            Text(
                text = "${(stat.completionRate * 100).roundToInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(colors.gridEmpty),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(stat.completionRate.coerceIn(0.02f, 1f))
                    .background(colors.textPrimary),
            )
        }
    }
}

@Composable
private fun MonthComparisonCard(data: InsightsSummary) {
    val colors = LocalMomentumColors.current
    val total = (data.completionsThisMonth + data.completionsLastMonth).coerceAtLeast(1)
    val thisMonthFraction = data.completionsThisMonth.toFloat() / total

    InsightsCard {
        CardTitle("This month vs. last")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(colors.gridEmpty),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(thisMonthFraction.coerceIn(0.02f, 1f))
                    .background(colors.textPrimary),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "${data.completionsThisMonth} this month", style = MaterialTheme.typography.bodySmall, color = colors.textPrimary)
            Text(text = "${data.completionsLastMonth} last month", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
        Text(
            text = monthDeltaLabel(data.monthOverMonthDelta),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun TipCard() {
    val colors = LocalMomentumColors.current
    InsightsCard {
        Text(
            text = "Getting 1% better each day compounds to over 37x in a year. Small, consistent completions matter more than perfect ones.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
    }
}

private fun monthDeltaLabel(delta: Int): String = when {
    delta > 0 -> "+$delta from last month"
    delta < 0 -> "$delta from last month"
    else -> "Same as last month"
}

private fun DayOfWeek.displayName(): String = getDisplayName(TextStyle.FULL, Locale.getDefault())
