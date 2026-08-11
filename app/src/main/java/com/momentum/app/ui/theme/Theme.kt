package com.momentum.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.momentum.app.domain.model.HabitColor
import kotlin.math.roundToInt

/**
 * ============================================================================================
 * SINGLE SOURCE OF TRUTH FOR COLOR.
 *
 * Every color used anywhere in the app — Compose UI, the Glance widget's bitmap-rendered grid,
 * notification-adjacent chrome — is derived from the hex literals in this file. Nothing else in
 * the codebase should contain a hardcoded color hex; import [MomentumColors] (via
 * [LocalMomentumColors]) or [HabitPalette] instead.
 *
 * No Material You / dynamic color: the palette is fixed so the app looks identical everywhere.
 * ============================================================================================
 */

// ---- Neutral base ----

private object NeutralLight {
    const val BACKGROUND = 0xFFFFFFFF
    const val SURFACE = 0xFFF7F7F5
    const val GRID_EMPTY = 0xFFEDEDEA
    const val HAIRLINE = 0xFFE5E5E3
    const val TEXT_PRIMARY = 0xFF1C1C1E
    const val TEXT_SECONDARY = 0xFF6E6E73
}

private object NeutralDark {
    const val BACKGROUND = 0xFF0D0D0F
    const val SURFACE = 0xFF1C1C1E
    const val GRID_EMPTY = 0xFF242427
    const val HAIRLINE = 0xFF2C2C2E
    const val TEXT_PRIMARY = 0xFFF5F5F7
    const val TEXT_SECONDARY = 0xFF98989D
}

/** Neutral chrome tokens for the current theme. Accent color never appears here. */
data class MomentumColors(
    val background: Color,
    val surface: Color,
    val gridEmpty: Color,
    val hairline: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val isDark: Boolean,
)

private val LightMomentumColors = MomentumColors(
    background = Color(NeutralLight.BACKGROUND),
    surface = Color(NeutralLight.SURFACE),
    gridEmpty = Color(NeutralLight.GRID_EMPTY),
    hairline = Color(NeutralLight.HAIRLINE),
    textPrimary = Color(NeutralLight.TEXT_PRIMARY),
    textSecondary = Color(NeutralLight.TEXT_SECONDARY),
    isDark = false,
)

private val DarkMomentumColors = MomentumColors(
    background = Color(NeutralDark.BACKGROUND),
    surface = Color(NeutralDark.SURFACE),
    gridEmpty = Color(NeutralDark.GRID_EMPTY),
    hairline = Color(NeutralDark.HAIRLINE),
    textPrimary = Color(NeutralDark.TEXT_PRIMARY),
    textSecondary = Color(NeutralDark.TEXT_SECONDARY),
    isDark = true,
)

val LocalMomentumColors = staticCompositionLocalOf { LightMomentumColors }

/**
 * ARGB Int accessors for the neutral tokens, for non-Compose consumers (the Glance widget's
 * bitmap renderer and its plain-View header) that can't read [LocalMomentumColors].
 */
object NeutralTokens {
    fun backgroundArgb(isDark: Boolean): Int = (if (isDark) NeutralDark.BACKGROUND else NeutralLight.BACKGROUND).toInt()
    fun surfaceArgb(isDark: Boolean): Int = (if (isDark) NeutralDark.SURFACE else NeutralLight.SURFACE).toInt()
    fun hairlineArgb(isDark: Boolean): Int = (if (isDark) NeutralDark.HAIRLINE else NeutralLight.HAIRLINE).toInt()
    fun textPrimaryArgb(isDark: Boolean): Int = (if (isDark) NeutralDark.TEXT_PRIMARY else NeutralLight.TEXT_PRIMARY).toInt()
    fun textSecondaryArgb(isDark: Boolean): Int = (if (isDark) NeutralDark.TEXT_SECONDARY else NeutralLight.TEXT_SECONDARY).toInt()
}

// ---- Habit accent ramps ----

/**
 * Four intensity levels per habit color, indexed 0..3 == L1..L4 (L1 = minimum completion,
 * L4 = target met or exceeded). These are the ONLY light-mode hex values that exist for accent
 * color; dark mode is always computed from them, never redefined.
 */
private val sourceRamps: Map<HabitColor, LongArray> = mapOf(
    HabitColor.MOSS to longArrayOf(0xFFD9EFD3, 0xFFA6D8A0, 0xFF6BB86A, 0xFF3E8E45),
    HabitColor.INDIGO to longArrayOf(0xFFDDE0F7, 0xFFADB3EC, 0xFF7A82D8, 0xFF4C52A8),
    HabitColor.AMBER to longArrayOf(0xFFF9E7C6, 0xFFF0C97F, 0xFFDDA13A, 0xFFA8701C),
    HabitColor.ROSE to longArrayOf(0xFFF8DDE4, 0xFFEFB0C0, 0xFFDE7E97, 0xFFA94E68),
    HabitColor.TEAL to longArrayOf(0xFFD3EDE7, 0xFF96D6C6, 0xFF4FB79E, 0xFF22806C),
    HabitColor.SLATE to longArrayOf(0xFFE3E3E0, 0xFFBFBFBA, 0xFF8A8A84, 0xFF4A4A46),
)

/** Fraction each dark-mode ramp level is shifted toward the dark background, per the spec (~15%). */
private const val DARK_SHIFT_FRACTION = 0.15f

object HabitPalette {

