package com.momentum.app.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.momentum.app.AppContainer
import com.momentum.app.ui.components.EmptyState
import com.momentum.app.ui.components.imageVector
import com.momentum.app.ui.components.rememberExportLauncher
import com.momentum.app.ui.components.rememberImportLauncher
import com.momentum.app.ui.theme.HabitPalette
import com.momentum.app.ui.theme.LocalMomentumColors
import kotlin.math.roundToInt

private val RowHeight = 72.dp

@Composable
fun TodayScreen(
    container: AppContainer,
    onHabitClick: (Long) -> Unit,
    onAddHabit: () -> Unit,
) {
    val viewModel: TodayViewModel = viewModel(
        factory = viewModelFactory { initializer { TodayViewModel(container) } },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalMomentumColors.current
    var menuExpanded by remember { mutableStateOf(false) }

    val exportLauncher = rememberExportLauncher(container)
    val importLauncher = rememberImportLauncher(container)

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.totalCount > 0) {
                            "${uiState.doneCount} of ${uiState.totalCount} done"
                        } else {
                            "Momentum"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                    )
                },
                actions = {
                    IconButton(onClick = onAddHabit) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add habit")
                    }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Export data") },
                            leadingIcon = { Icon(Icons.Rounded.FileUpload, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                exportLauncher()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Import data") },
                            leadingIcon = { Icon(Icons.Rounded.FileDownload, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                importLauncher()
                            },
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background,
                    titleContentColor = colors.textPrimary,
                ),
            )
        },
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding))
        } else if (uiState.habits.isEmpty()) {
            EmptyState(
                title = "No habits yet",
                message = "Add your first habit to start building momentum.",
                modifier = Modifier.fillMaxSize().padding(padding),
            )
        } else {
            ReorderableHabitList(
                items = uiState.habits,
                modifier = Modifier.fillMaxSize().padding(padding),
                onToggle = viewModel::toggle,
                onClick = onHabitClick,
                onReorder = viewModel::reorder,
            )
        }
    }
}

@Composable
private fun ReorderableHabitList(
    items: List<TodayHabitItem>,
    modifier: Modifier = Modifier,
    onToggle: (Long) -> Unit,
    onClick: (Long) -> Unit,
    onReorder: (List<Long>) -> Unit,
) {
    var orderedItems by remember { mutableStateOf(items) }
    LaunchedEffect(items) {
        orderedItems = items
    }

    val density = LocalDensity.current
    val rowHeightPx = with(density) { RowHeight.toPx() }
    val haptics = LocalHapticFeedback.current

    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }

    LazyColumn(modifier = modifier) {
        itemsIndexed(orderedItems, key = { _, item -> item.habit.id }) { index, item ->
            val isDragging = draggingIndex == index
            val colors = LocalMomentumColors.current

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(if (isDragging) 1f else 0f)
                    .offset { IntOffset(0, if (isDragging) dragOffset.roundToInt() else 0) }
                    .background(if (isDragging) colors.surface else colors.background)
                    .pointerInput(item.habit.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggingIndex = index
                                dragOffset = 0f
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDrag = { change, delta ->
                                change.consume()
                                val currentIndex = draggingIndex ?: return@detectDragGesturesAfterLongPress
                                dragOffset += delta.y
                                val moves = (dragOffset / rowHeightPx).roundToInt()
                                if (moves != 0) {
                                    val targetIndex = (currentIndex + moves).coerceIn(0, orderedItems.lastIndex)
                                    if (targetIndex != currentIndex) {
                                        val mutable = orderedItems.toMutableList()
                                        val moved = mutable.removeAt(currentIndex)
                                        mutable.add(targetIndex, moved)
                                        orderedItems = mutable
                                        dragOffset -= moves * rowHeightPx
                                        draggingIndex = targetIndex
                                    }
                                }
                            },
                            onDragEnd = {
                                draggingIndex = null
                                dragOffset = 0f
                                onReorder(orderedItems.map { it.habit.id })
                            },
                            onDragCancel = {
                                draggingIndex = null
                                dragOffset = 0f
                            },
                        )
                    },
            ) {
                TodayHabitRow(
                    item = item,
                    onToggle = { onToggle(item.habit.id) },
                    onClick = { onClick(item.habit.id) },
                )
                HorizontalDivider(color = colors.hairline)
            }
        }
    }
}

@Composable
private fun TodayHabitRow(
    item: TodayHabitItem,
    onToggle: () -> Unit,
    onClick: () -> Unit,
) {
    val colors = LocalMomentumColors.current
    val haptics = LocalHapticFeedback.current
    val accent = HabitPalette.accent(item.habit.colorKey, colors.isDark)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(RowHeight)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CompletionCircle(
            completed = item.completedToday,
            accent = accent,
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onToggle()
            },
        )
        Icon(
            imageVector = item.habit.iconKey.imageVector(),
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.habit.name,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textPrimary,
            )
            if (item.currentStreak > 0) {
                Text(
                    text = "${item.currentStreak} day streak",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun CompletionCircle(completed: Boolean, accent: Color, onClick: () -> Unit) {
    val colors = LocalMomentumColors.current
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .then(
                if (completed) {
                    Modifier.background(accent)
                } else {
                    Modifier
                        .background(colors.background)
                        .border(1.5.dp, colors.hairline, CircleShape)
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (completed) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Completed",
                tint = colors.background,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
