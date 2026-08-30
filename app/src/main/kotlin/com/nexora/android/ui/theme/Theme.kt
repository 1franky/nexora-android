package com.nexora.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Colores propios del dominio financiero (ingreso/gasto, tarjeta destacada)
 * que no tienen un rol equivalente en el esquema estándar de Material 3 —
 * MaterialTheme.colorScheme.error se reserva para errores de verdad (ej. un
 * campo de formulario inválido), no para "es un gasto".
 */
data class NexoraExtendedColors(
    val income: Color,
    val expense: Color,
    val accentContainer: Color,
)

private val LocalNexoraExtendedColors = staticCompositionLocalOf {
    NexoraExtendedColors(income = Color.Unspecified, expense = Color.Unspecified, accentContainer = Color.Unspecified)
}

private val DarkScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkAccentContainer,
    onPrimaryContainer = DarkTextPrimary,
    secondary = DarkPrimaryLight,
    onSecondary = DarkOnPrimary,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline,
    outlineVariant = DarkOutline,
    error = DarkExpense,
    onError = DarkOnPrimary,
)

private val LightScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightAccentContainer,
    onPrimaryContainer = LightTextPrimary,
    secondary = LightPrimaryLight,
    onSecondary = LightOnPrimary,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurface,
    onSurfaceVariant = LightTextSecondary,
    outline = LightOutline,
    outlineVariant = LightOutline,
    error = LightExpense,
    onError = LightOnPrimary,
)

private val DarkExtended = NexoraExtendedColors(income = DarkIncome, expense = DarkExpense, accentContainer = DarkAccentContainer)
private val LightExtended = NexoraExtendedColors(income = LightIncome, expense = LightExpense, accentContainer = LightAccentContainer)

/** Preferencia de tema del usuario (plan.md: claro/oscuro/sistema, persistida — ver ThemePreference.kt). */
enum class ThemeMode { LIGHT, DARK, SYSTEM }

@Composable
fun NexoraTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val useDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (useDark) DarkScheme else LightScheme
    val extendedColors = if (useDark) DarkExtended else LightExtended

    CompositionLocalProvider(LocalNexoraExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NexoraTypography,
            content = content,
        )
    }
}

/** Acceso a los colores extendidos, al estilo de MaterialTheme.colorScheme: `NexoraTheme.extendedColors.income`. */
object NexoraExtendedTheme {
    val colors: NexoraExtendedColors
        @Composable
        get() = LocalNexoraExtendedColors.current
}
