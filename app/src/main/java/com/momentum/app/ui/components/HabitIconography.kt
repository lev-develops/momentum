package com.momentum.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import com.momentum.app.domain.model.HabitColor
import com.momentum.app.domain.model.HabitIcon

fun HabitIcon.imageVector(): ImageVector = when (this) {
    HabitIcon.TARGET -> Icons.Rounded.TrackChanges
    HabitIcon.BOOK -> Icons.Rounded.MenuBook
    HabitIcon.DUMBBELL -> Icons.Rounded.FitnessCenter
    HabitIcon.WATER_DROP -> Icons.Rounded.WaterDrop
    HabitIcon.MEDITATION -> Icons.Rounded.SelfImprovement
    HabitIcon.SLEEP -> Icons.Rounded.Bedtime
    HabitIcon.RUN -> Icons.Rounded.DirectionsRun
    HabitIcon.FOOD -> Icons.Rounded.Restaurant
    HabitIcon.MONEY -> Icons.Rounded.Savings
    HabitIcon.PENCIL -> Icons.Rounded.Edit
    HabitIcon.MUSIC -> Icons.Rounded.MusicNote
    HabitIcon.SUN -> Icons.Rounded.WbSunny
    HabitIcon.HEART -> Icons.Rounded.Favorite
    HabitIcon.CODE -> Icons.Rounded.Code
    HabitIcon.PLANT -> Icons.Rounded.Eco
    HabitIcon.STAR -> Icons.Rounded.Star
}

/** Human-readable label, used as an accessibility [contentDescription] where the icon is the
 * only cue (e.g. the icon picker) rather than decoration next to visible text. */
fun HabitIcon.displayName(): String = when (this) {
    HabitIcon.TARGET -> "Target"
    HabitIcon.BOOK -> "Book"
    HabitIcon.DUMBBELL -> "Dumbbell"
    HabitIcon.WATER_DROP -> "Water drop"
    HabitIcon.MEDITATION -> "Meditation"
    HabitIcon.SLEEP -> "Sleep"
    HabitIcon.RUN -> "Run"
    HabitIcon.FOOD -> "Food"
    HabitIcon.MONEY -> "Money"
    HabitIcon.PENCIL -> "Pencil"
    HabitIcon.MUSIC -> "Music"
    HabitIcon.SUN -> "Sun"
    HabitIcon.HEART -> "Heart"
    HabitIcon.CODE -> "Code"
    HabitIcon.PLANT -> "Plant"
    HabitIcon.STAR -> "Star"
}

/** Human-readable label for a habit's accent color, used as an accessibility [contentDescription]
 * on color swatches where color alone would otherwise be the only cue. */
fun HabitColor.displayName(): String = when (this) {
    HabitColor.MOSS -> "Moss green"
    HabitColor.INDIGO -> "Indigo"
    HabitColor.AMBER -> "Amber"
    HabitColor.ROSE -> "Rose"
    HabitColor.TEAL -> "Teal"
    HabitColor.SLATE -> "Slate gray"
}
