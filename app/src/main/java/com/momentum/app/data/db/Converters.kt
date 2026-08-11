package com.momentum.app.data.db

import androidx.room.TypeConverter
import com.momentum.app.domain.model.HabitColor
import com.momentum.app.domain.model.HabitFrequency
import com.momentum.app.domain.model.HabitIcon

class Converters {

    @TypeConverter
    fun fromHabitFrequency(value: HabitFrequency): String = value.name

    @TypeConverter
    fun toHabitFrequency(value: String): HabitFrequency =
        runCatching { HabitFrequency.valueOf(value) }.getOrDefault(HabitFrequency.DAILY)

    @TypeConverter
    fun fromHabitColor(value: HabitColor): String = value.name

    @TypeConverter
    fun toHabitColor(value: String): HabitColor =
        runCatching { HabitColor.valueOf(value) }.getOrDefault(HabitColor.Default)

    @TypeConverter
    fun fromHabitIcon(value: HabitIcon): String = value.name

    @TypeConverter
    fun toHabitIcon(value: String): HabitIcon =
        runCatching { HabitIcon.valueOf(value) }.getOrDefault(HabitIcon.Default)
}