    /** [level] is 1..4. Returns an ARGB Int so both Compose (`Color(Int)`) and `android.graphics`
     * (`Paint.setColor(Int)`, used by the widget's bitmap renderer) can consume it directly. */
    fun levelArgb(colorKey: HabitColor, level: Int, isDark: Boolean): Int {
        val clamped = level.coerceIn(1, 4)
        val sourceArgb = (sourceRamps[colorKey] ?: sourceRamps.getValue(HabitColor.Default))[clamped - 1]
        return if (isDark) {
            shiftToward(sourceArgb, NeutralDark.BACKGROUND, DARK_SHIFT_FRACTION)
        } else {
            sourceArgb.toInt()
        }
    }

    /** Level 0 (no completion) — the neutral empty-cell token, not an accent color. */
    fun emptyArgb(isDark: Boolean): Int = if (isDark) NeutralDark.GRID_EMPTY.toInt() else NeutralLight.GRID_EMPTY.toInt()

    fun levelColor(colorKey: HabitColor, level: Int, isDark: Boolean): Color =
        Color(levelArgb(colorKey, level, isDark))

    fun cellColor(colorKey: HabitColor, level: Int, isDark: Boolean): Color =
        if (level <= 0) Color(emptyArgb(isDark)) else levelColor(colorKey, level, isDark)

    fun cellArgb(colorKey: HabitColor, level: Int, isDark: Boolean): Int =
        if (level <= 0) emptyArgb(isDark) else levelArgb(colorKey, level, isDark)

    /** The habit's most saturated tone — used for the completed-state checkmark, the only other
     * place accent color is allowed to appear. */
    fun accent(colorKey: HabitColor, isDark: Boolean): Color = levelColor(colorKey, level = 4, isDark = isDark)

    private fun shiftToward(sourceArgb: Long, targetArgb: Long, fraction: Float): Int {
        val sr = (sourceArgb shr 16 and 0xFF).toInt()
        val sg = (sourceArgb shr 8 and 0xFF).toInt()
        val sb = (sourceArgb and 0xFF).toInt()
        val tr = (targetArgb shr 16 and 0xFF).toInt()
        val tg = (targetArgb shr 8 and 0xFF).toInt()
        val tb = (targetArgb and 0xFF).toInt()
        val r = lerpChannel(sr, tr, fraction)
        val g = lerpChannel(sg, tg, fraction)
        val b = lerpChannel(sb, tb, fraction)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun lerpChannel(from: Int, to: Int, fraction: Float): Int =
        (from + (to - from) * fraction).roundToInt().coerceIn(0, 255)
}

// ---- Material3 bridge: fully neutral, accent color never leaks into chrome ----

/**
 * Every M3 color role is pinned to a neutral token — not just the handful this app's own
 * composables read directly — because stock M3 components (TimePicker, Switch, Snackbar, …)
 * reach for roles like primaryContainer/tertiary by default. Leaving any role unset would let
 * Material's default purple leak through in exactly the places the spec forbids accent color.
 */
private fun neutralColorScheme(colors: MomentumColors, isDark: Boolean) = if (isDark) {
    darkColorScheme(
        primary = colors.textPrimary,
        onPrimary = colors.background,
        primaryContainer = colors.surface,
        onPrimaryContainer = colors.textPrimary,
        inversePrimary = colors.background,
        secondary = colors.textSecondary,
        onSecondary = colors.background,
        secondaryContainer = colors.surface,
        onSecondaryContainer = colors.textPrimary,
        tertiary = colors.textPrimary,
        onTertiary = colors.background,
        tertiaryContainer = colors.surface,
        onTertiaryContainer = colors.textPrimary,
        background = colors.background,
        onBackground = colors.textPrimary,
        surface = colors.surface,
        onSurface = colors.textPrimary,
        surfaceVariant = colors.gridEmpty,
        onSurfaceVariant = colors.textSecondary,
        surfaceTint = colors.surface,
        inverseSurface = colors.textPrimary,
        inverseOnSurface = colors.background,
        error = colors.textPrimary,
        onError = colors.background,
        errorContainer = colors.surface,
        onErrorContainer = colors.textPrimary,
        outline = colors.hairline,
        outlineVariant = colors.hairline,
        scrim = colors.textPrimary,
    )
} else {
    lightColorScheme(
        primary = colors.textPrimary,
        onPrimary = colors.background,
        primaryContainer = colors.surface,
        onPrimaryContainer = colors.textPrimary,
        inversePrimary = colors.background,
        secondary = colors.textSecondary,
        onSecondary = colors.background,
        secondaryContainer = colors.surface,
        onSecondaryContainer = colors.textPrimary,
        tertiary = colors.textPrimary,
        onTertiary = colors.background,
        tertiaryContainer = colors.surface,
        onTertiaryContainer = colors.textPrimary,
        background = colors.background,
        onBackground = colors.textPrimary,
        surface = colors.surface,
        onSurface = colors.textPrimary,
        surfaceVariant = colors.gridEmpty,
        onSurfaceVariant = colors.textSecondary,
        surfaceTint = colors.surface,
        inverseSurface = colors.textPrimary,
        inverseOnSurface = colors.background,
        error = colors.textPrimary,
        onError = colors.background,
        errorContainer = colors.surface,
        onErrorContainer = colors.textPrimary,
        outline = colors.hairline,
        outlineVariant = colors.hairline,
        scrim = colors.textPrimary,
    )
}

private val LightColorScheme = neutralColorScheme(LightMomentumColors, isDark = false)
private val DarkColorScheme = neutralColorScheme(DarkMomentumColors, isDark = true)

@Composable
fun MomentumTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val momentumColors = if (darkTheme) DarkMomentumColors else LightMomentumColors
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalMomentumColors provides momentumColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MomentumTypography,
            shapes = MomentumShapes,
            content = content,
        )
    }
}
