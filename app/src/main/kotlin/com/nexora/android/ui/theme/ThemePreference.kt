package com.nexora.android.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore(name = "nexora_theme")
private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")

/**
 * Persiste la preferencia de tema del usuario (plan.md, sección 5:
 * "la selección del usuario debe persistirse"). Por defecto SYSTEM — a
 * diferencia de nexora-web (donde se quitó la opción "Sistema" a pedido
 * explícito), aquí sigue siendo un requisito vigente del plan de Android.
 */
class ThemePreference(private val context: Context) {

    val themeMode = context.themeDataStore.data.map { prefs ->
        when (prefs[THEME_MODE_KEY]) {
            ThemeMode.LIGHT.name -> ThemeMode.LIGHT
            ThemeMode.DARK.name -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.themeDataStore.edit { prefs -> prefs[THEME_MODE_KEY] = mode.name }
    }
}
