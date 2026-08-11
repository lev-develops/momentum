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
