package com.momentum.app.domain.model

/** A quick-start preset offered when creating a new habit — just default field values, nothing
 * persisted here. Picking one prefills Add/Edit; the user can still change anything before saving. */
data class HabitTemplate(
    val name: String,
    val icon: HabitIcon,
    val color: HabitColor,
    val frequency: HabitFrequency,
    val targetDaysPerWeek: Int,
    val category: String,
)

object HabitTemplates {
    val all: List<HabitTemplate> = listOf(
        HabitTemplate("Drink water", HabitIcon.WATER_DROP, HabitColor.TEAL, HabitFrequency.DAILY, 7, "Health"),
        HabitTemplate("Exercise", HabitIcon.DUMBBELL, HabitColor.ROSE, HabitFrequency.WEEKLY_TARGET, 3, "Fitness"),
        HabitTemplate("Read", HabitIcon.BOOK, HabitColor.INDIGO, HabitFrequency.DAILY, 7, "Learning"),
        HabitTemplate("Meditate", HabitIcon.MEDITATION, HabitColor.SLATE, HabitFrequency.DAILY, 7, "Mindfulness"),
        HabitTemplate("Sleep early", HabitIcon.SLEEP, HabitColor.INDIGO, HabitFrequency.DAILY, 7, "Health"),
        HabitTemplate("Run", HabitIcon.RUN, HabitColor.AMBER, HabitFrequency.WEEKLY_TARGET, 3, "Fitness"),
        HabitTemplate("Eat healthy", HabitIcon.FOOD, HabitColor.MOSS, HabitFrequency.DAILY, 7, "Health"),
        HabitTemplate("Save money", HabitIcon.MONEY, HabitColor.MOSS, HabitFrequency.WEEKLY_TARGET, 1, "Finance"),
        HabitTemplate("Journal", HabitIcon.PENCIL, HabitColor.AMBER, HabitFrequency.DAILY, 7, "Mindfulness"),
        HabitTemplate("Practice music", HabitIcon.MUSIC, HabitColor.ROSE, HabitFrequency.WEEKLY_TARGET, 4, "Hobbies"),
        HabitTemplate("Morning sunlight", HabitIcon.SUN, HabitColor.AMBER, HabitFrequency.DAILY, 7, "Health"),
        HabitTemplate("Gratitude", HabitIcon.HEART, HabitColor.ROSE, HabitFrequency.DAILY, 7, "Mindfulness"),
    )
}
