package com.momentum.app.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.momentum.app.AppContainer
import com.momentum.app.ui.components.EmptyState
import com.momentum.app.ui.components.PermissionsBanner
import com.momentum.app.ui.components.imageVector
import com.momentum.app.ui.components.rememberCsvExportLauncher
import com.momentum.app.ui.components.rememberExportLauncher
import com.momentum.app.ui.components.rememberImportLauncher
import com.momentum.app.ui.theme.HabitPalette
import com.momentum.app.ui.theme.LocalMomentumColors
import com.momentum.app.ui.theme.ThemePreference
import kotlin.math.roundToInt
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val RowHeight = 72.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    container: AppContainer,
    onHabitClick: (Long) -> Unit,
    onAddHabit: () -> Unit,
    onAccount: () -> Unit,
) {
    val viewModel: TodayViewModel = viewModel(
        factory = viewModelFactory { initializer { TodayViewModel(container) } },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalMomentumColors.current
    var menuExpanded by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val themePreference by container.appPrefsDataStore.themePreferenceFlow()
        .collectAsState(initial = ThemePreference.SYSTEM)

    val exportLauncher = rememberExportLauncher(container)
    val csvExportLauncher = rememberCsvExportLauncher(container)
    val importLauncher = rememberImportLauncher(container)

    val snackbarHostState = remember { SnackbarHostState() }
    val pendingDelete by container.pendingDeleteHolder.pending.collectAsStateWithLifecycle()
    LaunchedEffect(pendingDelete?.habit?.id) {
        val pending = pendingDelete ?: return@LaunchedEffect
        try {
            val result = snackbarHostState.showSnackbar(
                message = "\"${pending.habit.name}\" deleted",
                actionLabel = "Undo",
                withDismissAction = true,
            )
            when (result) {
                SnackbarResult.ActionPerformed -> container.pendingDeleteHolder.undo(pending.habit.id)
                SnackbarResult.Dismissed -> container.pendingDeleteHolder.commitIfPending(pending.habit.id)
            }
        } finally {
            // If this coroutine got cancelled instead (e.g. the user navigated off Today before
            // the snackbar resolved), still commit rather than leaving the delete stuck pending.
            withContext(NonCancellable) { container.pendingDeleteHolder.commitIfPending(pending.habit.id) }
        }
    }

    Scaffold(
        containerColor = colors.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                            text = { Text("Export as CSV") },
                            leadingIcon = { Icon(Icons.Rounded.FileUpload, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                csvExportLauncher()
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
                        DropdownMenuItem(
                            text = { Text("Cloud sync") },
                            leadingIcon = { Icon(Icons.Rounded.CloudSync, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onAccount()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Appearance") },
                            leadingIcon = { Icon(Icons.Rounded.DarkMode, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                showThemeDialog = true
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
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            PermissionsBanner(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            )
            if (uiState.categories.isNotEmpty()) {
                CategoryFilterRow(
                    categories = uiState.categories,
                    selected = uiState.selectedCategory,
                    onSelect = viewModel::setCategoryFilter,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (uiState.isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colors.textPrimary)
                    }
                } else if (uiState.habits.isEmpty()) {
                    EmptyState(
                        title = "No habits yet",
                        message = "Add your first habit to start building momentum — a few " +
                            "presets are ready to go, or start from scratch.",
                        modifier = Modifier.fillMaxSize(),
                        actionLabel = "Add a habit",
                        onAction = onAddHabit,
                    )
                } else {
                    ReorderableHabitList(
                        items = uiState.habits,
                        modifier = Modifier.fillMaxSize(),
                        onToggle = viewModel::toggle,
                        onClick = onHabitClick,
                        onReorder = viewModel::reorder,
                    )
                }
            }
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Appearance") },
            text = {
                Column {
                    ThemeOptionRow(
                        label = "System default",
                        selected = themePreference == ThemePreference.SYSTEM,
                        onClick = { scope.launch { container.appPrefsDataStore.setThemePreference(ThemePreference.SYSTEM) } },
                    )
                    ThemeOptionRow(
                        label = "Light",
                        selected = themePreference == ThemePreference.LIGHT,
                        onClick = { scope.launch { container.appPrefsDataStore.setThemePreference(ThemePreference.LIGHT) } },
                    )
                    ThemeOptionRow(
                        label = "Dark",
                        selected = themePreference == ThemePreference.DARK,
                        onClick = { scope.launch { container.appPrefsDataStore.setThemePreference(ThemePreference.DARK) } },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Done") }
            },
        )
    }
}

@Composable
private fun ThemeOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(text = label, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun CategoryFilterRow(
    categories: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMomentumColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CategoryChip(label = "All", isSelected = selected == null, onClick = { onSelect(null) })
        categories.forEach { category ->
            CategoryChip(label = category, isSelected = selected == category, onClick = { onSelect(category) })
        }
    }
}

@Composable
private fun CategoryChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val colors = LocalMomentumColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) colors.surface else colors.background)
            .border(1.dp, if (isSelected) colors.textPrimary else colors.hairline, RoundedCornerShape(20.dp))
            .selectable(selected = isSelected, onClick = onClick, role = Role.Tab)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = colors.textPrimary)
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
            habitName = item.habit.name,
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
private fun CompletionCircle(completed: Boolean, accent: Color, habitName: String, onClick: () -> Unit) {
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
            .toggleable(
                value = completed,
                onValueChange = { onClick() },
                role = Role.Checkbox,
            )
            .semantics { contentDescription = "$habitName, today" },
        contentAlignment = Alignment.Center,
    ) {
        if (completed) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = colors.background,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
