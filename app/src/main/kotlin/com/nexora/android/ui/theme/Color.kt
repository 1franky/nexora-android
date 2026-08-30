package com.nexora.android.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Tokens de marca. El modo oscuro reproduce exactamente la dirección "fintech
 * oscuro" elegida en los mockups (Main.dc.html); el modo claro reutiliza los
 * mismos tokens que ya usa nexora-web en su tema claro (theme.ts: primary
 * #1565c0, secondary/success #2e7d32, error #c62828) — misma marca, mismo
 * significado de color, en ambas plataformas.
 */

// --- Oscuro ---
val DarkBackground = Color(0xFF0B0F1A)
val DarkSurface = Color(0xFF141A2B)
val DarkOutline = Color(0xFF242C40)
val DarkAccentContainer = Color(0xFF16264A)
val DarkPrimary = Color(0xFF3987E5)
val DarkPrimaryLight = Color(0xFF6BA6EE)
val DarkOnPrimary = Color(0xFF0B0F1A)
val DarkTextPrimary = Color(0xFFF4F6FB)
val DarkTextSecondary = Color(0xFF7C879E)
val DarkTextTertiary = Color(0xFFA9B2C3)
val DarkIncome = Color(0xFF34D399)
val DarkExpense = Color(0xFFF87171)

// --- Claro ---
val LightBackground = Color(0xFFF7F9FC)
val LightSurface = Color(0xFFFFFFFF)
val LightOutline = Color(0xFFE4E8F0)
val LightAccentContainer = Color(0xFFDCE8FB)
val LightPrimary = Color(0xFF1565C0)
val LightPrimaryLight = Color(0xFF3987E5)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightTextPrimary = Color(0xFF1B1B1F)
val LightTextSecondary = Color(0xFF5C5E66)
val LightTextTertiary = Color(0xFF74777F)
val LightIncome = Color(0xFF2E7D32)
val LightExpense = Color(0xFFC62828)
