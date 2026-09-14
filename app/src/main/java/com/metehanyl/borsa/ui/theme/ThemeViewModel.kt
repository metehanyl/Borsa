package com.metehanyl.borsa.ui.theme

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val PREFS_NAME = "kuresel_borsa_prefs"
private const val KEY_THEME = "theme_preference"

/**
 * Kullanıcının açık/koyu (gece modu) tema tercihini tutar ve cihaz
 * kapatılıp açılsa da hatırlanması için basit bir SharedPreferences'e kaydeder.
 * Varsayılan SYSTEM'dir (cihazın kendi gece modu ayarını takip eder).
 */
class ThemeViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _themePreference = MutableStateFlow(loadSaved())
    val themePreference: StateFlow<ThemePreference> = _themePreference.asStateFlow()

    private fun loadSaved(): ThemePreference {
        val name = prefs.getString(KEY_THEME, null) ?: return ThemePreference.SYSTEM
        return runCatching { ThemePreference.valueOf(name) }.getOrDefault(ThemePreference.SYSTEM)
    }

    /** Sistem → Açık → Koyu → Sistem şeklinde döngüsel olarak temayı değiştirir. */
    fun cycleTheme() {
        val next = _themePreference.value.next()
        _themePreference.value = next
        prefs.edit().putString(KEY_THEME, next.name).apply()
    }
}
