package com.metehanyl.borsa.ui.theme

/** Kullanıcının uygulama teması tercihi. SYSTEM, cihazın gece modu ayarını takip eder. */
enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK;

    /** Sıradaki tercihe geç (döngüsel: Sistem → Açık → Koyu → Sistem...). */
    fun next(): ThemePreference = when (this) {
        SYSTEM -> LIGHT
        LIGHT -> DARK
        DARK -> SYSTEM
    }
}
