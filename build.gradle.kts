// Bloque raíz: declara los plugins usados por los subproyectos sin aplicarlos aquí
// (cada plugin se aplica en app/build.gradle.kts). Mantiene las versiones en un solo lugar.
plugins {
    // Desde AGP 9.0, el soporte de Kotlin va incluido en com.android.application
    // (ya no hace falta aplicar org.jetbrains.kotlin.android aparte).
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
