@file:OptIn(ExperimentalTextApi::class)

package com.example.multiminuteurs

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/* Same recipe as Topé: Material 3 Expressive colors computed from a single hue. */

fun hsl(h: Float, s: Float, l: Float, a: Float = 1f): Color =
    Color.hsl(((h % 360f) + 360f) % 360f, s / 100f, l / 100f, a)

@Immutable
data class Pal(
    val dark: Boolean,
    val amoled: Boolean,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val surface: Color,
    val surfaceLow: Color,
    val surfaceC: Color,
    val surfaceHigh: Color,
    val surfaceHighest: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val outlineVariant: Color,
    val error: Color,
)

fun palette(h: Float, dark: Boolean, amoled: Boolean): Pal {
    val p = if (dark) Pal(
        dark = true, amoled = amoled,
        primary = hsl(h, 55f, 72f), onPrimary = hsl(h, 70f, 14f),
        primaryContainer = hsl(h, 42f, 25f), onPrimaryContainer = hsl(h, 70f, 88f),
        secondaryContainer = hsl(h + 10, 16f, 27f), onSecondaryContainer = hsl(h, 30f, 88f),
        tertiaryContainer = hsl(h + 60, 32f, 25f), onTertiaryContainer = hsl(h + 60, 60f, 88f),
        surface = hsl(h, 10f, 7f), surfaceLow = hsl(h, 9f, 10f), surfaceC = hsl(h, 9f, 12f),
        surfaceHigh = hsl(h, 8f, 16f), surfaceHighest = hsl(h, 7f, 20f),
        onSurface = hsl(h, 10f, 90f), onSurfaceVariant = hsl(h, 8f, 76f),
        outline = hsl(h, 6f, 57f), outlineVariant = hsl(h, 7f, 30f), error = Color(0xFFFFB4AB),
    ) else Pal(
        dark = false, amoled = false,
        primary = hsl(h, 48f, 31f), onPrimary = Color.White,
        primaryContainer = hsl(h, 62f, 84f), onPrimaryContainer = hsl(h, 80f, 10f),
        secondaryContainer = hsl(h + 10, 28f, 87f), onSecondaryContainer = hsl(h, 40f, 12f),
        tertiaryContainer = hsl(h + 60, 52f, 86f), onTertiaryContainer = hsl(h + 60, 80f, 10f),
        surface = hsl(h, 30f, 97f), surfaceLow = hsl(h, 22f, 95f), surfaceC = hsl(h, 18f, 92f),
        surfaceHigh = hsl(h, 14f, 89f), surfaceHighest = hsl(h, 12f, 87f),
        onSurface = hsl(h, 10f, 10f), onSurfaceVariant = hsl(h, 8f, 27f),
        outline = hsl(h, 5f, 45f), outlineVariant = hsl(h, 10f, 78f), error = Color(0xFFBA1A1A),
    )
    return if (amoled) p.copy(
        surface = Color.Black, surfaceLow = Color.Black, surfaceC = Color(0xFF0E0F0E),
        surfaceHigh = Color(0xFF191A19), surfaceHighest = Color(0xFF232423),
    ) else p
}

/** A timer's own colors, from its hue (tonal container, like Topé's tiles). */
@Immutable
data class TimerColors(val container: Color, val onContainer: Color, val accent: Color, val onAccent: Color, val track: Color)

fun timerColors(hue: Float, pal: Pal): TimerColors = when {
    pal.amoled -> TimerColors(hsl(hue, 40f, 15f), hsl(hue, 70f, 88f), hsl(hue, 60f, 70f), hsl(hue, 70f, 12f), hsl(hue, 30f, 28f))
    pal.dark -> TimerColors(hsl(hue, 38f, 22f), hsl(hue, 70f, 88f), hsl(hue, 60f, 72f), hsl(hue, 70f, 14f), hsl(hue, 30f, 34f))
    else -> TimerColors(hsl(hue, 62f, 87f), hsl(hue, 80f, 11f), hsl(hue, 52f, 36f), Color.White, hsl(hue, 40f, 76f))
}

val LocalPal = staticCompositionLocalOf { palette(272f, true, false) }

// ---------------------------------------------------------------- Fredoka (variable: weight 300-700, width 75-125)

private val familyCache = HashMap<Pair<Int, Int>, FontFamily>()

