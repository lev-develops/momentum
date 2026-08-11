package com.momentum.app.ui.insights

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.momentum.app.AppContainer
import com.momentum.app.domain.insights.InsightsSummary
import com.momentum.app.ui.components.EmptyState
import com.momentum.app.ui.theme.LocalMomentumColors
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(container: AppContainer) {
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
                message = "Complete a few habits and your patterns will show up here.",
                modifier = Modifier.fillMaxSize().padding(padding),
            )
        } else {
            InsightsContent(data, modifier = Modifier.fillMaxSize().padding(padding))
        }
    }
}

@Composable
private fun InsightsContent(data: InsightsSummary, modifier: Modifier = Modifier) {
    val colors = LocalMomentumColors.current
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        data.bestWeekday?.let {
            InsightRow(
                label = "Best day",
                value = it.dayOfWeek.displayName(),
                detail = "${(it.rate * 100).roundToInt()}% completion",
            )
            HorizontalDivider(color = colors.hairline)
        }
        data.worstWeekday?.let {
            InsightRow(
                label = "Toughest day",
                value = it.dayOfWeek.displayName(),
                detail = "${(it.rate * 100).roundToInt()}% completion",
            )
            HorizontalDivider(color = colors.hairline)
        }
        data.bestTimeOfDay?.let {
            InsightRow(label = "Best time of day", value = it.label)
            HorizontalDivider(color = colors.hairline)
        }
        if (data.longestStreakEver > 0) {
            InsightRow(
                label = "Longest streak ever",
                value = "${data.longestStreakEver} days",
                detail = data.longestStreakHabitName,
            )
            HorizontalDivider(color = colors.hairline)
        }
        InsightRow(
            label = "This month vs last",
            value = "${data.completionsThisMonth} vs ${data.completionsLastMonth}",
            detail = monthDeltaLabel(data.monthOverMonthDelta),
        )
        HorizontalDivider(color = colors.hairline)

        Text(
            text = "Getting 1% better each day compounds to over 37x in a year. Small, consistent completions matter more than perfect ones.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            modifier = Modifier.padding(vertical = 24.dp),
        )
    }
}

private fun monthDeltaLabel(delta: Int): String = when {
    delta > 0 -> "+$delta from last month"
    delta < 0 -> "$delta from last month"
    else -> "Same as last month"
}

private fun java.time.DayOfWeek.displayName(): String =
    getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault())

@Composable
private fun InsightRow(label: String, value: String, detail: String? = null) {
    val colors = LocalMomentumColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
        Column(horizontalAlignment = Alignment.End) {
            Text(text = value, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = colors.textPrimary)
            if (detail != null) {
                Text(text = detail, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
        }
    }
}
