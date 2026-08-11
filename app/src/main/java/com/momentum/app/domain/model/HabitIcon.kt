package com.momentum.app.domain.model

/**
 * Identity only — mapped to an actual Material icon in ui/components/HabitIconography.kt.
 */
enum class HabitIcon {
    TARGET,
    BOOK,
    DUMBBELL,
    WATER_DROP,
    MEDITATION,
    SLEEP,
    RUN,
    FOOD,
    MONEY,
    PENCIL,
    MUSIC,
    SUN,
    HEART,
    CODE,
    PLANT,
    STAR,
    ;

    companion object {
        val Default = TARGET
    }
}