/** Fredoka at a given weight and width (%), like Topé's font-stretch. */
fun fredoka(weight: Int, width: Int = 100): FontFamily = familyCache.getOrPut(weight to width) {
    FontFamily(
        Font(
            R.font.fredoka,
            weight = FontWeight(weight),
            variationSettings = FontVariation.Settings(
                FontVariation.weight(weight),
                FontVariation.width(width.toFloat()),
            )
        )
    )
}

/** Text style with Fredoka. */
fun fs(size: TextUnit, weight: Int = 420, width: Int = 100, spacing: TextUnit = 0.sp, color: Color = Color.Unspecified) =
    TextStyle(
        fontFamily = fredoka(weight, width),
        fontWeight = FontWeight(weight),
        fontSize = size,
        letterSpacing = spacing,
        color = color,
    )

object Type {
    val title = fs(34.sp, 700, 125, (-0.6).sp)
    val quip = fs(14.sp, 400, 94, 0.1.sp)
    val section = fs(13.sp, 600, 86, 1.1.sp)
    val label = fs(14.sp, 600, 86, 0.35.sp)
    val body = fs(15.sp, 420, 100)
    val caption = fs(13.sp, 420, 96)
    val rowTitle = fs(16.sp, 620, 104)
    val rowSub = fs(13.sp, 420, 96)
    val button = fs(17.sp, 650, 110)
    val buttonBig = fs(20.sp, 700, 116, (-0.2).sp)
    val sheetTitle = fs(24.sp, 700, 118, (-0.3).sp)
    val nav = fs(12.sp, 500, 95)
    val navOn = fs(12.sp, 680, 115)
}

// ---------------------------------------------------------------- theme

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val settings by SettingsStore.settings.collectAsState()
    val sysDark = isSystemInDarkTheme()
    val dark = when (settings.theme) {
        "dark", "amoled" -> true
        "light" -> false
        else -> sysDark
    }
    val amoled = settings.theme == "amoled"
    val hue = APP_HUES[settings.color] ?: 272f
    val pal = remember(hue, dark, amoled) { palette(hue, dark, amoled) }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }

    val scheme = if (dark) darkColorScheme(
        primary = pal.primary, onPrimary = pal.onPrimary,
        primaryContainer = pal.primaryContainer, onPrimaryContainer = pal.onPrimaryContainer,
        secondaryContainer = pal.secondaryContainer, onSecondaryContainer = pal.onSecondaryContainer,
        tertiaryContainer = pal.tertiaryContainer, onTertiaryContainer = pal.onTertiaryContainer,
        background = pal.surface, onBackground = pal.onSurface,
        surface = pal.surface, onSurface = pal.onSurface, onSurfaceVariant = pal.onSurfaceVariant,
        surfaceContainerLow = pal.surfaceLow, surfaceContainer = pal.surfaceC,
        surfaceContainerHigh = pal.surfaceHigh, surfaceContainerHighest = pal.surfaceHighest,
        outline = pal.outline, outlineVariant = pal.outlineVariant, error = pal.error,
    ) else lightColorScheme(
        primary = pal.primary, onPrimary = pal.onPrimary,
        primaryContainer = pal.primaryContainer, onPrimaryContainer = pal.onPrimaryContainer,
        secondaryContainer = pal.secondaryContainer, onSecondaryContainer = pal.onSecondaryContainer,
        tertiaryContainer = pal.tertiaryContainer, onTertiaryContainer = pal.onTertiaryContainer,
        background = pal.surface, onBackground = pal.onSurface,
        surface = pal.surface, onSurface = pal.onSurface, onSurfaceVariant = pal.onSurfaceVariant,
        surfaceContainerLow = pal.surfaceLow, surfaceContainer = pal.surfaceC,
        surfaceContainerHigh = pal.surfaceHigh, surfaceContainerHighest = pal.surfaceHighest,
        outline = pal.outline, outlineVariant = pal.outlineVariant, error = pal.error,
    )

    CompositionLocalProvider(LocalPal provides pal) {
        MaterialTheme(
            colorScheme = scheme,
            typography = MaterialTheme.typography.let { t ->
                val f = fredoka(420)
                t.copy(
                    bodyLarge = t.bodyLarge.copy(fontFamily = f),
                    bodyMedium = t.bodyMedium.copy(fontFamily = f),
                    bodySmall = t.bodySmall.copy(fontFamily = f),
                    labelLarge = t.labelLarge.copy(fontFamily = fredoka(600)),
                )
            },
            content = content
        )
    }
}
