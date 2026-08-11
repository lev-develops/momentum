package com.momentum.app.domain.model

/**
 * Identity only — the actual hex ramps live in ui/theme/Theme.kt, the single
 * source of truth for color. This enum is just the persisted key.
 */
enum class HabitColor {
    MOSS,
    INDIGO,
    AMBER,
    ROSE,
    TEAL,
    SLATE,
    ;

    companion object {
        val Default = MOSS
    }
}
